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
  void testAddValidatesVectors() {
    assertThrows(NullPointerException.class, () -> index.add(1L, null));
    assertThrows(IllegalArgumentException.class, () -> index.add(1L, List.of()));
    assertThrows(
        NullPointerException.class,
        () -> index.add(1L, java.util.Arrays.asList((FloatVector) null)));
    assertEquals(0, index.size());
  }

  @Test
  void testAddAllValidatesItemsBeforeInserting() {
    assertThrows(NullPointerException.class, () -> index.addAll(null));

    Map<Long, List<FloatVector>> items = Map.of(1L, vectors1, 2L, List.of());
    assertThrows(IllegalArgumentException.class, () -> index.addAll(items));
    assertEquals(0, index.size()); // No partial insertion.
  }

  @Test
  void testAddAllRejectsDuplicateIdWithoutPartialInsert() {
    index.add(1L, vectors1);
    Map<Long, List<FloatVector>> items = Map.of(1L, vectors2, 2L, vectors2);
    assertThrows(IllegalArgumentException.class, () -> index.addAll(items));
    assertEquals(1, index.size()); // No partial insertion.
    assertTrue(index.get(2L).isEmpty());
    assertEquals(vectors1, index.get(1L).get()); // Existing item is unchanged.
  }

  @Test
  void testGetDistanceReturnsConfiguredDistance() {
    io.github.habedi.mvhnsw.distance.MultiVectorDistance distance =
        new io.github.habedi.mvhnsw.distance.WeightedAverageDistance(
            List.of(new SquaredEuclidean()), new float[] {1.0f});
    Index customIndex = MultiVectorHNSW.builder().withDistance(distance).build();
    assertSame(distance, customIndex.getDistance());
    assertNotNull(index.getDistance());
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
  void testEntryPointUpdateAfterDeletion() throws Exception {
    index.add(1L, vectors1);
    index.add(2L, vectors2);

    var entryPointField = MultiVectorHNSW.class.getDeclaredField("entryPoint");
    entryPointField.setAccessible(true);

    var initialEntryPoint = entryPointField.get(index);
    var nodeClass = initialEntryPoint.getClass();
    var idField = nodeClass.getDeclaredField("id");
    idField.setAccessible(true);
    long initialEntryPointId = (long) idField.get(initialEntryPoint);

    index.remove(initialEntryPointId);

    index.search(vectors2, 1, 10);

    var updatedEntryPoint = entryPointField.get(index);

    var deletedField = nodeClass.getDeclaredField("deleted");
    deletedField.setAccessible(true);
    assertFalse((boolean) deletedField.get(updatedEntryPoint));
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
  void testEntryPointUpdateAfterDeletionToHighestLevelNode() throws Exception {
    index.add(1L, vectors1);
    index.add(2L, vectors2);
    index.add(3L, List.of(FloatVector.of(100.0f, 100.0f)));

    var entryPointField = MultiVectorHNSW.class.getDeclaredField("entryPoint");
    entryPointField.setAccessible(true);

    var initialEntryPoint = entryPointField.get(index);
    var nodeClass = initialEntryPoint.getClass();
    var idField = nodeClass.getDeclaredField("id");
    idField.setAccessible(true);
    long initialEntryPointId = (long) idField.get(initialEntryPoint);

    index.remove(initialEntryPointId);

    index.search(vectors2, 1, 10);

    var updatedEntryPoint = entryPointField.get(index);
    var levelField = nodeClass.getDeclaredField("level");
    levelField.setAccessible(true);
    int updatedEntryPointLevel = (int) levelField.get(updatedEntryPoint);

    var nodesField = MultiVectorHNSW.class.getDeclaredField("nodes");
    nodesField.setAccessible(true);
    @SuppressWarnings("unchecked")
    var nodes = (Map<Long, Object>) nodesField.get(index);

    int maxLevel = 0;
    for (Object node : nodes.values()) {
      var deletedField = node.getClass().getDeclaredField("deleted");
      deletedField.setAccessible(true);
      if (!(boolean) deletedField.get(node)) {
        int level = (int) levelField.get(node);
        if (level > maxLevel) {
          maxLevel = level;
        }
      }
    }

    assertEquals(maxLevel, updatedEntryPointLevel);
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

  @Test
  void testVacuumIsDeterministic() throws Exception {
    // Use TreeMap for deterministic iteration order
    Map<Long, List<FloatVector>> items = new java.util.TreeMap<>();
    for (long i = 0; i < 100; i++) {
      items.put(i, List.of(FloatVector.of((float) i, (float) i)));
    }

    // 1. Build index1, which will have random levels
    Index index1 =
        MultiVectorHNSW.builder()
            .withM(10)
            .withEfConstruction(100)
            .withWeightedAverageDistance()
            .addDistance(new SquaredEuclidean(), 1.0f)
            .and()
            .build();
    index1.addAll(items);

    // 2. Extract levels from index1 to build an identical index2
    var nodesField = MultiVectorHNSW.class.getDeclaredField("nodes");
    nodesField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Long, Object> nodes1Map = (Map<Long, Object>) nodesField.get(index1);

    Map<Long, Integer> levels = new java.util.HashMap<>();
    for (Object node : nodes1Map.values()) {
      var idField = node.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      long id = (long) idField.get(node);

      var levelField = node.getClass().getDeclaredField("level");
      levelField.setAccessible(true);
      int level = (int) levelField.get(node);
      levels.put(id, level);
    }

    // 3. Build index2 deterministically with the same levels and insertion order
    Index index2 =
        MultiVectorHNSW.builder()
            .withM(10)
            .withEfConstruction(100)
            .withWeightedAverageDistance()
            .addDistance(new SquaredEuclidean(), 1.0f)
            .and()
            .build();

    var privateAddMethod =
        MultiVectorHNSW.class.getDeclaredMethod("add", long.class, List.class, int.class);
    privateAddMethod.setAccessible(true);

    for (Map.Entry<Long, List<FloatVector>> entry : items.entrySet()) {
      privateAddMethod.invoke(index2, entry.getKey(), entry.getValue(), levels.get(entry.getKey()));
    }

    // 4. Now that we have two identical indexes, test that vacuum behaves identically
    index1.remove(50L);
    index2.remove(50L);
    index1.vacuum();
    index2.vacuum();

    // 5. Compare the internal graph structures
    @SuppressWarnings("unchecked")
    Map<Long, Object> nodes1AfterVacuum = (Map<Long, Object>) nodesField.get(index1);
    @SuppressWarnings("unchecked")
    Map<Long, Object> nodes2AfterVacuum = (Map<Long, Object>) nodesField.get(index2);

    assertEquals(nodes1AfterVacuum.size(), nodes2AfterVacuum.size());

    for (long id : nodes1AfterVacuum.keySet()) {
      var node1 = nodes1AfterVacuum.get(id);
      var node2 = nodes2AfterVacuum.get(id);
      assertNotNull(node2, "Node " + id + " missing in second index");

      var connectionsField = node1.getClass().getDeclaredField("connections");
      connectionsField.setAccessible(true);

      @SuppressWarnings("unchecked")
      List<Object>[] connections1 = (List<Object>[]) connectionsField.get(node1);
      @SuppressWarnings("unchecked")
      List<Object>[] connections2 = (List<Object>[]) connectionsField.get(node2);

      assertEquals(connections1.length, connections2.length, "Node " + id + " has different level");
      for (int i = 0; i < connections1.length; i++) {
        var c1 =
            connections1[i].stream()
                .map(
                    n -> {
                      try {
                        var idField = n.getClass().getDeclaredField("id");
                        idField.setAccessible(true);
                        return (long) idField.get(n);
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                    })
                .collect(java.util.stream.Collectors.toSet());

        var c2 =
            connections2[i].stream()
                .map(
                    n -> {
                      try {
                        var idField = n.getClass().getDeclaredField("id");
                        idField.setAccessible(true);
                        return (long) idField.get(n);
                      } catch (Exception e) {
                        throw new RuntimeException(e);
                      }
                    })
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(c1, c2, "Connections at level " + i + " for node " + id + " do not match");
      }
    }
  }
}
