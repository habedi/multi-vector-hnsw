package io.github.habedi.mvhnsw.index;

import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.SquaredEuclidean;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MultiVectorHNSWMemoryTest {

  @Test
  void shouldLeakVectorMapDataOnRemove() throws NoSuchFieldException, IllegalAccessException {
    // given
    MultiVectorHNSW index =
        MultiVectorHNSW.builder()
            .withM(8)
            .withEfConstruction(100)
            .withWeightedAverageDistance()
            .addDistance(new SquaredEuclidean(), 1.0f)
            .and()
            .build();

    index.addAll(
        Map.of(
            1L, List.of(new FloatVector(new float[] {1.0f, 2.0f})),
            2L, List.of(new FloatVector(new float[] {3.0f, 4.0f}))));

    // when
    index.remove(1L);

    // then
    Field vectorMapField = MultiVectorHNSW.class.getDeclaredField("vectorMap");
    vectorMapField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Long, List<FloatVector>> vectorMap =
        (Map<Long, List<FloatVector>>) vectorMapField.get(index);

    // This assertion verifies the fix: the vector data for the removed item is now gone.
    assertFalse(
        vectorMap.containsKey(1L), "Vector map should not contain the removed key after the fix.");
  }
}
