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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 3, time = 5)
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

  private BenchmarkData loadedData;
  private Index index;
  private Map<Long, List<FloatVector>> preConvertedTestData;
  private Map<Long, List<FloatVector>> preConvertedTrainingData;
  private int numVectors;

  @State(Scope.Thread)
  @AuxCounters(AuxCounters.Type.EVENTS)
  public static class RecallCounters {
    public long hits;
    public long totalQueries;
  }

  @Setup(Level.Trial)
  public void setupTrial() throws IOException {
    loadedData = BenchmarkData.load(dataPath, datasetName, distanceMetric, K);

    if (!loadedData.trainingData().isEmpty()) {
      List<FloatVector> vectors = loadedData.trainingData().get(0).toFloatVectors();
      numVectors = vectors.size();
    }

    preConvertedTestData =
      loadedData.testData().stream()
        .collect(Collectors.toMap(TestItem::id, TestItem::toFloatVectors));
    preConvertedTrainingData =
      loadedData.trainingData().stream()
        .collect(Collectors.toMap(TestItem::id, TestItem::toFloatVectors));

    index = buildIndex();
  }

  @Benchmark
  public Index build() {
    return buildIndex();
  }

  @Benchmark
  public void search(Blackhole bh, RecallCounters counters) {
    for (Map.Entry<Long, List<FloatVector>> entry : preConvertedTestData.entrySet()) {
      List<SearchResult> results = index.search(entry.getValue(), K);
      updateRecall(entry.getKey(), results, loadedData.groundTruth(), counters);
      bh.consume(results);
    }
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

  private void updateRecall(
    long queryId,
    List<SearchResult> results,
    Map<Long, Set<Long>> groundTruth,
    RecallCounters counters) {
    Set<Long> truth = groundTruth.get(queryId);
    if (truth != null) {
      counters.hits += results.stream().filter(r -> truth.contains(r.id())).count();
      counters.totalQueries++;
    }
  }
}
