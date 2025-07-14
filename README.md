<div align="center">
  <picture>
    <img alt="Multi-Vector HNSW Logo" src="logo.svg" height="25%" width="25%">
  </picture>
<br>

<h2>Multi-Vector HNSW</h2>

[![Tests](https://img.shields.io/github/actions/workflow/status/habedi/multi-vector-hnsw/tests.yml?label=tests&style=flat&labelColor=282c34&logo=github)](https://github.com/habedi/multi-vector-hnsw/actions/workflows/tests.yml)
[![Code Coverage](https://img.shields.io/codecov/c/github/habedi/multi-vector-hnsw?style=flat&labelColor=282c34&logo=codecov)](https://codecov.io/gh/habedi/multi-vector-hnsw)
[![Code Quality](https://img.shields.io/codefactor/grade/github/habedi/multi-vector-hnsw?style=flat&labelColor=282c34&logo=codefactor)](https://www.codefactor.io/repository/github/habedi/multi-vector-hnsw)
[![Java](https://img.shields.io/badge/java-%3E=17-007ec6?style=flat&labelColor=282c34&logo=java)](https://openjdk.org)
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

* Simple and extendable API for multi-vector indexing and search
* Low-latency, configurable, and thread-safe HNSW implementation
* Built-in support for cosine, (squared) Euclidean, and dot product distances
* Bulk inserts and soft delete support
* Save and load support for persisting indexes to disk
* Fast distance calculations using SIMD instructions via Java Vector API
* Compatible with Java 17 and later

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

Alternatively, you can manually download the latest JAR files directly from the
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

        // 2. Add items to the index
        index.add(1L, List.of(FloatVector.of(1.5f, 2.5f), FloatVector.of(0.9f, 0.1f)));
        index.add(2L, List.of(FloatVector.of(9.1f, 8.2f), FloatVector.of(0.2f, 0.8f)));
        index.add(3L, List.of(FloatVector.of(1.6f, 2.4f), FloatVector.of(0.8f, 0.3f)));

        // 3. Create a query and search for the top 2 nearest neighbors
        List<FloatVector> query = List.of(FloatVector.of(1.4f, 2.6f), FloatVector.of(0.7f, 0.2f));
        List<SearchResult> results = index.search(query, 2);

        // 4. Print the results
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

### Documentation

Project documentation is available at the [docs](docs) directory.

---

### Examples

Check out the [examples](src/main/java/io/github/habedi/mvhnsw/examples) directory for various use cases of the library.

| # | File                                                                                                | Description                                                                                        |
|---|-----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| 1 | [`E01_SimpleSearch.java`](src/main/java/io/github/habedi/mvhnsw/examples/E01_SimpleSearch.java)     | A very simple example                                                                              |
| 2 | [`E02_SaveAndLoad.java`](src/main/java/io/github/habedi/mvhnsw/examples/E02_SaveAndLoad.java)       | Demonstrates how to save and load an index                                                         |
| 3 | [`E03_BulkAndVacuum.java`](src/main/java/io/github/habedi/mvhnsw/examples/E03_BulkAndVacuum.java)   | Shows how to use bulk insert and delete APIs                                                       |
| 4 | [`E04_CustomDistance.java`](src/main/java/io/github/habedi/mvhnsw/examples/E04_CustomDistance.java) | Demonstrates how to define and use a custom distance function                                      |
| 5 | [`E05_HybridSearch.java`](src/main/java/io/github/habedi/mvhnsw/examples/E05_HybridSearch.java)     | Performs hybrid search using dense and sparse vectors with different weights                       |
| 6 | [`E06_DocumentSearch.java`](src/main/java/io/github/habedi/mvhnsw/examples/E06_DocumentSearch.java) | Indexes documents using separate embeddings for titles and bodies, with different weights for each |

### Benchmarks

To run benchmarks for Multi-Vector HNSW, you can use the provided [Makefile](Makefile).

Execute `BENCHMARK_DATASET=<dataset_name> make bench-run` to start the benchmarks for a specified dataset.
At the moment, `<dataset_name>` can be one of `se_cs_768`, `se_ds_768`, or `se_pc_768`.

Check out the [benches](benches) directory for more details.

> [!NOTE]
> You can use `make bench-data` to download the benchmark datasets automatically.
> However, you need to have [huggingface-cli](https://huggingface.co/docs/huggingface_hub/en/guides/cli) installed.
> You can set up a Python environment with `huggingface-cli` using the provided [pyproject.toml](pyproject.toml) file.

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
