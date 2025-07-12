package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class WeightedAverageDistance implements MultiVectorDistance, Serializable {

    @Serial private static final long serialVersionUID = 1L;
    private final List<Distance<FloatVector>> distances;
    private final float[] weights;

    public WeightedAverageDistance(List<Distance<FloatVector>> distances, float[] weights) {
        Objects.requireNonNull(distances, "Distances list cannot be null.");
        Objects.requireNonNull(weights, "Weights array cannot be null.");
        if (distances.isEmpty()) {
            throw new IllegalArgumentException("Distances list cannot be empty.");
        }
        if (distances.size() != weights.length) {
            throw new IllegalArgumentException(
                    "The number of distances must match the number of weights.");
        }
        this.distances = distances;
        this.weights = normalize(weights);
    }

    private float[] normalize(float[] w) {
        double sum = 0.0;
        for (float weight : w) {
            if (weight < 0) {
                throw new IllegalArgumentException("Weights must be non-negative.");
            }
            sum += weight;
        }

        if (sum == 0.0) {
            throw new IllegalArgumentException("Sum of weights cannot be zero.");
        }

        float[] normalized = new float[w.length];
        for (int i = 0; i < w.length; i++) {
            normalized[i] = (float) (w[i] / sum);
        }
        return normalized;
    }

    @Override
    public double compute(List<FloatVector> vectors1, List<FloatVector> vectors2) {
        if (vectors1.size() != vectors2.size()) {
            throw new IllegalArgumentException("Vector list sizes must match.");
        }
        if (vectors1.size() != distances.size()) {
            throw new IllegalArgumentException(
                    "Number of vectors must match the number of distance functions.");
        }

        double totalDistance = 0.0;
        for (int i = 0; i < vectors1.size(); i++) {
            totalDistance +=
                    weights[i] * distances.get(i).compute(vectors1.get(i), vectors2.get(i));
        }
        return totalDistance;
    }
}
