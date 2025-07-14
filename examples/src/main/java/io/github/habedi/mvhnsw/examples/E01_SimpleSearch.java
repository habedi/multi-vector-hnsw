package io.github.habedi.mvhnsw.examples;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Cosine;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;
import java.util.List;

/**
 * Example 1: A simple example that shows the basic workflow of how things work.
 *
 * <p>1. Build an index with a single distance metric. 2. Add a few items. 3. Perform a search and
 * print the results.
 */
public class E01_SimpleSearch {
  public static void main(String[] args) {
    // 1. Configure the index
    Index index =
        MultiVectorHNSW.builder()
            .withM(16)
            .withEfConstruction(200)
            .withWeightedAverageDistance()
            .addDistance(new Cosine(), 1.0f) // Using a single distance with a weight of 1.0
            .and()
            .build();

    // 2. Add items to the index
    System.out.println("Adding items representing simple 2D points...");
    index.add(1L, List.of(FloatVector.of(0.1f, 0.9f))); // Similar to query
    index.add(2L, List.of(FloatVector.of(0.8f, 0.2f))); // Dissimilar
    index.add(3L, List.of(FloatVector.of(0.2f, 0.8f))); // Very similar to query

    // 3. Create a query and search for the top 2 nearest neighbors
    System.out.println("\nSearching for the 2 nearest neighbors to (0.1, 0.8)...");
    List<FloatVector> query = List.of(FloatVector.of(0.1f, 0.8f));
    List<SearchResult> results = index.search(query, 2);

    // 4. Print the results
    System.out.println("Search results (SearchResult[id, score]):");
    results.forEach(System.out::println);
  }
}
