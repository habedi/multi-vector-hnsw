package io.github.habedi.mvhnsw.index;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SearchResultTest {

  @Test
  void testSearchResultValueAndEquality() {
    SearchResult result1 = new SearchResult(123L, 0.75);
    SearchResult result2 = new SearchResult(123L, 0.75);
    SearchResult result3 = new SearchResult(456L, 0.75);
    SearchResult result4 = new SearchResult(123L, 0.85);

    // Check values
    assertEquals(123L, result1.id());
    assertEquals(0.75, result1.score());

    // Check equals and hashCode contract
    assertEquals(result1, result2);
    assertEquals(result1.hashCode(), result2.hashCode());

    assertNotEquals(result1, result3);
    assertNotEquals(result1, result4);
    assertNotEquals(null, result1);
    assertNotEquals(new Object(), result1);
  }

  @Test
  void testSearchResultToString() {
    SearchResult result = new SearchResult(123L, 0.75);
    String resultString = result.toString();

    assertTrue(resultString.contains("id=123"));
    assertTrue(resultString.contains("score=0.75"));
  }
}
