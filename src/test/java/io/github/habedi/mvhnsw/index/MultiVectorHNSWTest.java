package io.github.habedi.mvhnsw.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Cosine;
import io.github.habedi.mvhnsw.distance.Euclidean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultiVectorHNSWTest {

    private Index index;

    @BeforeEach
    void setUp() {
        index =
                MultiVectorHNSW.builder()
                        .withM(10)
                        .withEfConstruction(100)
                        .withWeightedAverageDistance()
                        .addDistance(new Euclidean(), 0.5f)
                        .addDistance(new Cosine(), 0.5f)
                        .and()
                        .build();
    }

    @Test
    void testAddAndGet() {
        long id = 123L;
        List<FloatVector> vectors =
                List.of(FloatVector.of(1.0f, 2.0f, 3.0f), FloatVector.of(4.0f, 5.0f, 6.0f));

        index.add(id, vectors);

        assertEquals(1, index.size());
        Optional<List<FloatVector>> retrieved = index.get(id);
        assertTrue(retrieved.isPresent());
        assertEquals(vectors, retrieved.get());
    }

    @Test
    void testRemove() {
        long id = 123L;
        List<FloatVector> vectors = List.of(FloatVector.of(1.0f, 2.0f, 3.0f));
        index.add(id, vectors);
        assertEquals(1, index.size());

        boolean removed = index.remove(id);

        assertTrue(removed);
        assertEquals(0, index.size());
        assertTrue(index.get(id).isEmpty());
    }

    @Test
    void testSearch() {
        long id1 = 1L;
        List<FloatVector> vectors1 =
                List.of(FloatVector.of(1.0f, 1.0f), FloatVector.of(1.0f, 0.0f));
        index.add(id1, vectors1);

        long id2 = 2L;
        List<FloatVector> vectors2 =
                List.of(FloatVector.of(10.0f, 10.0f), FloatVector.of(0.0f, 1.0f));
        index.add(id2, vectors2);

        List<FloatVector> queryVectors =
                List.of(FloatVector.of(1.1f, 1.1f), FloatVector.of(0.9f, 0.1f));
        List<SearchResult> results = index.search(queryVectors, 1);

        assertEquals(1, results.size());
        assertEquals(id1, results.get(0).id());
    }

    @Test
    void testSaveAndLoad(@TempDir File tempDir) throws IOException, ClassNotFoundException {
        for (long i = 1; i <= 50; i++) {
            List<FloatVector> vectors =
                    List.of(FloatVector.of(i, i + 1, i + 2), FloatVector.of(i + 3, i + 4, i + 5));
            index.add(i, vectors);
        }
        assertEquals(50, index.size());

        File indexPath = new File(tempDir, "my.index");
        index.save(indexPath.toPath());
        assertTrue(Files.exists(indexPath.toPath()));

        Index loadedIndex = MultiVectorHNSW.load(indexPath.toPath());
        assertEquals(50, loadedIndex.size());

        Optional<List<FloatVector>> vectors = loadedIndex.get(25L);
        assertTrue(vectors.isPresent());
        assertEquals(FloatVector.of(25f, 26f, 27f), vectors.get().get(0));

        List<SearchResult> results =
                loadedIndex.search(
                        List.of(FloatVector.of(25f, 26f, 27f), FloatVector.of(28f, 29f, 30f)), 1);
        assertEquals(1, results.size());
        assertEquals(25L, results.get(0).id());
    }
}
