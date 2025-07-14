package io.github.habedi.mvhnsw.distance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeightedAverageDistanceTest {

  private Distance<FloatVector> squaredEuclidean;
  private Distance<FloatVector> cosine;

  @BeforeEach
  void setUp() {
    squaredEuclidean = new SquaredEuclidean();
    cosine = new Cosine();
  }

  @Test
  void testDistanceCalculation() {
    List<Distance<FloatVector>> distances = List.of(squaredEuclidean, cosine);
    float[] weights = {0.7f, 0.3f};
    WeightedAverageDistance weightedDistance = new WeightedAverageDistance(distances, weights);

    List<FloatVector> vectors1 = List.of(FloatVector.of(1.0f, 2.0f), FloatVector.of(1.0f, 0.0f));
    List<FloatVector> vectors2 = List.of(FloatVector.of(4.0f, 6.0f), FloatVector.of(0.0f, 1.0f));

    // Squared Euclidean dist = (4-1)^2 + (6-2)^2 = 9 + 16 = 25.0
    // Cosine dist = 1.0 (for orthogonal vectors)
    // Total = (0.7 * 25.0) + (0.3 * 1.0) = 17.5 + 0.3 = 17.8
    assertEquals(17.8, weightedDistance.compute(vectors1, vectors2), 0.0001);
  }

  @Test
  void testWeightNormalization() {
    List<Distance<FloatVector>> distances = List.of(squaredEuclidean, cosine);
    // Weights [1.4, 0.6] sum to 2.0, so they should be normalized to [0.7, 0.3]
    float[] unnormalizedWeights = {1.4f, 0.6f};
    WeightedAverageDistance weightedDistance =
        new WeightedAverageDistance(distances, unnormalizedWeights);

    List<FloatVector> vectors1 = List.of(FloatVector.of(1.0f, 2.0f), FloatVector.of(1.0f, 0.0f));
    List<FloatVector> vectors2 = List.of(FloatVector.of(4.0f, 6.0f), FloatVector.of(0.0f, 1.0f));

    assertEquals(17.8, weightedDistance.compute(vectors1, vectors2), 0.0001);
  }

  @Test
  void testInvalidConstructorArguments() {
    List<Distance<FloatVector>> distances = List.of(squaredEuclidean);

    assertThrows(
        NullPointerException.class, () -> new WeightedAverageDistance(null, new float[] {1.0f}));
    assertThrows(NullPointerException.class, () -> new WeightedAverageDistance(distances, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeightedAverageDistance(Collections.emptyList(), new float[] {}));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new WeightedAverageDistance(distances, new float[] {0.5f, 0.5f})); // Mismatched lengths
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeightedAverageDistance(distances, new float[] {-1.0f})); // Negative weight
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeightedAverageDistance(distances, new float[] {0.0f})); // Zero sum weight
  }

  @Test
  void testInvalidComputeArguments() {
    List<Distance<FloatVector>> distances = List.of(squaredEuclidean);
    WeightedAverageDistance weightedDistance =
        new WeightedAverageDistance(distances, new float[] {1.0f});

    List<FloatVector> vectors1 = List.of(FloatVector.of(1.0f, 2.0f));
    List<FloatVector> vectors2 =
        List.of(FloatVector.of(4.0f, 6.0f), FloatVector.of(0.0f, 1.0f)); // Mismatched list sizes
    List<FloatVector> vectors3 =
        List.of(
            FloatVector.of(1.0f, 2.0f),
            FloatVector.of(1.0f, 0.0f)); // Too many vectors for configured distances

    assertThrows(
        IllegalArgumentException.class, () -> weightedDistance.compute(vectors1, vectors2));
    assertThrows(
        IllegalArgumentException.class, () -> weightedDistance.compute(vectors3, vectors3));
  }
}
