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
  void testDotProductDistance() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f, 3.0f);
    FloatVector v2 = FloatVector.of(4.0f, 5.0f, 6.0f);
    // Dot product is 1*4 + 2*5 + 3*6 = 32. Distance is the negative dot product.
    assertEquals(-32.0, distance.compute(v1, v2), 0.0001);
  }

  @Test
  void testOrthogonalVectors() {
    FloatVector v1 = FloatVector.of(1.0f, 0.0f);
    FloatVector v2 = FloatVector.of(0.0f, 1.0f);
    // Dot product is 0, so distance is 0.
    assertEquals(0.0, distance.compute(v1, v2), 0.0001);
  }

  @Test
  void testZeroVector() {
    FloatVector v1 = FloatVector.of(0.0f, 0.0f, 0.0f);
    FloatVector v2 = FloatVector.of(4.0f, 5.0f, 6.0f);
    assertEquals(0.0, distance.compute(v1, v2), 0.0001);
  }

  @Test
  void testMismatchedVectorLengths() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f, 3.0f);
    FloatVector v2 = FloatVector.of(4.0f, 5.0f);
    assertThrows(IllegalArgumentException.class, () -> distance.compute(v1, v2));
  }

  @Test
  void testGetName() {
    assertEquals("DotProduct", distance.getName());
  }
}
