# AGENTS.md

This file provides guidance to coding agents collaborating on this repository.

## Mission

Multi-Vector HNSW is a Java library that implements the Hierarchical Navigable
Small World (HNSW) algorithm with support for multi-vector indexing and search.
It lets callers index and search items that are each represented by a list of
high-dimensional float vectors, using distance functions such as squared
Euclidean, cosine, and dot product.

Priorities, in order:

1. Correct, well-tested implementations of the HNSW algorithm and its supporting data structures.
2. Clean, idiomatic Java with a safe, high-level API.
3. Thread safety across all public operations.
4. Maintainable code with consistent error handling and documentation.

## Core Rules

- Use English for code, comments, docs, and tests.
- Never use bare `null` returns or unchecked casts in non-test code without a
  comment explaining why. Prefer `Optional` for nullable return values.
- Never use `assert` for production flow control. Use explicit null checks,
  `Objects.requireNonNull()`, or throw `IllegalArgumentException` where
  appropriate.
- Validate all public method arguments eagerly and throw
  `IllegalArgumentException` or `NullPointerException` at the call site, not
  deep inside a helper.
- Prefer small, focused changes over large refactoring.
- Add comments only when they clarify non-obvious behavior.
- Do not add features, error handling, or abstractions beyond what is needed
  for the current task.
- Add tests for every bug fix and new feature to prevent regression.
- Follow red-green TDD: write a failing test first, then the code to pass it
  (see Test-Driven Development).

Quick examples:

- Good: add a `computeSquared` override in a new `Euclidean` class that avoids
  the square root, with unit tests for the identical-vector and
  mismatched-length cases.
- Good: add a regression test in `MultiVectorHNSWTest` that serializes the
  index, deserializes it, and confirms search results are unchanged.
- Bad: call `candidates.peek().id` immediately after an `assert candidates.peek() != null`
  statement. The `assert` is disabled in production JVMs; use an explicit null
  check or `Objects.requireNonNull()` instead.
- Bad: add new public API without a corresponding test. No public API change is
  complete without a test.

## Writing Style

- Use Oxford commas in inline lists: "a, b, and c" not "a, b, c".
- Do not use em dashes. Restructure the sentence, or use a colon or semicolon instead.
- Avoid colorful adjectives and adverbs. Write "distance function" not
  "powerful distance function".
- Prefer using noun phrases for checklist items, not imperative verbs. Write
  "input validation" not "validate inputs".
- Headings in Markdown files must be in title case: "Build from Source" not
  "Build from source". Minor words (a, an, the, and, but, or, for, in, on, at,
  to, by, of) stay lowercase unless they are the first word.
- Write correct and complete sentences.
- Avoid made-up words, abbreviations, and colons in the middle of sentences.
- Do not use pretentious language and made-up words.

## Repository Layout

- `src/main/java/io/github/habedi/mvhnsw/common/`: Core data types. `Vector`
  is the generic interface; `FloatVector` is the concrete, SIMD-accelerated
  implementation.
- `src/main/java/io/github/habedi/mvhnsw/distance/`: Distance functions.
  `Distance<T>` is the single-vector interface; `MultiVectorDistance` is the
  aggregation strategy interface. Implementations: `Cosine`, `DotProduct`, and
  `SquaredEuclidean`. `WeightedAverageDistance` is the built-in aggregator.
- `src/main/java/io/github/habedi/mvhnsw/index/`: The index. `Index` is the
  public interface; `MultiVectorHNSW` is the implementation. `SearchResult`
  carries search output.
- `src/main/java/module-info.java`: Java module descriptor.
- `src/test/java/`: Unit tests, organized to mirror the main source tree.
- `src/benchmark/java/`: JMH benchmarks and benchmark data utilities.
- `benches/`: Benchmark datasets and benchmark documentation.
- `docs/`: Project documentation.
- `examples/`: Usage examples.
- `Makefile`: GNU Make wrapper around Maven commands.
- `pom.xml`: Maven build descriptor.

## Architecture

The library has two layers.

- **Data and distance layer.** `FloatVector` holds a float array and exposes
  vectorized dot product and norm operations via the Java Vector API
  (`jdk.incubator.vector`). `Distance<FloatVector>` implementations compute a
  scalar score between two vectors. `MultiVectorDistance` implementations
  aggregate a list of per-vector scores into a single distance for a
  multi-vector item.
- **Index layer.** `MultiVectorHNSW` maintains an HNSW graph (`Node` objects
  with per-level connection lists) and a separate `vectorMap` that stores the
  raw vectors. Soft deletion marks nodes as deleted without rebuilding the
  graph; `vacuum()` triggers a full rebuild to permanently remove them.

