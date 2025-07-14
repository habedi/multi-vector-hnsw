package io.github.habedi.mvhnsw.common;

import static jdk.incubator.vector.FloatVector.SPECIES_PREFERRED;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FloatVectorTest {

  private static final int SIMD_LANE_COUNT = SPECIES_PREFERRED.length();

  @Test
  void testCoreFunctionality() {
    FloatVector vector = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
    assertEquals(3, vector.length());
    assertEquals(1.0f, vector.get(0));
    assertEquals(2.0f, vector.getPrimitive(1));
    assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f}, vector.toPrimitiveArray());
    assertArrayEquals(new Float[] {1.0f, 2.0f, 3.0f}, vector.toArray());
  }

  @Test
  void testInvalidConstructors() {
    assertThrows(IllegalArgumentException.class, () -> new FloatVector(new float[] {}));
    assertThrows(IllegalArgumentException.class, () -> new FloatVector(null));
    assertThrows(IllegalArgumentException.class, FloatVector::of);
  }

  @Test
  void testArithmeticOperations() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f, 3.0f);
    FloatVector v2 = FloatVector.of(4.0f, 5.0f, 6.0f);

    // Addition
    FloatVector sum = v1.add(v2);
    assertArrayEquals(new float[] {5.0f, 7.0f, 9.0f}, sum.toPrimitiveArray());

    // Multiplication
    FloatVector product = v1.mul(v2);
    assertArrayEquals(new float[] {4.0f, 10.0f, 18.0f}, product.toPrimitiveArray());

    // Dot Product
    double dot = v1.dot(v2);
    assertEquals(32.0, dot, 0.0001);
  }

  @Test
  void testArithmeticWithMismatchedLengths() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f, 3.0f);
    FloatVector v2 = FloatVector.of(4.0f, 5.0f);
    assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
    assertThrows(IllegalArgumentException.class, () -> v1.mul(v2));
    assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
  }

  @Test
  void testArithmeticWithUnsupportedVectorType() {
    // This test hits the `instanceof` check in the arithmetic methods.
    FloatVector v1 = FloatVector.of(1f, 2f);
    Vector<Float> unsupportedVector =
        new Vector<>() {
          public int length() {
            return 2;
          }

          public Float get(int i) {
            return 1f;
          }

          public Float[] toArray() {
            return new Float[0];
          }

          public Vector<Float> add(Vector<Float> other) {
            return null;
          }

          public Vector<Float> mul(Vector<Float> other) {
            return null;
          }

          public double dot(Vector<Float> other) {
            return 0;
          }

          public double norm() {
            return 0;
          }

          public double cosine(Vector<Float> other) {
            return 0;
          }
        };
    assertThrows(UnsupportedOperationException.class, () -> v1.add(unsupportedVector));
    assertThrows(UnsupportedOperationException.class, () -> v1.mul(unsupportedVector));
    assertThrows(UnsupportedOperationException.class, () -> v1.dot(unsupportedVector));
  }

  @Test
  void testNormAndCosine() {
    FloatVector v1 = FloatVector.of(3.0f, 4.0f);
    assertEquals(5.0, v1.norm(), 0.0001);

    FloatVector v2 = FloatVector.of(-4.0f, 3.0f); // Orthogonal
    assertEquals(0.0, v1.cosine(v2), 0.0001);

    FloatVector v3 = FloatVector.of(6.0f, 8.0f); // Same direction
    assertEquals(1.0, v1.cosine(v3), 0.0001);
  }

  @Test
  void testNormWithSimdAndScalarLoops() {
    // This test ensures both the vectorized and scalar remainder loops in norm() are executed.
    final int size = SIMD_LANE_COUNT + 1;
    float[] data = new float[size];
    for (int i = 0; i < size; i++) {
      data[i] = 1.0f;
    }
    FloatVector v = new FloatVector(data);
    assertEquals(Math.sqrt(size), v.norm(), 0.0001);
  }

  @Test
  void testCosineWithZeroVector() {
    FloatVector v1 = FloatVector.of(0.0f, 0.0f);
    FloatVector v2 = FloatVector.of(1.0f, 1.0f);
    assertEquals(0.0, v1.cosine(v2), 0.0001);
    assertEquals(0.0, v2.cosine(v1), 0.0001);
    assertEquals(0.0, v1.cosine(v1), 0.0001);
  }

  @Test
  void testEqualsAndHashCodeContract() {
    FloatVector v1 = FloatVector.of(1.0f, 2.0f);
    FloatVector v2 = FloatVector.of(1.0f, 2.0f);
    FloatVector v3 = FloatVector.of(2.0f, 1.0f);

    assertEquals(v1, v2);
    assertEquals(v1.hashCode(), v2.hashCode());
    assertNotEquals(v1, v3);
    assertNotEquals(null, v1);
    assertNotEquals(new Object(), v1);
  }

  @Test
  void testToString() {
    FloatVector v = FloatVector.of(1.1f, 2.2f);
    assertTrue(v.toString().contains("1.1"));
    assertTrue(v.toString().contains("2.2"));
  }
}
