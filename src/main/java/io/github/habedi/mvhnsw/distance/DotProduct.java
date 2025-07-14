package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.io.Serial;
import java.io.Serializable;

/**
 * Calculates distance based on the dot product of two vectors.
 *
 * <p>This distance is defined as the negative dot product ({@code -a.dot(b)}). It is not a true
 * metric, but it is commonly used in vector search for maximum inner product search (MIPS). When
 * using this distance, vectors with a larger (more positive) dot product are considered "closer" to
 * each other.
 */
public class DotProduct implements Distance<FloatVector>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Computes the negative dot product between two FloatVectors.
   *
   * @param a The first vector.
   * @param b The second vector.
   * @return The negative dot product.
   * @throws IllegalArgumentException if the vector lengths are not equal.
   */
  @Override
  public double compute(FloatVector a, FloatVector b) {
    if (a.length() != b.length()) {
      throw new IllegalArgumentException("Vector lengths must be equal.");
    }
    return -a.dot(b);
  }

  /**
   * Gets the name of the distance metric.
   *
   * @return The string "DotProduct".
   */
  @Override
  public String getName() {
    return "DotProduct";
  }
}
