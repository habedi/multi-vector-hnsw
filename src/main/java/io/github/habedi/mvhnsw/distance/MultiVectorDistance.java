package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.util.List;

/**
 * Defines a strategy for computing an aggregated distance between two items, where each item is
 * represented by a list of vectors.
 *
 * <p>This interface allows for custom aggregation logic, such as weighted averages, minimums, or
 * maximums of the distances between corresponding vector pairs.
 */
public interface MultiVectorDistance {

  /**
   * Computes the aggregated distance between two lists of vectors.
   *
   * @param vectors1 The list of vectors representing the first item.
   * @param vectors2 The list of vectors representing the second item.
   * @return A single, non-negative value representing the aggregated distance.
   * @throws IllegalArgumentException if the lists are not of equal size or do not meet other
   *     specific requirements of the implementation.
   */
  double compute(List<FloatVector> vectors1, List<FloatVector> vectors2);
}
