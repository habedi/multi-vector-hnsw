package io.github.habedi.mvhnsw.distance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.habedi.mvhnsw.common.FloatVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CosineTest {

  private Cosine distance;

  @BeforeEach
  void setUp() {
    distance = new Cosine();
  }

  @Test
  void testIdenticalAndOppositeVectors() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f, 3.0f);
    FloatVector v2 = FloatVector.of(1.0f, 2.0f, 3.0f); // Identical
    FloatVector v3 = FloatVector.of(-1.0f, -2.0f, -3.0f); // Opposite

    // Similarity is 1, so distance is 1 - 1 = 0
    assertEquals(0.0, distance.compute(v1, v2), 0.0001);
    // Similarity is -1, so distance is 1 - (-1) = 2
    assertEquals(2.0, distance.compute(v1, v3), 0.0001);
  }

  @Test
  void testOrthogonalVectors() {
    FloatVector v1 = FloatVector.of(1.0f, 0.0f);
    FloatVector v2 = FloatVector.of(0.0f, 1.0f);
    // Similarity is 0, so distance is 1 - 0 = 1
    assertEquals(1.0, distance.compute(v1, v2), 0.0001);
  }

  @Test
  void testZeroVectors() {
    FloatVector v1 = FloatVector.of(0.0f, 0.0f, 0.0f);
    FloatVector v2 = FloatVector.of(1.0f, 2.0f, 3.0f);
    // Similarity is 0 if a norm is 0, so distance is 1
    assertEquals(1.0, distance.compute(v1, v2), 0.0001);
    assertEquals(1.0, distance.compute(v1, v1), 0.0001);
  }

  @Test
  void testMismatchedVectorLengths() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f);
    FloatVector v2 = FloatVector.of(1.0f, 2.0f, 3.0f);
    assertThrows(IllegalArgumentException.class, () -> distance.compute(v1, v2));
  }
}
