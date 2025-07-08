package io.github.habedi.mvhnsw.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItemTest {

    @Test
    void testItemCreation() {
        // Create test vectors
        FloatVector vector1 = FloatVector.of(1.0f, 2.0f, 3.0f);
        FloatVector vector2 = FloatVector.of(4.0f, 5.0f, 6.0f);
        List<FloatVector> vectors = Arrays.asList(vector1, vector2);

        // Create test item
        long id = 123L;
        String payload = "test payload";
        TestItem item = new TestItem(id, vectors, payload);

        // Verify the item's properties
        assertEquals(id, item.id());
        assertEquals(vectors, item.vectors());
        assertEquals(payload, item.payload());
    }

    @Test
    void testItemWithMultipleVectors() {
        // Create test vectors
        FloatVector vector1 = FloatVector.of(1.0f, 2.0f, 3.0f);
        FloatVector vector2 = FloatVector.of(4.0f, 5.0f, 6.0f);
        FloatVector vector3 = FloatVector.of(7.0f, 8.0f, 9.0f);
        List<FloatVector> vectors = Arrays.asList(vector1, vector2, vector3);

        // Create test item
        TestItem item = new TestItem(456L, vectors, "multiple vectors");

        // Verify the vectors
        assertEquals(3, item.vectors().size());
        assertEquals(vector1, item.vectors().get(0));
        assertEquals(vector2, item.vectors().get(1));
        assertEquals(vector3, item.vectors().get(2));
    }

    @Test
    void testItemWithNullPayload() {
        // Create test item with null payload
        FloatVector vector = FloatVector.of(1.0f, 2.0f, 3.0f);
        TestItem item = new TestItem(789L, List.of(vector), null);

        // Verify the payload is null
        assertNull(item.payload());
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
        public long id() {
            return id;
        }

        @Override
        public List<FloatVector> vectors() {
            return vectors;
        }

        @Override
        public String payload() {
            return payload;
        }
    }
}
