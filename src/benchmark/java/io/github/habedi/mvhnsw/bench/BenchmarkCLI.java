package io.github.habedi.mvhnsw.bench;

import io.github.habedi.mvhnsw.bench.data.BenchmarkData;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  name = "benchmark",
  mixinStandardHelpOptions = true,
  description = "Runs the benchmark for Multi-Vector HNSW.")
public class BenchmarkCLI implements Callable<Integer> {

  private static final Logger log = LogManager.getLogger(BenchmarkCLI.class);

  private static final int K = 100;

  private static final List<String> METRIC_ORDER =
    List.of("squared_euclidean", "cosine", "dot_product");

  @Option(
    names = {"-d", "--dataset"},
    description = "The name of the dataset to use for the benchmark.",
    defaultValue = "se_p_768")
  private String datasetName;

  @Option(
    names = {"--data-path"},
    description = "The base path to the benchmark data directory.",
    defaultValue = "benches/multi-vector-hnsw-datasets")
  private String dataPath;

  @Option(names = {"-m"}, description = "The M parameter for HNSW.", defaultValue = "16")
  private int m;

  @Option(
    names = {"-efc", "--ef-construction"},
    description = "The efConstruction parameter for HNSW.",
    defaultValue = "200")
  private int efConstruction;

  @Option(
    names = {"-efs", "--ef-search"},
    description =
      "The efSearch parameter for HNSW. Accepts a comma-separated list of values to sweep.",
    defaultValue = "100",
    split = ",")
  private int[] efSearch;

  @Option(
    names = {"-p", "--profiler"},
    description = "Enable a JMH profiler (e.g., 'stack', 'jfr').")
  private String profiler;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new BenchmarkCLI()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws RunnerException, IOException {
    log.info(
      "Starting benchmark with dataset={}, m={}, efConstruction={}, efSearch={}",
      datasetName,
      m,
      efConstruction,
      Arrays.toString(efSearch));

    String[] efSearchValues =
      Arrays.stream(efSearch).mapToObj(String::valueOf).toArray(String[]::new);

    ChainedOptionsBuilder builder =
      new OptionsBuilder()
        .include(IndexBenchmark.class.getSimpleName())
        .param("datasetName", datasetName)
        .param("dataPath", dataPath)
        .param("m", String.valueOf(m))
        .param("efConstruction", String.valueOf(efConstruction))
        .param("efSearch", efSearchValues);

    if (profiler != null && !profiler.isBlank()) {
      log.info("Enabling JMH profiler: {}", profiler);
      builder.addProfiler(profiler);
    }

    Options opt = builder.build();
    Collection<RunResult> results = new Runner(opt).run();
    printSummaryTable(results);

    return 0;
  }

  private static String paramKey(RunResult result) {
    var params = result.getParams();
    return params.getParam("distanceMetric") + "|" + params.getParam("efSearch");
  }

  private void printSummaryTable(Collection<RunResult> results) throws IOException {
    System.out.println("\n\n--- HNSW Benchmark Summary ---");
    String header =
      String.format(
        "%-20s | %-8s | %-8s | %-8s | %-5s | %-8s | %-8s | %-18s | %-20s | %-12s",
        "Distance",
        "Train",
        "Test",
        "Dims",
        "M",
        "efConst",
        "efSearch",
        "Build Time (s)",
        "Avg Time/Query (ms)",
        "Recall@" + K);
    System.out.println(header);
    System.out.println(new String(new char[header.length()]).replace("\0", "-"));

    Map<String, RunResult> buildResultsByKey =
      results.stream()
        .filter(r -> r.getPrimaryResult().getLabel().equals("build"))
        .collect(Collectors.toMap(BenchmarkCLI::paramKey, r -> r, (a, b) -> a));

    Comparator<RunResult> byMetric =
      Comparator.comparingInt(r -> METRIC_ORDER.indexOf(r.getParams().getParam("distanceMetric")));
    Comparator<RunResult> byEfSearch =
      Comparator.comparingInt(r -> Integer.parseInt(r.getParams().getParam("efSearch")));
    List<RunResult> searchResults =
      results.stream()
        .filter(r -> r.getPrimaryResult().getLabel().equals("search"))
        .sorted(byMetric.thenComparing(byEfSearch))
        .toList();

    for (RunResult r : searchResults) {
      var params = r.getParams();
      String metric = params.getParam("distanceMetric");
      int mParam = Integer.parseInt(params.getParam("m"));
      int efcParam = Integer.parseInt(params.getParam("efConstruction"));
      int efsParam = Integer.parseInt(params.getParam("efSearch"));
      String dsName = params.getParam("datasetName");
      String dsPath = params.getParam("dataPath");

      BenchmarkData data = BenchmarkData.load(dsPath, dsName, metric, K);

      Map<String, Result> secondaryResults = r.getSecondaryResults();
      Result hitsResult = secondaryResults.get("hits");
      Result totalQueriesResult = secondaryResults.get("totalQueries");

      double totalHits = (hitsResult != null) ? hitsResult.getScore() : 0.0;
      double totalQueries = (totalQueriesResult != null) ? totalQueriesResult.getScore() : 0.0;

      // Recall is |retrieved ∩ relevant| / |relevant|. The benchmark measures recall once per
      // trial and reports the same totals every iteration, so the counters sum across iterations
      // and forks. Both counters scale identically, so their ratio is the per-query average hit
      // count, which is divided by the mean ground-truth size (capped at K).
      double avgRelevant =
        data.groundTruth().values().stream()
          .mapToDouble(truth -> Math.min(K, truth.size()))
          .average()
          .orElse(0.0);
      double recall =
        (totalQueries == 0 || avgRelevant == 0) ? 0 : totalHits / (totalQueries * avgRelevant);

      // The search benchmark op is a single query, so per-query time is the inverse of the JMH
      // throughput score (ops/ms).
      double throughputOpsPerMs = r.getPrimaryResult().getScore();
      double throughputError = r.getPrimaryResult().getScoreError();
      double avgTimePerQuery = 1.0 / throughputOpsPerMs;

      // Propagate the relative error: error_time/time = error_throughput/throughput
      double relativeError = throughputError / throughputOpsPerMs;
      double avgTimeError = avgTimePerQuery * relativeError;
      String avgTimeStr = String.format("%.4f ± %.4f", avgTimePerQuery, avgTimeError);

      String buildTimeStr = "n/a";
      RunResult buildResult = buildResultsByKey.get(paramKey(r));
      if (buildResult != null) {
        // The build benchmark runs in single-shot mode with a millisecond output unit.
        double buildMs = buildResult.getPrimaryResult().getScore();
        double buildErrMs = buildResult.getPrimaryResult().getScoreError();
        buildTimeStr =
          Double.isNaN(buildErrMs)
            ? String.format("%.1f", buildMs / 1000.0)
            : String.format("%.1f ± %.1f", buildMs / 1000.0, buildErrMs / 1000.0);
      }

      String dims =
        String.format(
          "%dx%d",
          data.trainingData().get(0).toFloatVectors().size(),
          data.trainingData().get(0).toFloatVectors().get(0).length());

      System.out.printf(
        "%-20s | %-8d | %-8d | %-8s | %-5d | %-8d | %-8d | %-18s | %-20s | %.4f\n",
        metric,
        data.trainingData().size(),
        data.testData().size(),
        dims,
        mParam,
        efcParam,
        efsParam,
        buildTimeStr,
        avgTimeStr,
        recall);
    }
    System.out.println();
  }
}
