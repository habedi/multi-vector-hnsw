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
    void testComputeWithIdenticalVectors() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});

        // Identical vectors have cosine similarity of 1, so distance is 0
        assertEquals(0.0, distance.compute(v1, v2), 0.0001);
    }

    @Test
    void testComputeWithOrthogonalVectors() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 0.0f, 0.0f});
        FloatVector v2 = new FloatVector(new float[] {0.0f, 1.0f, 0.0f});

        // Orthogonal vectors have cosine similarity of 0, so distance is 1
        assertEquals(1.0, distance.compute(v1, v2), 0.0001);
    }

    @Test
    void testComputeWithOppositeVectors() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {-1.0f, -2.0f, -3.0f});

        // Opposite vectors have cosine similarity of -1, so distance is 2
        assertEquals(2.0, distance.compute(v1, v2), 0.0001);
    }

    @Test
    void testComputeWithDifferentLengthVectors() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});
        FloatVector v2 = new FloatVector(new float[] {4.0f, 5.0f});

        assertThrows(IllegalArgumentException.class, () -> distance.compute(v1, v2));
    }

    @Test
    void testComputeWithZeroVector() {
        FloatVector v1 = new FloatVector(new float[] {0.0f, 0.0f, 0.0f});
        FloatVector v2 = new FloatVector(new float[] {1.0f, 2.0f, 3.0f});

        // When one vector is zero, cosine similarity is 0, so distance is 1
        assertEquals(1.0, distance.compute(v1, v2), 0.0001);
    }

    @Test
    void testComputeWithBothZeroVectors() {
        FloatVector v1 = new FloatVector(new float[] {0.0f, 0.0f, 0.0f});
        FloatVector v2 = new FloatVector(new float[] {0.0f, 0.0f, 0.0f});

        // When both vectors are zero, cosine similarity is defined as 0, so distance is 1
        assertEquals(1.0, distance.compute(v1, v2), 0.0001);
    }

    @Test
    void testComputeSquared() {
        FloatVector v1 = new FloatVector(new float[] {1.0f, 0.0f, 0.0f});
        FloatVector v2 = new FloatVector(new float[] {0.0f, 1.0f, 0.0f});

        // Cosine distance is 1, so squared distance is 1
        assertEquals(1.0, distance.computeSquared(v1, v2), 0.0001);
    }

    @Test
    void testGetName() {
        assertEquals("Cosine", distance.getName());
    }
}
