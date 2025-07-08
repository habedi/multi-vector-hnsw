// src/main/java/io/github/habedi/mvhnsw/distance/WeightedAverageDistance.java
package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.index.Item;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class WeightedAverageDistance implements MultiVectorDistance<FloatVector>, Serializable {

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
        this.weights = weights;
    }

    @Override
    public double compute(Item<FloatVector, ?> item1, Item<FloatVector, ?> item2) {
        return compute(item1, item2.vectors());
    }

    @Override
    public double compute(Item<FloatVector, ?> item, List<FloatVector> queryVectors) {
        List<FloatVector> itemVectors = item.vectors();
        if (itemVectors.size() != queryVectors.size()) {
            throw new IllegalArgumentException("Item vector count must match query vector count.");
        }

        double totalDistance = 0.0;
        for (int i = 0; i < itemVectors.size(); i++) {
            totalDistance +=
                    weights[i] * distances.get(i).compute(itemVectors.get(i), queryVectors.get(i));
        }
        return totalDistance;
    }
}
