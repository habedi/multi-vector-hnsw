// src/test/java/io/github/habedi/mvhnsw/distance/DotProductTest.java
package io.github.habedi.mvhnsw.distance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.habedi.mvhnsw.common.FloatVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DotProductTest {

  private DotProduct distance;

  @BeforeEach
  void setUp() {
    distance = new DotProduct();
  }

  @Test
  void testCompute() {
    FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
    FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f, 6.0f});

    // Dot product is 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
    // Distance is the negative dot product
    assertEquals(-32.0, distance.compute(v1, v2), 0.0001);
  }

  @Test
  void testComputeWithOrthogonalVectors() {
    FloatVector v1 = new FloatVector(new float[] {1.0f, 0.0f});
    FloatVector v2 = new FloatVector(new float[] {0.0f, 1.0f});

    // Dot product is 0, so distance is 0
    assertEquals(0.0, distance.compute(v1, v2), 0.0001);
  }

  @Test
  void testComputeWithDifferentLengthVectors() {
    FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
    FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f});

    assertThrows(IllegalArgumentException.class, () -> distance.compute(v1, v2));
  }

  @Test
  void testGetName() {
    assertEquals("DotProduct", distance.getName());
  }
}
