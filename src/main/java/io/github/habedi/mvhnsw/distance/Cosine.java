package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.io.Serial;
import java.io.Serializable;

/**
 * Calculates the cosine distance between two vectors.
 *
 * <p>Cosine distance is a measure of dissimilarity derived from the angle between two vectors. It
 * is calculated as {@code 1 - cosine_similarity}. A value of 0 indicates the vectors are identical
 * in orientation, 1 indicates they are orthogonal, and 2 indicates they are diametrically opposed.
 */
public class Cosine implements Distance<FloatVector>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Computes the cosine distance between two FloatVectors.
   *
   * @param a The first vector.
   * @param b The second vector.
   * @return The cosine distance, a value between 0.0 and 2.0.
   * @throws IllegalArgumentException if the vector lengths are not equal.
   */
  @Override
  public double compute(FloatVector a, FloatVector b) {
    if (a.length() != b.length()) {
      throw new IllegalArgumentException("Vector lengths must be equal.");
    }
    double similarity = a.cosine(b);
    return 1.0 - similarity;
  }

  /**
   * Gets the name of the distance metric.
   *
   * @return The string "Cosine".
   */
  @Override
  public String getName() {
    return "Cosine";
  }
}
