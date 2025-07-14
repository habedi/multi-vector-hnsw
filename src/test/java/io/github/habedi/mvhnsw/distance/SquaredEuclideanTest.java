package io.github.habedi.mvhnsw.distance;

import static jdk.incubator.vector.FloatVector.SPECIES_PREFERRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.habedi.mvhnsw.common.FloatVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SquaredEuclideanTest {

  private static final int SIMD_LANE_COUNT = SPECIES_PREFERRED.length();
  private SquaredEuclidean distance;

  @BeforeEach
  void setUp() {
    distance = new SquaredEuclidean();
  }

  @Test
  void testDistanceCalculation() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f, 3.0f);
    FloatVector v2 = FloatVector.of(4.0f, 5.0f, 6.0f);
    // (4-1)^2 + (5-2)^2 + (6-3)^2 = 9 + 9 + 9 = 27
    assertEquals(27.0, distance.compute(v1, v2), 0.001);
  }

  @Test
  void testZeroDistance() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f, 3.0f);
    assertEquals(0.0, distance.compute(v1, v1), 0.0001);
  }

  @Test
  void testMismatchedVectorLengths() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f, 3.0f);
    FloatVector v2 = FloatVector.of(4.0f, 5.0f);
    assertThrows(IllegalArgumentException.class, () -> distance.compute(v1, v2));
  }

  @Test
  void testSimdAndScalarLoopCoverage() {
    // A vector size that guarantees both the vectorized and scalar loops are hit
    final int size = SIMD_LANE_COUNT * 2 + 1;
    float[] arr1 = new float[size];
    float[] arr2 = new float[size];
    for (int i = 0; i < size; i++) {
      arr1[i] = 1.0f;
      arr2[i] = 3.0f;
    }
    FloatVector v1 = new FloatVector(arr1);
    FloatVector v2 = new FloatVector(arr2);

    // (3-1)^2 = 4, summed over all elements
    assertEquals(size * 4.0, distance.compute(v1, v2), 0.001);
  }
}
