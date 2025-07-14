## Multi-Vector HNSW Documentation

### How Does It Work?

Multi-Vector HNSW extends the standard HNSW algorithm to support multiple vectors per indexed item.
Given that each item can have multiple vectors, the main idea is to define a way to compute the distance between two items based
on their vectors.
This is done using a custom distance function that aggregates the distances between corresponding vectors of a pair of items.

The HNSW algorithm is relatively flexible when it comes to distance functions.
The main requirement is that the function produces scalar values that are consistent with the similarity ordering of items.
That means, if item A is more similar to a query (item) than item B, then the computed distance from A to the query should be lower than the
distance from B.
The distance function does not need to be non-negative or a proper metric (for example, it can violate triangle inequality).
This flexibility makes it possible to plug in custom distance aggregation strategies (like max, min, weighted average, etc.) without
breaking the correctness of HNSW’s graph traversal and search logic.

Figure below shows a high-level overview of how the distance aggregation function works in Multi-Vector HNSW.

<div align="center">
  <picture>
<img src="assets/images/distance_aggregation.svg" alt="Distance Aggregation Function" width="auto" height="auto" align="center">
    </picture>
</div>

### Supported Distances

Here’s your **cleaned-up version** of the full section, with typos fixed and the updated table including formulas:

---

### Supported Distance Functions

At the moment, Multi-Vector HNSW supports the following distance functions out of the box:

| # | Distance Function                                                                            | Description                                                 | Formula (for vectors **A**, **B**)        |
|---|----------------------------------------------------------------------------------------------|-------------------------------------------------------------|-------------------------------------------|
| 1 | [Cosine](../src/main/java/io/github/habedi/mvhnsw/distance/Cosine.java)                      | Computes the `1 - cosine similarity` between two vectors    | $1 - \frac{A \cdot B}{\|A\| \cdot \|B\|}$ |
| 2 | [Squared Euclidean](../src/main/java/io/github/habedi/mvhnsw/distance/SquaredEuclidean.java) | Computes the squared Euclidean distance between two vectors | $\sum_i (A_i - B_i)^2$                    |
| 3 | [Dot Product](../src/main/java/io/github/habedi/mvhnsw/distance/DotProduct.java)             | Computes the `-1 * dot product` between two vectors         | $- (A \cdot B) = -\sum_i A_i B_i$         |

> [!NOTE]
> Squared Euclidean distance gives the same ordering as standard Euclidean distance, but it's faster to compute.
> If you specifically need the Euclidean distance, it's easy to implement, but in most cases, the squared version is a better choice.

### Adding New Distances

It's very easy and straightforward to add new distances if you need to extend the library's functionality.
To do that, you need to implement two interfaces:

1. [Distance<FloatVector>](../src/main/java/io/github/habedi/mvhnsw/distance/Distance.java): Represents a distance between
   a pair of vectors (see [Cosine.java](../src/main/java/io/github/habedi/mvhnsw/distance/Cosine.java) for an example).
2. [MultiVectorDistance](../src/main/java/io/github/habedi/mvhnsw/distance/MultiVectorDistance.java): Represents the aggregated
   distance between two lists of vectors
   (see [WeightedAverageDistance.java](../src/main/java/io/github/habedi/mvhnsw/distance/WeightedAverageDistance.java) for an example
   implementation).

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
        //    using Manhattan as its base distance
        var minManhattanDistance = new MinDistance(new Manhattan());

        // 2. Pass the custom distance function directly to the builder
        Index index = MultiVectorHNSW.builder()
            .withM(16)
            .withEfConstruction(200)
            .withDistance(minManhattanDistance) // Use the generic `withDistance` method
            .build();

        // 3. Add and search data as usual
        index.add(1L, List.of(FloatVector.of(1f, 2f), FloatVector.of(10f, 10f)));
        index.add(2L, List.of(FloatVector.of(8f, 8f), FloatVector.of(1f, 3f)));

        List<FloatVector> query = List.of(FloatVector.of(2f, 2f), FloatVector.of(9f, 9f));
        List<SearchResult> results = index.search(query, 1);

        // The distance for item 1 is min(|1-2|+|2-2|, |10-9|+|10-9|) = min(1, 2) = 1
        // The distance for item 2 is min(|8-2|+|8-2|, |1-9|+|3-9|) = min(12, 14) = 12
        // So, item 1 should be the closest
        System.out.println("Search results:");
        results.forEach(System.out::println);
    }
}
```
