<div align="center">
  <picture>
    <img alt="Multi-Vector HNSW Logo" src="logo.svg" height="25%" width="25%">
  </picture>
<br>

<h2>Multi-Vector HNSW</h2>

[![Tests](https://img.shields.io/github/actions/workflow/status/habedi/multi-vector-hnsw/tests.yml?label=tests&style=flat&labelColor=282c34&logo=github)](https://github.com/habedi/multi-vector-hnsw/actions/workflows/tests.yml)
[![Lints](https://img.shields.io/github/actions/workflow/status/habedi/multi-vector-hnsw/lints.yml?label=lint&style=flat&labelColor=282c34&logo=github)](https://github.com/habedi/multi-vector-hnsw/actions/workflows/lints.yml)
[![Java](https://img.shields.io/badge/java-%3E=17-007ec6?style=flat&labelColor=282c34&logo=java)](https://openjdk.org)
[![Code Coverage](https://img.shields.io/codecov/c/github/habedi/multi-vector-hnsw?style=flat&labelColor=282c34&logo=codecov)](https://codecov.io/gh/habedi/multi-vector-hnsw)
[![Code Quality](https://img.shields.io/codefactor/grade/github/habedi/multi-vector-hnsw?style=flat&labelColor=282c34&logo=codefactor)](https://www.codefactor.io/repository/github/habedi/multi-vector-hnsw)
[![Docs](https://img.shields.io/badge/docs-latest-007ec6?style=flat&labelColor=282c34&logo=readthedocs)](docs)
[![License](https://img.shields.io/badge/license-MIT%2FApache--2.0-007ec6?style=flat&labelColor=282c34&logo=open-source-initiative)](https://github.com/habedi/multi-vector-hnsw)
[![Release](https://img.shields.io/github/release/habedi/multi-vector-hnsw.svg?style=flat&labelColor=282c34&logo=github)](https://github.com/habedi/multi-vector-hnsw/releases/latest)

A Java implementation of HNSW with multi-vector search support

</div>

---

Multi-Vector HNSW is a Java library that implements
the [Hierarchical Navigable Small World (HNSW)](https://arxiv.org/abs/1603.09320) algorithm with support for multi-vector
indexing.
It lets you index and search objects represented by multiple high-dimensional vectors using common distance functions like
Euclidean, cosine, and dot product.

In many real-world applications, a single vector isn’t enough to represent an object.
For example, an image might have different feature vectors for color, shape, and texture; a document might have separate
embeddings for different parts like the title, body, and summary.
Most HNSW implementations only support one vector per object.
This makes them less useful when working with complex objects that are better represented by multiple vectors.
Multi-Vector HNSW solves this by letting each object be indexed with multiple vectors.
It also lets you choose how to define the distances between objects using a custom aggregated distance function.
This can allow for more realistic and flexible searches when dealing with complex objects.

### Features

Of course. Here is the expanded feature list, keeping your items and adding more based on the library's capabilities.

***

### Features

* Simple, extendable API for multi-vector indexing and search
* Fast, configurable, thread-safe, HNSW implementation
* Built-in support for cosine, (squared) Euclidean, and dot product distances
* Bulk inserts and soft delete support
* Save and load functionality for persisting indexes to disk
* Fast distance calculations using SIMD instructions via Java Vector API
* Compatible with Java 17 and later

> [!IMPORTANT]
> This project is in its early stages of development, so breaking changes are expected to occur.

---

### Getting Started

If you are using Maven, add this dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>io.github.habedi</groupId>
    <artifactId>multi-vector-hnsw</artifactId>
    <version>0.2.0</version>
</dependency>
```

If you are using Gradle, add this dependency to your `build.gradle`:

```groovy
dependencies {
    implementation 'io.github.habedi:multi-vector-hnsw:0.2.0'
}
```

#### Simple Example

Below is a simple example of how to create an index, add a few items with multiple vectors, and perform a search.

```java
import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Cosine;
import io.github.habedi.mvhnsw.distance.SquaredEuclidean;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;
import java.util.List;

public class SimpleExample {
    public static void main(String[] args) {

        // 1. Configure the index for items with two vectors.
        // We'll weight the first vector (Euclidean distance) as 70% important
        // and the second vector (Cosine distance) as 30% important.
        Index index = MultiVectorHNSW.builder()
            .withM(16)
            .withEfConstruction(200)
            .withWeightedAverageDistance()
            .addDistance(new SquaredEuclidean(), 0.7f)
            .addDistance(new Cosine(), 0.3f)
            .and()
            .build();

        // 2. Add items to the index.
        index.add(1L, List.of(FloatVector.of(1.5f, 2.5f), FloatVector.of(0.9f, 0.1f)));
        index.add(2L, List.of(FloatVector.of(9.1f, 8.2f), FloatVector.of(0.2f, 0.8f)));
        index.add(3L, List.of(FloatVector.of(1.6f, 2.4f), FloatVector.of(0.8f, 0.3f)));

        // 3. Create a query and search for the top 2 nearest neighbors.
        List<FloatVector> query = List.of(FloatVector.of(1.4f, 2.6f), FloatVector.of(0.7f, 0.2f));
        List<SearchResult> results = index.search(query, 2);

        // 4. Print the results.
        System.out.println("Search results:");
        results.forEach(System.out::println);
    }
}
```

Output:

```shell
Search results:
SearchResult[id=3, score=0.06800000000000003]
SearchResult[id=1, score=0.08800000000000002]
```

---

### Adding New Distances

It's very easy to add new distances if you need to extend the library's functionality.
To do that you need to implement two interfaces:

1. [`Distance<FloatVector>`](src/main/java/io/github/habedi/mvhnsw/distance/Distance.java): Represents a distance between
   a pair of vectors (see [`Cosine`](src/main/java/io/github/habedi/mvhnsw/distance/Cosine.java) for an example).
2. [`MultiVectorDistance`](src/main/java/io/github/habedi/mvhnsw/distance/MultiVectorDistance.java): Represents the aggregated
   distance between two lists of vectors (see [
   `WeightedAverageDistance`](src/main/java/io/github/habedi/mvhnsw/distance/WeightedAverageDistance.java) for an example).

To add new functionality, you just need to create new classes that implement these interfaces.
The HNSW builder can accept any class that conforms to the
[`MultiVectorDistance`](src/main/java/io/github/habedi/mvhnsw/distance/MultiVectorDistance.java) interface.

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

---

### Documentation

To be added.

---

### Benchmarks

To can use `make bench-data` command to download the datasets used for the benchmarks.

Run `BENCHMARK_DATASET=<dataset_name> make bench-run` to run the benchmarks for a specified dataset.
At the moment, `<dataset_name>` can be one of `se_cs_768`, `se_ds_768`, or `se_pc_768`.
See the [benches/README.md](benches/README.md) file for more information about the benchmarks and datasets.

### Contributing

Contributions are welcome!
Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### Logo

Bird claw logo courtesy of [SVG Repo](https://www.svgrepo.com/svg/499015/bird-claw).

### License

This project is available under either of the following licenses:

* MIT License ([LICENSE-MIT](LICENSE-MIT))
* Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE))
