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
        return List.of(euclidean, (Distance<FloatVector>) (Distance<?>) cosine);
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
                NullPointerException.class,
                () -> new WeightedAverageDistance(createDistancesList(), null));
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
    void testComputeWithIdenticalVectors() {
        List<FloatVector> vectors1 =
                List.of(
                        new FloatVector(new float[] {1.0f, 2.0f}),
                        new FloatVector(new float[] {3.0f, 4.0f}));
        List<FloatVector> vectors2 =
                List.of(
                        new FloatVector(new float[] {1.0f, 2.0f}),
                        new FloatVector(new float[] {3.0f, 4.0f}));

        assertEquals(0.0, distance.compute(vectors1, vectors2), 0.0001);
    }

    @Test
    void testComputeWithDifferentVectors() {
        List<FloatVector> vectors1 =
                List.of(
                        new FloatVector(new float[] {1.0f, 2.0f}),
                        new FloatVector(new float[] {1.0f, 0.0f}));
        List<FloatVector> vectors2 =
                List.of(
                        new FloatVector(new float[] {4.0f, 6.0f}),
                        new FloatVector(new float[] {0.0f, 1.0f}));

        assertEquals(3.8, distance.compute(vectors1, vectors2), 0.0001);
    }

    @Test
    void testComputeWithDifferentVectorCounts() {
        List<FloatVector> vectors1 =
                List.of(
                        new FloatVector(new float[] {1.0f, 2.0f}),
                        new FloatVector(new float[] {1.0f, 0.0f}));
        List<FloatVector> vectors2 = List.of(new FloatVector(new float[] {4.0f, 6.0f}));

        assertThrows(IllegalArgumentException.class, () -> distance.compute(vectors1, vectors2));
    }
}
