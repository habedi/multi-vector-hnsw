package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.io.Serial;
import java.io.Serializable;

public class Cosine implements Distance<FloatVector>, Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @Override
    public double compute(FloatVector a, FloatVector b) {
        if (a.length() != b.length()) {
            throw new IllegalArgumentException("Vector lengths must be equal.");
        }
        double similarity = a.cosine(b);
        return 1.0 - similarity;
    }

    @Override
    public String getName() {
        return "Cosine";
    }
}
