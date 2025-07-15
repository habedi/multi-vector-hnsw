<div align="center">
  <picture>
    <img alt="Multi-Vector HNSW Logo" src="logo.svg" height="25%" width="25%">
  </picture>
<br>

<h2>Multi-Vector HNSW</h2>

[![Tests](https://img.shields.io/github/actions/workflow/status/habedi/multi-vector-hnsw/tests.yml?label=tests&style=flat&labelColor=282c34&logo=github)](https://github.com/habedi/multi-vector-hnsw/actions/workflows/tests.yml)
[![Code Coverage](https://img.shields.io/codecov/c/github/habedi/multi-vector-hnsw?style=flat&labelColor=282c34&logo=codecov)](https://codecov.io/gh/habedi/multi-vector-hnsw)
[![Code Quality](https://img.shields.io/codefactor/grade/github/habedi/multi-vector-hnsw?style=flat&labelColor=282c34&logo=codefactor)](https://www.codefactor.io/repository/github/habedi/multi-vector-hnsw)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.habedi/multi-vector-hnsw?label=maven&style=flat&labelColor=282c34&logo=apache-maven)](https://central.sonatype.com/artifact/io.github.habedi/multi-vector-hnsw)
[![Docs](https://img.shields.io/badge/docs-latest-007ec6?style=flat&labelColor=282c34&logo=readthedocs)](docs)
[![License](https://img.shields.io/badge/license-MIT%2FApache--2.0-007ec6?style=flat&labelColor=282c34&logo=open-source-initiative)](https://github.com/habedi/multi-vector-hnsw)

A Java implementation of HNSW with multi-vector search support

</div>

---

Multi-Vector HNSW is a Java library that implements the [Hierarchical Navigable Small World (HNSW)](https://arxiv.org/abs/1603.09320)
algorithm with built-in support for multi-vector indexing.
It lets you index and search objects represented by multiple high-dimensional vectors, using standard distance functions like Euclidean,
cosine, and dot product.

Most vector search libraries assume every object has a single embedding.
But in real-world use cases (like document search, multi-modal AI, or hybrid dense/sparse setups) you often have multiple embeddings per
item.
This library extends HNSW to support that: multi-vector indexing, custom distance aggregation, and a clean Java API.

### Features

* Simple and extendable API for multi-vector indexing and search
* Fast, configurable, and thread-safe HNSW implementation
* Built-in support for cosine, (squared) Euclidean, and dot product distances
* Bulk inserts and soft delete support
* Save and load support for persisting indexes to disk
* Fast distance calculations using SIMD instructions via Java Vector API
* Pure Java 17 implementation with no native dependencies

---

### Getting Started

If you are using Maven, add this dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>io.github.habedi</groupId>
    <artifactId>multi-vector-hnsw</artifactId>
    <version>0.2.0-beta</version>
</dependency>
```

If you are using Gradle, add this dependency to your `build.gradle`:

```groovy
dependencies {
    implementation 'io.github.habedi:multi-vector-hnsw:0.2.0-beta'
}
```

Alternatively, you can download the latest JAR files directly from the
[releases](https://github.com/habedi/multi-vector-hnsw/releases) page and add it to your classpath.

#### Basic Usage

Below is an example of how to create an index, add items with multiple vectors, and perform a search.

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
        // We'll weight the first vector (squared Euclidean distance) as 70% important
        // and the second vector (Cosine distance) as 30% important.
        Index index = MultiVectorHNSW.builder()
            .withM(16)
            .withEfConstruction(200)
            .withWeightedAverageDistance()
            .addDistance(new SquaredEuclidean(), 0.7f)
            .addDistance(new Cosine(), 0.3f)
            .and()
            .build();

        // 2. Add items to the index
        index.add(1L, List.of(FloatVector.of(1.5f, 2.5f), FloatVector.of(0.9f, 0.1f)));
        index.add(2L, List.of(FloatVector.of(9.1f, 8.2f), FloatVector.of(0.2f, 0.8f)));
        index.add(3L, List.of(FloatVector.of(1.6f, 2.4f), FloatVector.of(0.8f, 0.3f)));

        // 3. Create a query and search for the top 2 nearest neighbors
        List<FloatVector> query = List.of(FloatVector.of(1.4f, 2.6f), FloatVector.of(0.7f, 0.2f));
        // The third parameter, `efSearch`, controls the accuracy/speed trade-off.
        List<SearchResult> results = index.search(query, 2, 20);

        // 4. Print the results
        System.out.println("Search results:");
        results.forEach(System.out::println);
    }
}
```

Output:

```shell
Search results:
SearchResult[id=1, score=0.018205724472503872] # Smaller score means closer match
SearchResult[id=3, score=0.05697077252055386]
```

---

### Documentation

Project documentation is available at the [docs](docs) directory.

---

### Examples

Check out the [examples](examples) directory for more usage examples.

### Benchmarks

See the [benches](benches) directory for information on how to run project benchmarks.

#### Sample Results

The table below shows benchmark results for different distance functions using the
[`se_cs_768`](https://huggingface.co/datasets/habedi/multi-vector-hnsw-datasets)
dataset on a machine with 32GB RAM and an AMD Ryzen 5 7600X CPU, running on GraalVM JDK 21.

Each item is represented by three 768-dimensional vectors. The index was built with `M=16` and `efConstruction=200`.
Searches were performed with `efSearch=100` to find the top 100 nearest neighbors.

For each distance function, we report:

* **Average Query Time:** The average time in milliseconds to perform a single search.
* **Recall@100:** How many of the top 100 true nearest neighbors were found, on average.

Distances are aggregated using a uniformly-weighted average across the three vectors.

In this setup, the average query time is **~1.17–1.37 ms**, with recall around **89%**.

| Distance Function | Train Size | Test Size | Avg Query Time (ms) | Recall@100 |
|:------------------|:-----------|:----------|:--------------------|:-----------|
| Squared Euclidean | 36,712     | 4,080     | 1.37                | 89.30%     |
| Cosine            | 36,712     | 4,080     | 1.19                | 89.54%     |
| Dot Product       | 36,712     | 4,080     | 1.17                | 89.19%     |

You can reproduce these results by running:

```bash
make bench-run BENCHMARK_DATASET=se_cs_768 ARGS="--ef-search=100"
```

---

### Contributing

Contributions are welcome!
Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### Logo

Talon logo is from [SVG Repo](https://www.svgrepo.com/svg/499015/bird-claw).

### License

This project is available under either of the following licenses:

* MIT License ([LICENSE-MIT](LICENSE-MIT))
* Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE))
