package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.io.Serial;
import java.io.Serializable;

public class DotProduct implements Distance<FloatVector>, Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @Override
    public double compute(FloatVector a, FloatVector b) {
        if (a.length() != b.length()) {
            throw new IllegalArgumentException("Vector lengths must be equal.");
        }
        return -a.dot(b);
    }

    @Override
    public String getName() {
        return "DotProduct";
    }
}
