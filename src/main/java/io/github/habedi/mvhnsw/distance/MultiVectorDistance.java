// src/main/java/io/github/habedi/mvhnsw/distance/MultiVectorDistance.java
package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.Vector;
import io.github.habedi.mvhnsw.index.Item;
import java.util.List;

public interface MultiVectorDistance<V extends Vector<?>> {

    double compute(Item<V, ?> item1, Item<V, ?> item2);

    double compute(Item<V, ?> item, List<V> queryVectors);
}
