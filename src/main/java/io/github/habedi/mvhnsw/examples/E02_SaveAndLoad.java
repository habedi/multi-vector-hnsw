package io.github.habedi.mvhnsw.examples;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.SquaredEuclidean;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Example 2: Demonstrates saving an index to a file and loading it back.
 *
 * <p>This is crucial for applications that need to persist their state without rebuilding the index
 * from scratch on every startup.
 */
public class E02_SaveAndLoad {
  public static void main(String[] args) throws IOException, ClassNotFoundException {
    // --- 1. Build and Populate an Index ---
    System.out.println("Building the initial index...");
    Index originalIndex =
        MultiVectorHNSW.builder()
            .withM(16)
            .withEfConstruction(200)
            .withWeightedAverageDistance()
            .addDistance(new SquaredEuclidean(), 1.0f)
            .and()
            .build();

    originalIndex.add(1L, List.of(FloatVector.of(1.0f, 2.0f)));
    originalIndex.add(2L, List.of(FloatVector.of(5.0f, 6.0f)));
    System.out.println("Original index has " + originalIndex.size() + " items.");

    // --- 2. Save the Index to a File ---
    Path indexPath = Files.createTempFile("my-hnsw-index-", ".idx");
    System.out.println("Saving index to: " + indexPath);
    originalIndex.save(indexPath);

    // --- 3. Load the Index from the File ---
    System.out.println("\nLoading index from file...");
    Index loadedIndex = MultiVectorHNSW.load(indexPath);
    System.out.println("Loaded index has " + loadedIndex.size() + " items.");

    // --- 4. Verify the Loaded Index Works ---
    System.out.println("\nSearching in the loaded index to verify it's functional...");
    List<FloatVector> query = List.of(FloatVector.of(1.1f, 2.1f));
    List<SearchResult> results = loadedIndex.search(query, 1);

    System.out.println("Search results:");
    results.forEach(System.out::println);

    // Clean up the temporary file
    Files.delete(indexPath);
    System.out.println("\nCleaned up temporary index file.");
  }
}
