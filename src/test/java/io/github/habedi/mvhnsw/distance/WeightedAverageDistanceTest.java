package io.github.habedi.mvhnsw.distance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.index.Item;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeightedAverageDistanceTest {

    private WeightedAverageDistance distance;
    private Euclidean euclidean;
    private Cosine cosine;

    // Helper method to create a properly typed list of distances
    private List<Distance<FloatVector>> createDistancesList() {
        return List.of(euclidean, (Distance<FloatVector>) (Distance<?>) cosine);
    }

    @BeforeEach
    void setUp() {
        euclidean = new Euclidean();
        cosine = new Cosine();

        distance = new WeightedAverageDistance(createDistancesList(), new float[] {0.7f, 0.3f});
    }

    // Helper method to create a test item with multiple vectors
    private Item<FloatVector, String> createItem(long id, String payload, float[][] vectors) {
        return new Item<>() {
            @Override
            public long getId() {
                return id;
            }

            @Override
            public List<FloatVector> getVectors() {
                return List.of(new FloatVector(vectors[0]), new FloatVector(vectors[1]));
            }

            @Override
            public String getPayload() {
                return payload;
            }
        };
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
    void testComputeWithIdenticalItems() {
        Item<FloatVector, String> item1 =
                createItem(1, "item1", new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});
        Item<FloatVector, String> item2 =
                createItem(2, "item2", new float[][] {{1.0f, 2.0f}, {3.0f, 4.0f}});

        // Euclidean distance between [1,2] and [1,2] is 0
        // Cosine distance between [3,4] and [3,4] is 0
        // Weighted average: 0.7 * 0 + 0.3 * 0 = 0
        assertEquals(0.0, distance.compute(item1, item2), 0.0001);
    }

    @Test
    void testComputeWithDifferentItems() {
        Item<FloatVector, String> item1 =
                createItem(1, "item1", new float[][] {{1.0f, 2.0f}, {1.0f, 0.0f}});
        Item<FloatVector, String> item2 =
                createItem(2, "item2", new float[][] {{4.0f, 6.0f}, {0.0f, 1.0f}});

        // Euclidean distance between [1,2] and [4,6] is 5
        // Cosine distance between [1,0] and [0,1] is 1 (orthogonal)
        // Weighted average: 0.7 * 5 + 0.3 * 1 = 3.5 + 0.3 = 3.8
        assertEquals(3.8, distance.compute(item1, item2), 0.0001);
    }

    @Test
    void testComputeWithQueryVectors() {
        Item<FloatVector, String> item =
                createItem(1, "item1", new float[][] {{1.0f, 2.0f}, {1.0f, 0.0f}});
        List<FloatVector> queryVectors =
                List.of(
                        new FloatVector(new float[] {4.0f, 6.0f}),
                        new FloatVector(new float[] {0.0f, 1.0f}));

        // Euclidean distance between [1,2] and [4,6] is 5
        // Cosine distance between [1,0] and [0,1] is 1 (orthogonal)
        // Weighted average: 0.7 * 5 + 0.3 * 1 = 3.5 + 0.3 = 3.8
        assertEquals(3.8, distance.compute(item, queryVectors), 0.0001);
    }

    @Test
    void testComputeWithDifferentVectorCounts() {
        Item<FloatVector, String> item =
                createItem(1, "item1", new float[][] {{1.0f, 2.0f}, {1.0f, 0.0f}});
        List<FloatVector> queryVectors = List.of(new FloatVector(new float[] {4.0f, 6.0f}));

        assertThrows(IllegalArgumentException.class, () -> distance.compute(item, queryVectors));
    }
}
