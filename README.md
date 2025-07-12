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

Multi-Vector HNSW is a Java library that provides an implementation of
the [Hierarchical Navigable Small World (HNSW)](https://arxiv.org/abs/1603.09320) algorithm with support for indexing of
multi-vector data.
It allows users to index objects represented by multiple high-dimensional vectors and perform fast approximate nearest neighbor
searches using various distances like Euclidean, Cosine, and Dot Product.

### Why Multi-Vector HNSW?

In many real-world applications, objects can be represented by multiple vectors, such as images with multiple feature sets
or documents with multiple embeddings for different parts of the text.
A normal HNSW implementation typically supports indexing and searching for objects represented by a single vector.
This library aims to help users overcome this limitation by providing a flexible and efficient way to handle multi-vector data.

### Features

- **Multi-vector support**: Index and search for objects represented by multiple high-dimensional vectors.
- **Easy to use**: Simple API for indexing and searching.
- **Fast search**: Efficient approximate nearest neighbor search using the HNSW algorithm.
- **Customizable distance metrics**: Choose from various distance metrics including Euclidean, Cosine, and Dot Product.
- **Memory-efficient**: Designed to handle large datasets with minimal memory overhead.
- **Java-based**: Written in pure Java 17+, and easily integrable into applications running on the JVM.

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

### Examples

To be added.

---

### Documentation

To be added.

---

### Contributing

Contributions are welcome!
Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### Logo

Bird claw logo courtesy of [SVG Repo](https://www.svgrepo.com/svg/499015/bird-claw).

### License

This project is available under either of the following licenses:

* MIT License ([LICENSE-MIT](LICENSE-MIT))
* Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE))
