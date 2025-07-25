package io.github.habedi.mvhnsw.index;

import static org.junit.jupiter.api.Assertions.*;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.SquaredEuclidean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiVectorHNSWTest {

  private final List<FloatVector> vectors1 = List.of(FloatVector.of(1.0f, 1.0f));
  private final List<FloatVector> vectors2 = List.of(FloatVector.of(10.0f, 10.0f));
  private Index index;

  @BeforeEach
  void setUp() {
    index =
        MultiVectorHNSW.builder()
            .withM(10)
            .withEfConstruction(100)
            .withWeightedAverageDistance()
            .addDistance(new SquaredEuclidean(), 1.0f)
            .and()
            .build();
  }

  @Test
  void testAddAndGet() {
    index.add(1L, vectors1);
    assertEquals(1, index.size());
    Optional<List<FloatVector>> retrieved = index.get(1L);
    assertTrue(retrieved.isPresent());
    assertEquals(vectors1, retrieved.get());
    assertTrue(index.get(99L).isEmpty()); // Test non-existent item
  }

  @Test
  void testAddAllAndClear() {
    Map<Long, List<FloatVector>> items = Map.of(1L, vectors1, 2L, vectors2);
    index.addAll(items);
    assertEquals(2, index.size());
    assertTrue(index.get(2L).isPresent());
    index.clear();
    assertEquals(0, index.size());
    assertTrue(index.get(1L).isEmpty());
  }

  @Test
  void testRemove() {
    index.add(1L, vectors1);
    assertTrue(index.remove(1L));
    assertEquals(0, index.size());
    assertTrue(index.get(1L).isEmpty());
    assertFalse(index.remove(1L)); // Cannot remove twice
    assertFalse(index.remove(99L)); // Cannot remove non-existent
  }

  @Test
  void testAddThrowsOnDuplicateActiveId() {
    index.add(1L, vectors1);
    assertThrows(IllegalArgumentException.class, () -> index.add(1L, vectors2));
  }

  @Test
  void testReAddAfterRemoveIsAllowed() {
    index.add(1L, vectors1);
    index.remove(1L);
    assertDoesNotThrow(() -> index.add(1L, vectors2));
    assertEquals(1, index.size());
    assertEquals(vectors2, index.get(1L).get());
  }

  @Test
  void testSearchFunctionality() {
    index.add(1L, vectors1);
    index.add(2L, vectors2);
    List<SearchResult> results = index.search(vectors1, 1, 10);
    assertEquals(1, results.size());
    assertEquals(1L, results.get(0).id());
  }

  @Test
  void testSearchThrowsIfEfSearchIsLessThanK() {
    index.add(1L, vectors1);
    assertThrows(IllegalArgumentException.class, () -> index.search(vectors1, 5, 4));
  }

  @Test
  void testSearchEmptyIndex() {
    assertTrue(index.search(vectors1, 5, 10).isEmpty());
  }

  @Test
  void testSearchWithDeletedEntryPoint() {
    index.add(1L, vectors1);
    index.add(2L, vectors2);
    index.remove(1L); // Assume 1L was the entry point

    List<SearchResult> results = index.search(vectors2, 1, 10);
    assertEquals(1, results.size());
    assertEquals(2L, results.get(0).id());
  }

  @Test
  void testSearchDoesNotReturnDeletedItems() {
    index.add(1L, vectors1);
    index.add(2L, vectors2);
    index.remove(2L); // Delete the item we'd normally find

    List<SearchResult> results = index.search(vectors2, 2, 10);
    assertEquals(1, results.size());
    assertEquals(1L, results.get(0).id()); // Should find the other item
  }

  @Test
  void testVacuumRemovesDeletedItems() {
    index.add(1L, vectors1);
    index.add(2L, vectors2);
    index.remove(1L);

    index.vacuum();

    assertEquals(1, index.size());
    assertFalse(index.keySet().contains(1L));
    assertTrue(index.keySet().contains(2L));
  }

  @Test
  void testVacuumOnCleanAndEmptyIndex() {
    index.add(1L, vectors1);
    index.vacuum(); // Should do nothing
    assertEquals(1, index.size());

    index.clear();
    assertDoesNotThrow(() -> index.vacuum()); // Should not fail on empty
    assertEquals(0, index.size());
  }

  @Test
  void testSaveAndLoadIndex(@TempDir File tempDir) throws IOException, ClassNotFoundException {
    index.add(1L, vectors1);
    index.add(2L, vectors2);
    File indexPath = new File(tempDir, "test.index");

    index.save(indexPath.toPath());
    assertTrue(Files.exists(indexPath.toPath()));

    Index loadedIndex = MultiVectorHNSW.load(indexPath.toPath());
    assertEquals(2, loadedIndex.size());
    assertEquals(vectors2, loadedIndex.get(2L).get());
  }

  @Test
  void testBuilderValidation() {
    assertThrows(IllegalArgumentException.class, () -> MultiVectorHNSW.builder().withM(0));
    assertThrows(
        IllegalArgumentException.class, () -> MultiVectorHNSW.builder().withEfConstruction(0));
    assertThrows(NullPointerException.class, () -> MultiVectorHNSW.builder().build());
  }

  @Test
  void testConcurrentReadWrites() throws InterruptedException {
    final int writerThreads = 2;
    final int readerThreads = 4;
    final int itemsPerWriter = 200;
    final int totalItems = writerThreads * itemsPerWriter;
    final var executor = Executors.newFixedThreadPool(writerThreads + readerThreads);
    final var latch = new CountDownLatch(writerThreads + readerThreads);
    final AtomicInteger writeCounter = new AtomicInteger();

    assertDoesNotThrow(
        () -> {
          // Writer tasks
          for (int i = 0; i < writerThreads; i++) {
            executor.submit(
                () -> {
                  latch.countDown();
                  try {
                    latch.await();
                    for (int j = 0; j < itemsPerWriter; j++) {
                      int id = writeCounter.incrementAndGet();
                      index.add((long) id, List.of(FloatVector.of(id, id)));
                    }
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                });
          }

          // Reader tasks
          for (int i = 0; i < readerThreads; i++) {
            executor.submit(
                () -> {
                  latch.countDown();
                  try {
                    latch.await();
                    for (int j = 0; j < 500; j++) {
                      // Search for a random existing item
                      int searchId = (j % writeCounter.get()) + 1;
                      index.search(List.of(FloatVector.of(searchId, searchId)), 5, 10);
                    }
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                });
          }

          executor.shutdown();
          assertTrue(
              executor.awaitTermination(15, TimeUnit.SECONDS),
              "Executor did not terminate in time");

          assertEquals(totalItems, index.size());
          assertTrue(index.get((long) totalItems).isPresent());
        });
  }
}