### Key Design Decisions

- Thread safety: all public write operations (`add`, `remove`, `addAll`,
  `clear`, `vacuum`) hold the write lock of a `ReentrantReadWriteLock`. All
  read operations (`search`, `get`, `size`, `keySet`, `save`) hold the read
  lock.
- Soft deletion: `remove(id)` sets `node.deleted = true`. Deleted nodes are
  excluded from search results but remain in the graph structure until
  `vacuum()` is called.
- Builder pattern: `MultiVectorHNSW` is constructed through a fluent
  `Builder` that validates all parameters before building. The nested
  `WeightedAverageDistanceBuilder` provides a guided path for the common
  weighted-average distance configuration.
- SIMD acceleration: `FloatVector.dot()` and `FloatVector.norm()` use
  `jdk.incubator.vector` with a scalar remainder loop for arrays that are not
  a multiple of the SIMD lane width.
- Serialization: `MultiVectorHNSW` is `Serializable`. The `lock` field is
  `transient` and is re-initialized in `readObject`. The `norm` field in
  `FloatVector` is also `transient` and must be handled correctly on
  deserialization.

### Public Contract

The public API is the `Index` interface, the `SearchResult` record, the
`FloatVector` class, and the interfaces in the `distance` package. Internal
types (`Node`, `Neighbor`) are private nested classes. Do not expose them.

## Required Validation

Run `make format-check`, `make lint`, and `make test` before any commit. Key
targets:

| Target          | Command              | What It Runs                                      |
|-----------------|----------------------|---------------------------------------------------|
| Build and test  | `make build`         | `mvn verify` (compile, checkstyle, and all tests) |
| Package         | `make package`       | `mvn package`                                     |
| Test            | `make test`          | `mvn verify` with configurable log level          |
| Format          | `make format`        | `mvn spotless:apply` (Google Java Style)          |
| Format Check    | `make format-check`  | `mvn spotless:check` (non-mutating)               |
| Lint            | `make lint`          | `mvn checkstyle:check`                            |
| Clean           | `make clean`         | `mvn clean`                                       |
| Benchmark Data  | `make bench-data`    | Downloads datasets for benchmarks                 |
| Benchmark JAR   | `make bench-jar`     | Builds the JMH benchmark JAR                      |
| Benchmark Run   | `make bench-run`     | Runs benchmarks (requires `bench-data` first)     |

## Test-Driven Development

Develop with the red-green-refactor cycle. Write the test before the
implementation.

1. Red: write a test that captures the desired behavior, then run it
   (`make test`) and confirm it fails for the expected reason. A test that
   passes before any code is written is not exercising the new behavior.
2. Green: write the smallest amount of code that makes the test pass. Do not
   add behavior the failing test does not require.
3. Refactor: clean up the implementation and tests while keeping them green,
   then rerun `make lint` and `make test`.

Guidelines:

- One logical behavior per cycle. Add edge cases (empty index, single item,
  deleted entry point, mismatched vector lengths) as separate red-green steps
  rather than in a single large test.
- For bug fixes, write the regression test first as the red step: it must fail
  on the current code and pass after the fix.

## Testing Expectations

- Tests live in `src/test/java/` and mirror the main source package structure.
- Every public method must have at least one test covering the happy path and
  one test covering relevant error paths.
- Concurrency tests should use `CountDownLatch` to synchronize threads and
  verify that no exceptions are thrown and that the final state is consistent.
- Serialization round-trips should be tested for all `Serializable` classes,
  including correctness of `transient` fields after deserialization.
- No public API change is complete without a corresponding test.

## Commit and PR Hygiene

- Keep commits scoped to one logical change.
- PR descriptions should include:
    1. Behavioral change summary.
    2. Tests added or updated.
    3. `make lint && make test` passes (yes/no).

Suggested PR checklist:

- [ ] Unit tests added or updated for logic changes
- [ ] Regression test added for bug fixes
- [ ] `make format-check` passes
- [ ] `make lint` passes
- [ ] `make test` passes
- [ ] Docs or README updated if the public API changed

## Review Guidelines

Review output should be concise and cover only critical issues. Do not include
style-only feedback or broad praise.

- `P0`: must-fix defects (incorrect search result, a production `NullPointerException`
  or `AssertionError` path, a broken build, or a broken test).
- `P1`: high-priority defects (missing argument validation in a public method,
  a `transient` field not handled in `readObject`, a concurrency hazard, or a
  numerical change with no regression test).

Use this review format:

1. `Severity` (`P0`/`P1`)
2. `File:line`
3. `Issue`
4. `Why it matters`
5. `Minimal fix direction`
