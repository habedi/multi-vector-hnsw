package io.github.habedi.mvhnsw.distance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.habedi.mvhnsw.common.FloatVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EuclideanTest {

  private Euclidean distance;

  @BeforeEach
  void setUp() {
    distance = new Euclidean();
  }

  @Test
  void testComputeWithIdenticalVectors() {
    FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
    FloatVector v2 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});

    assertEquals(0.0, distance.compute(v1, v2), 0.0001);
  }

  @Test
  void testComputeWithDifferentVectors() {
    FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
    FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f, 6.0f});

    // sqrt((4-1)^2 + (5-2)^2 + (6-3)^2) = sqrt(9 + 9 + 9) = sqrt(27) = 5.196
    assertEquals(5.196, distance.compute(v1, v2), 0.001);
  }

  @Test
  void testComputeWithDifferentLengthVectors() {
    FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
    FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f});

    assertThrows(IllegalArgumentException.class, () -> distance.compute(v1, v2));
  }

  @Test
  void testComputeSquared() {
    FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
    FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f, 6.0f});

    // (4-1)^2 + (5-2)^2 + (6-3)^2 = 9 + 9 + 9 = 27
    assertEquals(27.0, distance.computeSquared(v1, v2), 0.0001);
  }

  @Test
  void testComputeSquaredWithDifferentLengthVectors() {
    FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
    FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f});

    assertThrows(IllegalArgumentException.class, () -> distance.computeSquared(v1, v2));
  }

  @Test
  void testGetName() {
    assertEquals("Euclidean", distance.getName());
  }
}
