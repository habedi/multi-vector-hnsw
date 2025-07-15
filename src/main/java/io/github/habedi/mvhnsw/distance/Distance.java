package io.github.habedi.mvhnsw.distance;

/**
 * Represents a function that computes the distance between two items of the same type.
 *
 * @param <T> the type of items to measure the distance between.
 */
public interface Distance<T> {

  /**
   * Computes the distance between two items.
   *
   * @param a The first item.
   * @param b The second item.
   * @return A value representing the distance. Lower values typically indicate closer items. This
   *     value is not required to be non-negative.
   */
  double compute(T a, T b);

  /**
   * Computes the squared distance between two items.
   *
   * <p>This default implementation returns the square of the value from {@link #compute(Object,
   * Object)}. For distance metrics where the squared value can be calculated more efficiently
   * (e.g., Euclidean distance), this method should be overridden.
   *
   * @param a The first item.
   * @param b The second item.
   * @return The squared distance.
   */
  default double computeSquared(T a, T b) {
    double dist = compute(a, b);
    return dist * dist;
  }

  /**
   * Gets the name of the distance metric.
   *
   * @return A string identifier for the distance function (e.g., "Cosine", "Euclidean").
   */
  String getName();
}
