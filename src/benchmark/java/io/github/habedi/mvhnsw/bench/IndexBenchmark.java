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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.BenchmarkParams;

@State(Scope.Benchmark)
@Fork(value = 1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class IndexBenchmark {

  private static final int K = 100;

  @Param({"se_p_768"})
  public String datasetName;

  @Param("benches/multi-vector-hnsw-datasets")
  public String dataPath;

  @Param({"squared_euclidean", "cosine", "dot_product"})
  public String distanceMetric;

  @Param({"16"})
  public int m;

  @Param({"200"})
  public int efConstruction;

  @Param({"100"})
  public int efSearch;

  private BenchmarkData loadedData;
  private Index index;
  private Map<Long, List<FloatVector>> preConvertedTrainingData;
  private List<Long> queryIds;
  private List<List<FloatVector>> queries;
  private int numVectors;
  private long recallHits;
  private long recallQueries;
  private int nextQuery;

  @Setup(Level.Trial)
  public void setupTrial(BenchmarkParams params) throws IOException {
    if (efSearch < K) {
      throw new IllegalArgumentException("efSearch must be >= K for benchmark to be valid.");
    }

    loadedData = BenchmarkData.load(dataPath, datasetName, distanceMetric, K);

    if (!loadedData.trainingData().isEmpty()) {
      List<FloatVector> vectors = loadedData.trainingData().get(0).toFloatVectors();
      numVectors = vectors.size();
    }

    preConvertedTrainingData =
      loadedData.trainingData().stream()
        .collect(Collectors.toMap(TestItem::id, TestItem::toFloatVectors));

    queryIds = new ArrayList<>(loadedData.testData().size());
    queries = new ArrayList<>(loadedData.testData().size());
    for (TestItem item : loadedData.testData()) {
      queryIds.add(item.id());
      queries.add(item.toFloatVectors());
    }

    // The build benchmark constructs its own index inside the timed method, so the shared index
    // and the recall sweep are only needed for the search benchmark.
    if (params.getBenchmark().endsWith(".search")) {
      index = buildIndex();
      computeRecall();
    }
  }

  // Recall is deterministic for a built index and a fixed query set, so it is measured once here
  // instead of inside the timed search loop. The benchmark method only copies the totals into the
  // aux counters so they reach the parent process through the JMH results.
  private void computeRecall() {
    for (int i = 0; i < queries.size(); i++) {
      Set<Long> truth = loadedData.groundTruth().get(queryIds.get(i));
      if (truth == null) {
        continue;
      }
      List<SearchResult> results = index.search(queries.get(i), K, efSearch);
      recallHits += results.stream().filter(r -> truth.contains(r.id())).count();
      recallQueries++;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.SingleShotTime)
  @Warmup(iterations = 1)
  @Measurement(iterations = 3)
  public Index build() {
    return buildIndex();
  }

  @Benchmark
  @BenchmarkMode(Mode.Throughput)
  @Fork(2)
  @Warmup(iterations = 3, time = 5)
  @Measurement(iterations = 5, time = 5)
  public void search(Blackhole bh, RecallCounters counters) {
    List<SearchResult> results = index.search(queries.get(nextQuery), K, efSearch);
    nextQuery++;
    if (nextQuery == queries.size()) {
      nextQuery = 0;
    }
    counters.hits = recallHits;
    counters.totalQueries = recallQueries;
    bh.consume(results);
  }

  private Index buildIndex() {
    MultiVectorHNSW.Builder.WeightedAverageDistanceBuilder distanceBuilder =
      MultiVectorHNSW.builder()
        .withM(m)
        .withEfConstruction(efConstruction)
        .withWeightedAverageDistance();

    Distance<FloatVector> distance = createDistance();
    for (int i = 0; i < numVectors; i++) {
      distanceBuilder.addDistance(distance, 1.0f / numVectors);
    }

    Index newIndex = distanceBuilder.and().build();
    newIndex.addAll(preConvertedTrainingData);
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

  @State(Scope.Thread)
  @AuxCounters(AuxCounters.Type.EVENTS)
  public static class RecallCounters {
    public long hits;
    public long totalQueries;
  }
}
