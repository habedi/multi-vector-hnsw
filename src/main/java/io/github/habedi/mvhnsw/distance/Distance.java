// src/main/java/io/github/habedi/mvhnsw/distance/Distance.java
package io.github.habedi.mvhnsw.distance;

public interface Distance<T> {

    double compute(T a, T b);

    default double computeSquared(T a, T b) {
        double dist = compute(a, b);
        return dist * dist;
    }

    String getName();
}
