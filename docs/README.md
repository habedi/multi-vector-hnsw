## Multi-Vector HNSW Documentation

### How Does It Work?

Multi-Vector HNSW extends the normal HNSW algorithm to support multiple vectors per indexed item.
Given that each item can have multiple vectors, the main idea is to define a way to compute the distance between two items based
on their vectors.
This is done using a custom distance function that aggregates the distances between corresponding vectors of a pair of items.

### Adding New Distances

It's very easy to add new distances if you need to extend the library's functionality.
To do that you need to implement two interfaces:

1. [Distance<FloatVector>](../src/main/java/io/github/habedi/mvhnsw/distance/Distance.java): Represents a distance between
   a pair of vectors (see [Cosine.java](../src/main/java/io/github/habedi/mvhnsw/distance/Cosine.java) for an example).
2. [MultiVectorDistance](../src/main/java/io/github/habedi/mvhnsw/distance/MultiVectorDistance.java): Represents the aggregated
   distance between two lists of vectors (
   see [WeightedAverageDistance.java](../src/main/java/io/github/habedi/mvhnsw/distance/WeightedAverageDistance.java) for an example).

To add new functionality, you just need to create new classes that implement these interfaces.
The HNSW builder can accept any class that conforms to
the [MultiVectorDistance](../src/main/java/io/github/habedi/mvhnsw/distance/MultiVectorDistance.java) interface.

#### Example: Adding Manhattan and Min-Distance

Here is a complete, runnable example that demonstrates how to add a new `Manhattan` distance and a new `MinDistance` aggregation
strategy.

##### 1\. Implement `Manhattan` Distance

Create a `Manhattan.java` class that implements the `Distance<FloatVector>` interface.
This calculates the sum of absolute differences between vector components.

```java
package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;

import java.io.Serial;
import java.io.Serializable;

public class Manhattan implements Distance<FloatVector>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public double compute(FloatVector a, FloatVector b) {
        if (a.length() != b.length()) {
            throw new IllegalArgumentException("Vector lengths must be equal.");
        }
        double sum = 0.0;
        for (int i = 0; i < a.length(); i++) {
            sum += Math.abs(a.getPrimitive(i) - b.getPrimitive(i));
        }
        return sum;
    }

    @Override
    public String getName() {
        return "Manhattan";
    }
}
```

##### 2\. Implement `MinDistance` (Aggregated) Distance

Create a `MinDistance.java` class that implements the `MultiVectorDistance` interface.
Instead of a weighted average, this class will find the *minimum* distance among all vector pairs, using a base distance function
like `Manhattan`.

```java
package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class MinDistance implements MultiVectorDistance, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final Distance<FloatVector> baseDistance;

    public MinDistance(Distance<FloatVector> baseDistance) {
        this.baseDistance = Objects.requireNonNull(baseDistance, "Base distance cannot be null.");
    }

    @Override
    public double compute(List<FloatVector> vectors1, List<FloatVector> vectors2) {
        if (vectors1.size() != vectors2.size() || vectors1.isEmpty()) {
            throw new IllegalArgumentException("Vector lists must be non-empty and of equal size.");
        }
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < vectors1.size(); i++) {
            double dist = baseDistance.compute(vectors1.get(i), vectors2.get(i));
            if (dist < minDistance) {
                minDistance = dist;
            }
        }
        return minDistance;
    }
}
```

##### 3\. Put Everything Together

Now you can use these new, custom classes to build an index and perform searches with the new distance functions.

```java
import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;

// Import the custom distance classes (from wherever you placed them)
import some.package.Manhattan; // Replace `some.package` with your actual package name
import some.package.MinDistance;

import java.util.List;

public class ExtensibilityExample {
    public static void main(String[] args) {

        // 1. Create an instance of our new "MinDistance" aggregation,
        //    using Manhattan as its base distance.
        var minManhattanDistance = new MinDistance(new Manhattan());

        // 2. Pass the custom distance function directly to the builder.
        Index index = MultiVectorHNSW.builder()
            .withM(16)
            .withEfConstruction(200)
            .withDistance(minManhattanDistance) // Use the generic `withDistance` method
            .build();

        // 3. Add and search data as usual.
        index.add(1L, List.of(FloatVector.of(1f, 2f), FloatVector.of(10f, 10f)));
        index.add(2L, List.of(FloatVector.of(8f, 8f), FloatVector.of(1f, 3f)));

        List<FloatVector> query = List.of(FloatVector.of(2f, 2f), FloatVector.of(9f, 9f));
        List<SearchResult> results = index.search(query, 1);

        // The distance for item 1 is min(|1-2|+|2-2|, |10-9|+|10-9|) = min(1, 2) = 1
        // The distance for item 2 is min(|8-2|+|8-2|, |1-9|+|3-9|) = min(12, 14) = 12
        // So, item 1 should be the closest.
        System.out.println("Search results:");
        results.forEach(System.out::println);
    }
}
```
