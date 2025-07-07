package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.Vector;
import java.io.Serial;
import java.io.Serializable;

/**
 * Calculates the Cosine distance between two vectors. The distance is defined as {@code 1.0 -
 * cosine_similarity}. The resulting distance is in the range [0, 2], where 0 means identical
 * direction and 2 means opposite directions.
 */
public class Cosine implements Distance<Vector<Float>>, Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * Computes the Cosine distance.
     *
     * @param a the first vector.
     * @param b the second vector.
     * @return the Cosine distance.
     * @throws IllegalArgumentException if vector lengths are not equal.
     */
    @Override
    public double compute(Vector<Float> a, Vector<Float> b) {
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
