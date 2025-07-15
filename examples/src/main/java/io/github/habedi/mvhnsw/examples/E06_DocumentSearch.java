package io.github.habedi.mvhnsw.examples;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Cosine;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;

import java.util.List;

/**
 * Example 6: Simulates a document search scenario.
 *
 * <p>Documents are indexed with two vectors: one for the title and one for the body. We can weight
 * the title match higher than the body match to improve search relevance.
 */
public class E06_DocumentSearch {

  public static void main(String[] args) {
    // 1. Configure the index to prioritize title matches (70%) over body matches (30%)
    System.out.println("Configuring index for document search (70% title, 30% body)...");
    Index index =
      MultiVectorHNSW.builder()
        .withM(32)
        .withEfConstruction(250)
        .withWeightedAverageDistance()
        .addDistance(new Cosine(), 0.7f) // Title Vector
        .addDistance(new Cosine(), 0.3f) // Body Vector
        .and()
        .build();

    // 2. Simulate fetching embeddings and add documents
    Document doc1 =
      new Document(
        1L,
        "Java Performance",
        "Tips for...",
        List.of(titleEmbedding("java performance"), bodyEmbedding("optimizing jvm")));
    Document doc2 =
      new Document(
        2L,
        "Python for Beginners",
        "An intro to...",
        List.of(titleEmbedding("python intro"), bodyEmbedding("data science basics")));
    Document doc3 =
      new Document(
        3L,
        "Advanced Java",
        "Concurrency...",
        List.of(titleEmbedding("advanced java"), bodyEmbedding("multithreading patterns")));

    index.add(doc1.id(), doc1.vectors());
    index.add(doc2.id(), doc2.vectors());
    index.add(doc3.id(), doc3.vectors());

    // 3. A user searches for "java threading"
    System.out.println("\nUser is searching for 'java threading'...");
    List<FloatVector> queryVectors =
      List.of(titleEmbedding("java threading"), bodyEmbedding("java threading"));
    List<SearchResult> results = index.search(queryVectors, 3, 20);

    System.out.println("Search Results:");
    results.forEach(
      result -> {
        // In a real application, you would use the ID to look up the full document
        System.out.printf(
          "  - Found Document ID: %d with score: %.4f%n", result.id(), result.score());
      });
  }

  // --- Mock Embedding Functions ---
  // In a real application, these would call an actual text embedding model.
  private static FloatVector titleEmbedding(String text) {
    if (text.contains("java performance")) return FloatVector.of(0.9f, 0.1f, 0.1f);
    if (text.contains("python intro")) return FloatVector.of(0.1f, 0.9f, 0.1f);
    if (text.contains("advanced java")) return FloatVector.of(0.8f, 0.2f, 0.1f);
    if (text.contains("java threading")) return FloatVector.of(0.85f, 0.15f, 0.1f);
    return FloatVector.of(0.3f, 0.3f, 0.3f);
  }

  private static FloatVector bodyEmbedding(String text) {
    if (text.contains("optimizing jvm")) return FloatVector.of(0.7f, 0.3f, 0.1f);
    if (text.contains("data science basics")) return FloatVector.of(0.1f, 0.8f, 0.2f);
    if (text.contains("multithreading patterns")) return FloatVector.of(0.2f, 0.1f, 0.9f);
    if (text.contains("java threading")) return FloatVector.of(0.2f, 0.2f, 0.8f);
    return FloatVector.of(0.3f, 0.3f, 0.3f);
  }

  // A simple record to represent our documents
  public record Document(long id, String title, String body, List<FloatVector> vectors) {
  }
}
