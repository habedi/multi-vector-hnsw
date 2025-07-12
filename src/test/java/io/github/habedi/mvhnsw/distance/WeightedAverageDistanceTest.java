package io.github.habedi.mvhnsw.distance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeightedAverageDistanceTest {

  private WeightedAverageDistance distance;
  private Euclidean euclidean;
  private Cosine cosine;

  private List<Distance<FloatVector>> createDistancesList() {
    return List.of(euclidean, cosine);
  }

  @BeforeEach
  void setUp() {
    euclidean = new Euclidean();
    cosine = new Cosine();
    distance = new WeightedAverageDistance(createDistancesList(), new float[] {0.7f, 0.3f});
  }

  @Test
  void testConstructorWithNullDistances() {
    assertThrows(
        NullPointerException.class,
        () -> new WeightedAverageDistance(null, new float[] {0.5f, 0.5f}));
  }

  @Test
  void testConstructorWithNullWeights() {
    assertThrows(
        NullPointerException.class, () -> new WeightedAverageDistance(createDistancesList(), null));
  }

  @Test
  void testConstructorWithEmptyDistances() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeightedAverageDistance(List.of(), new float[] {}));
  }

  @Test
  void testConstructorWithMismatchedDistancesAndWeights() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeightedAverageDistance(createDistancesList(), new float[] {0.5f}));
  }

  @Test
  void testConstructorWithNegativeWeights() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeightedAverageDistance(createDistancesList(), new float[] {1.0f, -0.5f}));
  }

  @Test
  void testConstructorWithZeroSumWeights() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new WeightedAverageDistance(createDistancesList(), new float[] {0.0f, 0.0f}));
  }

  @Test
  void testComputeWithIdenticalVectors() {
    List<FloatVector> vectors1 = List.of(FloatVector.of(1.0f, 2.0f), FloatVector.of(3.0f, 4.0f));
    List<FloatVector> vectors2 = List.of(FloatVector.of(1.0f, 2.0f), FloatVector.of(3.0f, 4.0f));

    assertEquals(0.0, distance.compute(vectors1, vectors2), 0.0001);
  }

  @Test
  void testComputeWithDifferentVectors() {
    List<FloatVector> vectors1 = List.of(FloatVector.of(1.0f, 2.0f), FloatVector.of(1.0f, 0.0f));
    List<FloatVector> vectors2 = List.of(FloatVector.of(4.0f, 6.0f), FloatVector.of(0.0f, 1.0f));

    // euclidean dist = sqrt((4-1)^2 + (6-2)^2) = sqrt(9 + 16) = 5.0
    // cosine dist = 1 - (dot(v1,v2) / (norm(v1)*norm(v2))) = 1 - 0 = 1.0
    // total = 0.7 * 5.0 + 0.3 * 1.0 = 3.5 + 0.3 = 3.8
    assertEquals(3.8, distance.compute(vectors1, vectors2), 0.0001);
  }

  @Test
  void testWeightNormalization() {
    // Weights [1.4, 0.6] sum to 2.0, should be normalized to [0.7, 0.3]
    WeightedAverageDistance normalizedDistance =
        new WeightedAverageDistance(createDistancesList(), new float[] {1.4f, 0.6f});

    List<FloatVector> vectors1 = List.of(FloatVector.of(1.0f, 2.0f), FloatVector.of(1.0f, 0.0f));
    List<FloatVector> vectors2 = List.of(FloatVector.of(4.0f, 6.0f), FloatVector.of(0.0f, 1.0f));

    assertEquals(3.8, normalizedDistance.compute(vectors1, vectors2), 0.0001);
  }

  @Test
  void testComputeWithMismatchedVectorListSizes() {
    List<FloatVector> vectors1 = List.of(FloatVector.of(1.0f, 2.0f), FloatVector.of(1.0f, 0.0f));
    List<FloatVector> vectors2 = List.of(FloatVector.of(4.0f, 6.0f));

    assertThrows(IllegalArgumentException.class, () -> distance.compute(vectors1, vectors2));
  }

  @Test
  void testComputeWithWrongNumberOfVectorsForDistances() {
    List<FloatVector> vectors1 = List.of(FloatVector.of(1.0f, 2.0f));
    List<FloatVector> vectors2 = List.of(FloatVector.of(4.0f, 6.0f));

    // Configured for 2 distances, but called with 1 vector pair
    assertThrows(IllegalArgumentException.class, () -> distance.compute(vectors1, vectors2));
  }
}
