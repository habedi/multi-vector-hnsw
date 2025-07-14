package io.github.habedi.mvhnsw.examples;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Cosine;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;
import java.util.List;
import java.util.Map;

/**
 * Example 3: Demonstrates bulk operations for efficiency and memory management.
 *
 * <p>1. Use `addAll()` to efficiently add multiple items. 2. Use `remove()` to perform soft
 * deletes. 3. Use `vacuum()` to permanently remove deleted items and reclaim space.
 */
public class E03_BulkAndVacuum {
  public static void main(String[] args) {
    Index index =
        MultiVectorHNSW.builder()
            .withM(16)
            .withEfConstruction(200)
            .withWeightedAverageDistance()
            .addDistance(new Cosine(), 1.0f)
            .and()
            .build();

    // 1. Add items in bulk
    System.out.println("Adding items in bulk...");
    Map<Long, List<FloatVector>> items =
        Map.of(
            1L, List.of(FloatVector.of(0.1f, 0.9f)),
            2L, List.of(FloatVector.of(0.8f, 0.2f)),
            3L, List.of(FloatVector.of(0.2f, 0.7f)),
            4L, List.of(FloatVector.of(0.9f, 0.1f)));
    index.addAll(items);
    System.out.println("Index size after bulk add: " + index.size());

    // 2. Perform soft deletes
    System.out.println("\nPerforming soft deletes on items 2 and 4...");
    index.remove(2L);
    index.remove(4L);
    System.out.println("Index size after soft deletes: " + index.size());

    // 3. Verify search results (deleted items are ignored)
    List<SearchResult> results = index.search(List.of(FloatVector.of(0.8f, 0.2f)), 1);
    System.out.println("Closest item to deleted item 2 is now: " + results.get(0));

    // 4. Vacuum the index
    System.out.println("\nRunning vacuum to permanently remove deleted items...");
    index.vacuum();
    System.out.println("Index size after vacuum: " + index.size());
    System.out.println("Final item keys in index: " + index.keySet());
  }
}
