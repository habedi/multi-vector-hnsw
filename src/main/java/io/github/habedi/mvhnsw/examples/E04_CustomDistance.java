package io.github.habedi.mvhnsw.examples;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Distance;
import io.github.habedi.mvhnsw.distance.DotProduct;
import io.github.habedi.mvhnsw.distance.MultiVectorDistance;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Example 4: Demonstrates the library's extensibility.
 *
 * <p>We create a completely new distance aggregation strategy (`MaxDistance`) and pass it to the
 * generic `.withDistance()` builder method.
 */
public class E04_CustomDistance {

  /**
   * A custom distance aggregation that finds the *maximum* distance among all vector pairs, using a
   * base distance function.
   */
  public static class MaxDistance implements MultiVectorDistance, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final Distance<FloatVector> baseDistance;

    public MaxDistance(Distance<FloatVector> baseDistance) {
      this.baseDistance = Objects.requireNonNull(baseDistance, "Base distance cannot be null.");
    }

    @Override
    public double compute(List<FloatVector> vectors1, List<FloatVector> vectors2) {
      if (vectors1.size() != vectors2.size() || vectors1.isEmpty()) {
        throw new IllegalArgumentException("Vector lists must be non-empty and of equal size.");
      }
      double maxDistance = Double.MIN_VALUE;
      for (int i = 0; i < vectors1.size(); i++) {
        double dist = baseDistance.compute(vectors1.get(i), vectors2.get(i));
        if (dist > maxDistance) {
          maxDistance = dist;
        }
      }
      return maxDistance;
    }
  }

  public static void main(String[] args) {
    // 1. Create an instance of our new "MaxDistance" aggregation
    var maxDotProductDistance = new MaxDistance(new DotProduct());

    // 2. Pass the custom distance function directly to the builder
    Index index =
        MultiVectorHNSW.builder()
            .withM(16)
            .withEfConstruction(200)
            .withDistance(maxDotProductDistance) // Use the generic `withDistance` method
            .build();

    // 3. Add and search data
    index.add(1L, List.of(FloatVector.of(0.9f, 0.1f), FloatVector.of(0.2f, 0.8f)));
    index.add(2L, List.of(FloatVector.of(0.1f, 0.9f), FloatVector.of(0.8f, 0.2f)));

    List<FloatVector> query = List.of(FloatVector.of(1.0f, 0.0f), FloatVector.of(0.0f, 1.0f));
    List<SearchResult> results = index.search(query, 1);

    // Distance for item 1 is max(-0.9, -0.8) = -0.8
    // Distance for item 2 is max(-0.1, -0.2) = -0.1
    // Since smaller distance is better, item 1 should be the closest.
    System.out.println("Search results with MaxDistance aggregation:");
    results.forEach(System.out::println);
  }
}
