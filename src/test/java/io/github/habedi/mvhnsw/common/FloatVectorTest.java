package io.github.habedi.mvhnsw.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FloatVectorTest {

    @Test
    void testConstructorAndLength() {
        FloatVector vector = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        assertEquals(3, vector.length());
    }

    @Test
    void testConstructorWithEmptyArray() {
        assertThrows(IllegalArgumentException.class, () -> new FloatVector(new float[] {}));
    }

    @Test
    void testConstructorWithNullArray() {
        assertThrows(IllegalArgumentException.class, () -> new FloatVector(null));
    }

    @Test
    void testStaticFactoryMethod() {
        FloatVector vector = FloatVector.of(1.0f, 2.0f, 3.0f);
        assertEquals(3, vector.length());
        assertEquals(1.0f, vector.get(0));
        assertEquals(2.0f, vector.get(1));
        assertEquals(3.0f, vector.get(2));
    }

    @Test
    void testGetMethod() {
        FloatVector vector = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        assertEquals(1.0f, vector.get(0));
        assertEquals(2.0f, vector.get(1));
        assertEquals(3.0f, vector.get(2));
    }

    @Test
    void testGetPrimitiveMethod() {
        FloatVector vector = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        assertEquals(1.0f, vector.getPrimitive(0));
        assertEquals(2.0f, vector.getPrimitive(1));
        assertEquals(3.0f, vector.getPrimitive(2));
    }

    @Test
    void testToArray() {
        FloatVector vector = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        Float[] array = vector.toArray();
        assertEquals(3, array.length);
        assertEquals(1.0f, array[0]);
        assertEquals(2.0f, array[1]);
        assertEquals(3.0f, array[2]);
    }

    @Test
    void testToPrimitiveArray() {
        float[] original = {1.0f, 2.0f, 3.0f};
        FloatVector vector = new FloatVector(original);
        float[] array = vector.toPrimitiveArray();

        assertNotSame(original, array);
        assertArrayEquals(original, array);
    }

    @Test
    void testAddMethod() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f, 6.0f});
        FloatVector result = v1.add(v2);

        assertArrayEquals(new float[] {5.0f, 7.0f, 9.0f}, result.toPrimitiveArray());
    }

    @Test
    void testAddMethodWithDifferentLengths() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f});

        assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
    }

    @Test
    void testMulMethod() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f, 6.0f});
        FloatVector result = v1.mul(v2);

        assertArrayEquals(new float[] {4.0f, 10.0f, 18.0f}, result.toPrimitiveArray());
    }

    @Test
    void testMulMethodWithDifferentLengths() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f});

        assertThrows(IllegalArgumentException.class, () -> v1.mul(v2));
    }

    @Test
    void testDotProduct() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f, 6.0f});

        double dot = v1.dot(v2);
        assertEquals(32.0, dot, 0.0001);
    }

    @Test
    void testDotProductWithDifferentLengths() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f});

        assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
    }

    @Test
    void testDotProductWithSingleElement() {
        FloatVector v1 = FloatVector.of(5.0f);
        FloatVector v2 = FloatVector.of(10.0f);
        assertEquals(50.0, v1.dot(v2), 0.0001);
    }

    @Test
    void testNorm() {
        FloatVector v = new FloatVector(new float[] {3.0f, 4.0f});
        assertEquals(5.0, v.norm(), 0.0001);
        assertEquals(5.0, v.norm(), 0.0001); // Test caching
    }

    @Test
    void testCosine() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 0.0f});
        FloatVector v2 = new FloatVector(new float[] {0.0f, 1.0f});
        assertEquals(0.0, v1.cosine(v2), 0.0001); // Orthogonal vectors

        FloatVector v3 = new FloatVector(new float[] {1.0f, 1.0f});
        FloatVector v4 = new FloatVector(new float[] {2.0f, 2.0f});
        assertEquals(1.0, v3.cosine(v4), 0.0001); // Same direction

        FloatVector v5 = new FloatVector(new float[] {1.0f, 1.0f});
        FloatVector v6 = new FloatVector(new float[] {-1.0f, -1.0f});
        assertEquals(-1.0, v5.cosine(v6), 0.0001); // Opposite direction
    }

    @Test
    void testCosineWithZeroVector() {
        FloatVector v1 = new FloatVector(new float[] {0.0f, 0.0f});
        FloatVector v2 = new FloatVector(new float[] {1.0f, 1.0f});
        assertEquals(0.0, v1.cosine(v2), 0.0001);
        assertEquals(0.0, v2.cosine(v1), 0.0001);
    }

    @Test
    void testEquals() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v3 = new FloatVector(new float[] {3.0f, 2.0f, 1.0f});

        assertEquals(v1, v2);
        assertNotEquals(v1, v3);
        assertNotEquals(null, v1);
        assertNotEquals("not a vector", v1);
    }

    @Test
    void testHashCode() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});

        assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    void testToString() {
        FloatVector v = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        String str = v.toString();

        assertTrue(str.startsWith("FloatVector"));
        assertTrue(str.contains("[1.0, 2.0, 3.0]"));
    }
}
