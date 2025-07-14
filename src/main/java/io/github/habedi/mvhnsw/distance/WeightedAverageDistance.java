package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * A {@link MultiVectorDistance} that computes a weighted average of multiple, distinct distance
 * functions.
 *
 * <p>This allows for combining different distance metrics (e.g., 70% Cosine and 30% Euclidean) into
 * a single score, although it is not necessary. The weights are automatically normalized to sum to
 * 1.0.
 */
public class WeightedAverageDistance implements MultiVectorDistance, Serializable {

  @Serial private static final long serialVersionUID = 1L;
  private final List<Distance<FloatVector>> distances;
  private final float[] weights;

  /**
   * Constructs a new WeightedAverageDistance.
   *
   * @param distances A list of {@link Distance} functions to apply to the corresponding vectors.
   * @param weights A float array of weights to apply to each distance calculation. The weights will
   *     be normalized.
   * @throws NullPointerException if the distances or weights are null.
   * @throws IllegalArgumentException if the lists are empty, if their sizes do not match, or if any
   *     weight is negative or the sum of weights is zero.
   */
  public WeightedAverageDistance(List<Distance<FloatVector>> distances, float[] weights) {
    Objects.requireNonNull(distances, "Distances list cannot be null.");
    Objects.requireNonNull(weights, "Weights array cannot be null.");
    if (distances.isEmpty()) {
      throw new IllegalArgumentException("Distances list cannot be empty.");
    }
    if (distances.size() != weights.length) {
      throw new IllegalArgumentException(
          "The number of distances must match the number of weights.");
    }
    this.distances = distances;
    this.weights = normalize(weights);
  }

  /** Normalizes the given weights to sum to 1.0. */
  private float[] normalize(float[] w) {
    double sum = 0.0;
    for (float weight : w) {
      if (weight < 0) {
        throw new IllegalArgumentException("Weights must be non-negative.");
      }
      sum += weight;
    }

    if (sum == 0.0) {
      throw new IllegalArgumentException("Sum of weights cannot be zero.");
    }

    float[] normalized = new float[w.length];
    for (int i = 0; i < w.length; i++) {
      normalized[i] = (float) (w[i] / sum);
    }
    return normalized;
  }

  /**
   * Computes the weighted average distance between two lists of vectors.
   *
   * @param vectors1 The list of vectors for the first item.
   * @param vectors2 The list of vectors for the second item.
   * @return The aggregated weighted distance.
   * @throws IllegalArgumentException if the vector lists' sizes do not match each other or the
   *     configured number of distance functions.
   */
  @Override
  public double compute(List<FloatVector> vectors1, List<FloatVector> vectors2) {
    if (vectors1.size() != vectors2.size()) {
      throw new IllegalArgumentException("Vector list sizes must match.");
    }
    if (vectors1.size() != distances.size()) {
      throw new IllegalArgumentException(
          "Number of vectors must match the number of distance functions.");
    }

    double totalDistance = 0.0;
    for (int i = 0; i < vectors1.size(); i++) {
      totalDistance += weights[i] * distances.get(i).compute(vectors1.get(i), vectors2.get(i));
    }
    return totalDistance;
  }
}
