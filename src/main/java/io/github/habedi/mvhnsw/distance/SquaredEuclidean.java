package io.github.habedi.mvhnsw.distance;

import static jdk.incubator.vector.FloatVector.SPECIES_PREFERRED;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.io.Serial;
import java.io.Serializable;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Calculates the squared Euclidean (L2) distance between two vectors.
 *
 * <p>Squared Euclidean distance is the sum of the squared differences between corresponding
 * elements of the two vectors. It is often used in place of Euclidean distance to avoid the
 * computationally expensive square root operation when only the relative order of distances is
 * important. This implementation is optimized using the Java Vector API for performance.
 */
public class SquaredEuclidean implements Distance<FloatVector>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private static final VectorSpecies<java.lang.Float> SPECIES = SPECIES_PREFERRED;

  /**
   * Computes the squared Euclidean distance between two FloatVectors.
   *
   * @param a The first vector.
   * @param b The second vector.
   * @return The squared Euclidean distance.
   * @throws IllegalArgumentException if the vector lengths are not equal.
   */
  @Override
  public double compute(FloatVector a, FloatVector b) {
    if (a.length() != b.length()) {
      throw new IllegalArgumentException("Vector lengths must be equal.");
    }

    float[] v1 = a.getUnsafeRawData();
    float[] v2 = b.getUnsafeRawData();
    double sumSq = 0.0;
    int bound = SPECIES.loopBound(v1.length);
    int i = 0;

    // Vectorized loop for the bulk of the array
    for (; i < bound; i += SPECIES.length()) {
      var va = jdk.incubator.vector.FloatVector.fromArray(SPECIES, v1, i);
      var vb = jdk.incubator.vector.FloatVector.fromArray(SPECIES, v2, i);
      var diff = va.sub(vb);
      sumSq += diff.mul(diff).reduceLanes(VectorOperators.ADD);
    }

    // Scalar loop for the remainder
    for (; i < v1.length; i++) {
      double diff = v1[i] - v2[i];
      sumSq += diff * diff;
    }

    return sumSq;
  }

  /**
   * Overrides the default {@code computeSquared} to avoid redundant calculations.
   *
   * @param a The first vector.
   * @param b The second vector.
   * @return The squared Euclidean distance, calculated directly.
   */
  @Override
  public double computeSquared(FloatVector a, FloatVector b) {
    return compute(a, b);
  }

  /**
   * Gets the name of the distance metric.
   *
   * @return The string "SquaredEuclidean".
   */
  @Override
  public String getName() {
    return "SquaredEuclidean";
  }
}
