// src/main/java/io/github/habedi/mvhnsw/distance/Euclidean.java
package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.io.Serializable;

public class Euclidean implements Distance<FloatVector>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public double compute(FloatVector a, FloatVector b) {
        return Math.sqrt(computeSquared(a, b));
    }

    @Override
    public double computeSquared(FloatVector a, FloatVector b) {
        if (a.length() != b.length()) {
            throw new IllegalArgumentException("Vector lengths must be equal.");
        }

        double sumSq = 0.0;
        for (int i = 0; i < a.length(); i++) {
            double diff = a.getPrimitive(i) - b.getPrimitive(i);
            sumSq += diff * diff;
        }
        return sumSq;
    }

    @Override
    public String getName() {
        return "Euclidean";
    }
}
