package io.github.habedi.mvhnsw.index;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SearchResultTest {

    @Test
    void testSearchResultCreation() {
        long id = 123L;
        double score = 0.75;
        SearchResult result = new SearchResult(id, score);

        assertEquals(id, result.id());
        assertEquals(score, result.score());
    }

    @Test
    void testSearchResultEquality() {
        SearchResult result1 = new SearchResult(123L, 0.75);
        SearchResult result2 = new SearchResult(123L, 0.75);

        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testSearchResultInequality() {
        SearchResult result1 = new SearchResult(123L, 0.75);
        SearchResult result2 = new SearchResult(456L, 0.85);

        assertNotEquals(result1, result2);
    }

    @Test
    void testSearchResultToString() {
        SearchResult result = new SearchResult(123L, 0.75);
        String resultString = result.toString();

        assertTrue(resultString.contains("SearchResult"));
        assertTrue(resultString.contains("id=123"));
        assertTrue(resultString.contains("score=0.75"));
    }
}
