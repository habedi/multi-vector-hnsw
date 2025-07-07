package io.github.habedi.mvhnsw.index;

import static org.junit.jupiter.api.Assertions.*;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class SearchResultTest {

    @Test
    void testSearchResultCreation() {
        // Create a test item
        FloatVector vector = FloatVector.of(1.0f, 2.0f, 3.0f);
        Item<FloatVector, String> item = new TestItem(123L, List.of(vector), "test payload");

        // Create a search result
        double score = 0.75;
        SearchResult<FloatVector, String> result = new SearchResult<>(item, score);

        // Verify the search result properties
        assertEquals(item, result.item());
        assertEquals(score, result.score());
    }

    @Test
    void testSearchResultEquality() {
        // Create two identical items
        FloatVector vector = FloatVector.of(1.0f, 2.0f, 3.0f);
        Item<FloatVector, String> item1 = new TestItem(123L, List.of(vector), "test payload");
        Item<FloatVector, String> item2 = new TestItem(123L, List.of(vector), "test payload");

        // Create two search results with the same properties
        SearchResult<FloatVector, String> result1 = new SearchResult<>(item1, 0.75);
        SearchResult<FloatVector, String> result2 = new SearchResult<>(item2, 0.75);

        // Verify equality
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testSearchResultInequality() {
        // Create two different items
        FloatVector vector1 = FloatVector.of(1.0f, 2.0f, 3.0f);
        FloatVector vector2 = FloatVector.of(4.0f, 5.0f, 6.0f);
        Item<FloatVector, String> item1 = new TestItem(123L, List.of(vector1), "payload1");
        Item<FloatVector, String> item2 = new TestItem(456L, List.of(vector2), "payload2");

        // Create two search results with different properties
        SearchResult<FloatVector, String> result1 = new SearchResult<>(item1, 0.75);
        SearchResult<FloatVector, String> result2 = new SearchResult<>(item2, 0.85);

        // Verify inequality
        assertNotEquals(result1, result2);
    }

    @Test
    void testSearchResultToString() {
        // Create a test item
        FloatVector vector = FloatVector.of(1.0f, 2.0f, 3.0f);
        Item<FloatVector, String> item = new TestItem(123L, List.of(vector), "test payload");

        // Create a search result
        SearchResult<FloatVector, String> result = new SearchResult<>(item, 0.75);

        // Verify toString contains important information
        String resultString = result.toString();
        assertTrue(resultString.contains("SearchResult"));
        assertTrue(resultString.contains("0.75"));
    }

    /** A simple implementation of the Item interface for testing purposes. */
    private static class TestItem implements Item<FloatVector, String> {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestItem testItem = (TestItem) o;
            return id == testItem.id
                    && Objects.equals(vectors, testItem.vectors)
                    && Objects.equals(payload, testItem.payload);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, vectors, payload);
        }
    }
}
