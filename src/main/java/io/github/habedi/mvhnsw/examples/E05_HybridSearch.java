package io.github.habedi.mvhnsw.examples;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Cosine;
import io.github.habedi.mvhnsw.distance.DotProduct;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;
import java.util.List;

/**
 * Example 5: Simulates a "hybrid" search scenario.
 *
 * <p>This is useful when you want to combine different types of vector representations. Here, we
 * combine a dense embedding (like one from a deep model) with a sparse, keyword-based vector (like
 * TF-IDF), weighting each one differently.
 */
public class E05_HybridSearch {
  public static void main(String[] args) {
    // 1. Configure a hybrid distance strategy
    Index index =
        MultiVectorHNSW.builder()
            .withM(16)
            .withEfConstruction(200)
            .withWeightedAverageDistance()
            // Dense vector (e.g., from a sentence transformer) gets 80% of the weight
            .addDistance(new Cosine(), 0.8f)
            // Sparse vector (e.g., TF-IDF keywords) gets 20% of the weight
            .addDistance(new DotProduct(), 0.2f)
            .and()
            .build();

    // 2. Add items with two distinct vector types
    // Item 1: Semantically about "dogs", keyword is "animal"
    index.add(
        1L,
        List.of(
            FloatVector.of(0.9f, 0.1f), // Dense: "dog"
            FloatVector.of(1.0f, 0.0f, 0.0f) // Sparse: "animal"
            ));
    // Item 2: Semantically about "cats", keyword is "animal"
    index.add(
        2L,
        List.of(
            FloatVector.of(0.1f, 0.9f), // Dense: "cat"
            FloatVector.of(1.0f, 0.0f, 0.0f) // Sparse: "animal"
            ));
    // Item 3: Semantically about "dogs", keyword is "pet"
    index.add(
        3L,
        List.of(
            FloatVector.of(0.85f, 0.15f), // Dense: "dog"
            FloatVector.of(0.0f, 1.0f, 0.0f) // Sparse: "pet"
            ));

    // 3. Create a hybrid query: semantically about "canine" (close to "dog"),
    //    but with a strong preference for the keyword "animal".
    System.out.println("Searching for a 'canine' that is also an 'animal'...");
    List<FloatVector> query =
        List.of(
            FloatVector.of(0.95f, 0.05f), // Dense: "canine"
            FloatVector.of(1.0f, 0.0f, 0.0f) // Sparse: "animal"
            );
    List<SearchResult> results = index.search(query, 3);

    System.out.println("Hybrid Search Results:");
    results.forEach(System.out::println);
  }
}
