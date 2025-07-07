// src/main/java/io/github/habedi/mvhnsw/index/Item.java
package io.github.habedi.mvhnsw.index;

import io.github.habedi.mvhnsw.common.Vector;
import java.util.List;

public interface Item<V extends Vector<?>, P> {

    long getId();

    List<V> getVectors();

    P getPayload();
}
