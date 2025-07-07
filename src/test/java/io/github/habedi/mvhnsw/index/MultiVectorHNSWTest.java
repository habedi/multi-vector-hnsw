// src/test/java/io/github/habedi/mvhnsw/index/MultiVectorHNSWTest.java
package io.github.habedi.mvhnsw.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Euclidean;
import io.github.habedi.mvhnsw.distance.MultiVectorDistance;
import io.github.habedi.mvhnsw.distance.WeightedAverageDistance;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiVectorHNSWTest {

    private MultiVectorHNSW<String> index;

    @BeforeEach
    void setUp() {
        MultiVectorDistance<FloatVector> distance =
                new WeightedAverageDistance(
                        Arrays.asList(new Euclidean(), new Euclidean()), new float[] {0.5f, 0.5f});
        index =
                new MultiVectorHNSW.Builder<String>(distance)
                        .withM(10)
                        .withEfConstruction(100)
                        .build();
    }

    @Test
    void testAddAndGet() {
        FloatVector vector1 = FloatVector.of(1.0f, 2.0f, 3.0f);
        FloatVector vector2 = FloatVector.of(4.0f, 5.0f, 6.0f);
        List<FloatVector> vectors = Arrays.asList(vector1, vector2);

        long id = 123L;
        String payload = "test payload";
        TestItem item = new TestItem(id, vectors, payload);
        index.add(item);

        assertEquals(1, index.size());
        Optional<Item<FloatVector, String>> retrievedItem = index.get(id);
        assertTrue(retrievedItem.isPresent());
        assertEquals(id, retrievedItem.get().getId());
        assertEquals(vectors, retrievedItem.get().getVectors());
        assertEquals(payload, retrievedItem.get().getPayload());
    }

    @Test
    void testRemove() {
        long id = 123L;
        FloatVector vector = FloatVector.of(1.0f, 2.0f, 3.0f);
        TestItem item = new TestItem(id, List.of(vector), "test payload");
        index.add(item);
        assertEquals(1, index.size());

        boolean removed = index.remove(id);

        assertTrue(removed);
        assertEquals(0, index.size());
        assertTrue(index.get(id).isEmpty());
    }

    @Test
    void testSearch() {
        FloatVector v1a = FloatVector.of(1.0f, 1.0f, 1.0f);
        FloatVector v1b = FloatVector.of(1.0f, 1.0f, 1.0f);
        index.add(new TestItem(1, Arrays.asList(v1a, v1b), "item 1"));

        FloatVector v2a = FloatVector.of(10.0f, 10.0f, 10.0f);
        FloatVector v2b = FloatVector.of(10.0f, 10.0f, 10.0f);
        index.add(new TestItem(2, Arrays.asList(v2a, v2b), "item 2"));

        FloatVector queryVector1 = FloatVector.of(1.1f, 1.1f, 1.1f);
        FloatVector queryVector2 = FloatVector.of(1.1f, 1.1f, 1.1f);
        List<SearchResult<FloatVector, String>> results =
                index.search(Arrays.asList(queryVector1, queryVector2), 1);

        assertEquals(1, results.size());
        assertEquals(1, results.get(0).item().getId());
    }

    @Test
    void testSaveAndLoad(@TempDir File tempDir) throws IOException, ClassNotFoundException {
        for (long i = 1; i <= 50; i++) {
            FloatVector vector1 = FloatVector.of(i, i + 1, i + 2);
            FloatVector vector2 = FloatVector.of(i + 3, i + 4, i + 5);
            index.add(new TestItem(i, Arrays.asList(vector1, vector2), "payload " + i));
        }
        assertEquals(50, index.size());

        File indexPath = new File(tempDir, "my.index");
        index.save(indexPath.toPath());
        assertTrue(Files.exists(indexPath.toPath()));

        MultiVectorHNSW<String> loadedIndex = MultiVectorHNSW.load(indexPath.toPath());
        assertEquals(50, loadedIndex.size());

        Optional<Item<FloatVector, String>> item = loadedIndex.get(25L);
        assertTrue(item.isPresent());
        assertEquals("payload 25", item.get().getPayload());

        // Verify search works on loaded index
        List<SearchResult<FloatVector, String>> results =
                loadedIndex.search(
                        List.of(FloatVector.of(25f, 26f, 27f), FloatVector.of(28f, 29f, 30f)), 1);
        assertEquals(1, results.size());
        assertEquals(25L, results.get(0).item().getId());
    }

    /** A simple implementation of the Item interface for testing purposes. */
    private static class TestItem implements Item<FloatVector, String>, Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private final long id;
        private final List<FloatVector> vectors;
        private final String payload;

        public TestItem(long id, List<FloatVector> vectors, String payload) {
            this.id = id;
            this.vectors = vectors;
            this.payload = payload;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public List<FloatVector> getVectors() {
            return vectors;
        }

        @Override
        public String getPayload() {
            return payload;
        }
    }
}
