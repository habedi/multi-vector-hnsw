// src/benchmark/java/io/github/habedi/mvhnsw/bench/IndexBenchmark.java
package io.github.habedi.mvhnsw.bench;

import io.github.habedi.mvhnsw.bench.data.BenchmarkData;
import io.github.habedi.mvhnsw.bench.data.TestItem;
import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Cosine;
import io.github.habedi.mvhnsw.distance.Distance;
import io.github.habedi.mvhnsw.distance.DotProduct;
import io.github.habedi.mvhnsw.distance.SquaredEuclidean;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Fork(value = 0)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 3, time = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class IndexBenchmark {

  private static final int K = 100;

  public record BenchmarkResult(
    double recall, int trainSize, int testSize, int numVectors, int vectorDim) {}

  public static final Map<String, BenchmarkResult> benchmarkData = new ConcurrentHashMap<>();

  private final LongAdder hits = new LongAdder();
  private final LongAdder totalQueries = new LongAdder();

  @Param({"se_cs_768"})
  public String datasetName;

  @Param({"squared_euclidean", "cosine", "dot_product"})
  public String distanceMetric;

  @Param({"16"})
  public int m;

  @Param({"200"})
  public int efConstruction;

  private BenchmarkData loadedData;
  private Index index;
  private int trainSize, testSize, numVectors, vectorDim;

  public static String getParamKey(String dataset, String metric, int m, int efc) {
    return String.format("%s|%s|%d|%d", dataset, metric, m, efc);
  }

  @Setup(Level.Trial)
  public void setupTrial() throws IOException {
    loadedData = BenchmarkData.load(datasetName, distanceMetric, K);
    trainSize = loadedData.trainingData().size();
    testSize = loadedData.testData().size();

    if (!loadedData.trainingData().isEmpty()) {
      List<FloatVector> vectors = loadedData.trainingData().get(0).toFloatVectors();
      numVectors = vectors.size();
      if (!vectors.isEmpty()) {
        vectorDim = vectors.get(0).length();
      }
    }

    index = buildIndex();
    hits.reset();
    totalQueries.reset();
  }

  @TearDown(Level.Trial)
  public void tearDownTrial() {
    long totalHits = hits.sum();
    long totalQueriesCount = totalQueries.sum();
    double recall =
      (totalQueriesCount == 0) ? 0 : (double) totalHits / (totalQueriesCount * K);
    String key = getParamKey(datasetName, distanceMetric, m, efConstruction);
    benchmarkData.put(
      key, new BenchmarkResult(recall, trainSize, testSize, numVectors, vectorDim));
  }

  @Benchmark
  public Index build() {
    return buildIndex();
  }

  @Benchmark
  public void search(Blackhole bh) {
    for (TestItem testItem : loadedData.testData()) {
      List<SearchResult> results = index.search(testItem.toFloatVectors(), K);
      updateRecall(testItem.id(), results, loadedData.groundTruth());
      bh.consume(results);
    }
  }

  private Index buildIndex() {
    List<FloatVector> vectors = loadedData.trainingData().get(0).toFloatVectors();
    int numVectors = vectors.size();
    float weight = 1.0f / numVectors;

    MultiVectorHNSW.Builder.WeightedAverageDistanceBuilder distanceBuilder =
      MultiVectorHNSW.builder()
        .withM(m)
        .withEfConstruction(efConstruction)
        .withWeightedAverageDistance();

    Distance<FloatVector> distance = createDistance();
    for (int i = 0; i < numVectors; i++) {
      distanceBuilder.addDistance(distance, weight);
    }

    Index newIndex = distanceBuilder.and().build();
    for (TestItem item : loadedData.trainingData()) {
      newIndex.add(item.id(), item.toFloatVectors());
    }
    return newIndex;
  }

  private Distance<FloatVector> createDistance() {
    return switch (distanceMetric.toLowerCase()) {
      case "squared_euclidean" -> new SquaredEuclidean();
      case "cosine" -> new Cosine();
      case "dot_product" -> new DotProduct();
      default -> throw new IllegalArgumentException("Unknown distance metric: " + distanceMetric);
    };
  }

  private void updateRecall(
    long queryId, List<SearchResult> results, Map<Long, Set<Long>> groundTruth) {
    Set<Long> truth = groundTruth.get(queryId);
    if (truth != null) {
      long currentHits = results.stream().filter(r -> truth.contains(r.id())).count();
      hits.add(currentHits);
      totalQueries.increment();
    }
  }
}
