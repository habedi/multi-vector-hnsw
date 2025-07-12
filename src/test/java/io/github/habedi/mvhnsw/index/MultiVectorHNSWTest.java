package io.github.habedi.mvhnsw.index;

import static org.junit.jupiter.api.Assertions.*;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Cosine;
import io.github.habedi.mvhnsw.distance.Euclidean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiVectorHNSWTest {

  private final List<FloatVector> vectors1 =
      List.of(FloatVector.of(1.0f, 1.0f), FloatVector.of(1.0f, 0.0f));
  private final List<FloatVector> vectors2 =
      List.of(FloatVector.of(10.0f, 10.0f), FloatVector.of(0.0f, 1.0f));
  private Index index;

  @BeforeEach
  void setUp() {
    index =
        MultiVectorHNSW.builder()
            .withM(10)
            .withEfConstruction(100)
            .withWeightedAverageDistance()
            .addDistance(new Euclidean(), 0.5f)
            .addDistance(new Cosine(), 0.5f)
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
  }

  @Test
  void testAddWithExistingIdThrowsException() {
    index.add(1L, vectors1);
    assertThrows(IllegalArgumentException.class, () -> index.add(1L, vectors2));
  }

  @Test
  void testRemove() {
    index.add(1L, vectors1);
    assertEquals(1, index.size());

    boolean removed = index.remove(1L);
    assertTrue(removed);
    assertEquals(0, index.size());
    assertTrue(index.get(1L).isEmpty());

    boolean removedAgain = index.remove(1L);
    assertFalse(removedAgain);
  }

  @Test
  void testReAddAfterRemove() {
    index.add(1L, vectors1);
    index.remove(1L);
    assertEquals(0, index.size());

    assertDoesNotThrow(() -> index.add(1L, vectors2));
    assertEquals(1, index.size());
    Optional<List<FloatVector>> retrieved = index.get(1L);
    assertTrue(retrieved.isPresent());
    assertEquals(vectors2, retrieved.get());
  }

  @Test
  void testSearch() {
    index.add(1L, vectors1);
    index.add(2L, vectors2);

    List<FloatVector> queryVectors =
        List.of(FloatVector.of(1.1f, 1.1f), FloatVector.of(0.9f, 0.1f));
    List<SearchResult> results = index.search(queryVectors, 1);

    assertEquals(1, results.size());
    assertEquals(1L, results.get(0).id());
  }

  @Test
  void testSearchEmptyIndex() {
    List<FloatVector> queryVectors =
        List.of(FloatVector.of(1.1f, 1.1f), FloatVector.of(0.9f, 0.1f));
    List<SearchResult> results = index.search(queryVectors, 5);
    assertTrue(results.isEmpty());
  }

  @Test
  void testAddAll() {
    Map<Long, List<FloatVector>> items = Map.of(1L, vectors1, 2L, vectors2);
    index.addAll(items);
    assertEquals(2, index.size());
    assertTrue(index.get(2L).isPresent());
  }

  @Test
  void testClear() {
    index.add(1L, vectors1);
    index.add(2L, vectors2);
    assertEquals(2, index.size());

    index.clear();
    assertEquals(0, index.size());
    assertTrue(index.get(1L).isEmpty());
  }

  @Test
  void testVacuum() {
    index.add(1L, vectors1);
    index.add(2L, vectors2);
    index.remove(1L); // Soft delete
    assertEquals(1, index.size());

    // At this point, the internal maps might still contain remnants of item 1
    index.vacuum(); // Rebuilds the index with only live items

    assertEquals(1, index.size());
    assertTrue(index.get(2L).isPresent());
    assertFalse(index.get(1L).isPresent());

    // Verify we can still search correctly
    List<SearchResult> results = index.search(vectors2, 1);
    assertEquals(1, results.size());
    assertEquals(2L, results.get(0).id());
  }

  @Test
  void testSaveAndLoad(@TempDir File tempDir) throws IOException, ClassNotFoundException {
    index.add(1L, vectors1);
    index.add(2L, vectors2);
    assertEquals(2, index.size());

    File indexPath = new File(tempDir, "my.index");
    index.save(indexPath.toPath());
    assertTrue(Files.exists(indexPath.toPath()));

    Index loadedIndex = MultiVectorHNSW.load(indexPath.toPath());
    assertEquals(2, loadedIndex.size());

    Optional<List<FloatVector>> retrieved = loadedIndex.get(1L);
    assertTrue(retrieved.isPresent());
    assertEquals(vectors1, retrieved.get());
  }

  @Test
  void testBuilderValidation() {
    assertThrows(IllegalArgumentException.class, () -> MultiVectorHNSW.builder().withM(0));
    assertThrows(
        IllegalArgumentException.class, () -> MultiVectorHNSW.builder().withEfConstruction(-10));
    assertThrows(
        NullPointerException.class,
        () -> MultiVectorHNSW.builder().withM(16).withEfConstruction(100).build());
  }
}
