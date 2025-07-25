package io.github.habedi.mvhnsw.bench;

import io.github.habedi.mvhnsw.bench.data.BenchmarkData;

import java.io.IOException;
import java.util.Collection;
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
    description = "The efSearch parameter for HNSW.",
    defaultValue = "100")
  private int efSearch;

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
      efSearch);

    ChainedOptionsBuilder builder =
      new OptionsBuilder()
        .include(IndexBenchmark.class.getSimpleName())
        .param("datasetName", datasetName)
        .param("dataPath", dataPath)
        .param("m", String.valueOf(m))
        .param("efConstruction", String.valueOf(efConstruction))
        .param("efSearch", String.valueOf(efSearch));

    if (profiler != null && !profiler.isBlank()) {
      log.info("Enabling JMH profiler: {}", profiler);
      builder.addProfiler(profiler);
    }

    Options opt = builder.build();
    Collection<RunResult> results = new Runner(opt).run();
    printSummaryTable(results);

    return 0;
  }

  private void printSummaryTable(Collection<RunResult> results) throws IOException {
    System.out.println("\n\n--- HNSW Benchmark Summary ---");
    String header =
      String.format(
        "%-20s | %-8s | %-8s | %-8s | %-5s | %-8s | %-8s | %-20s | %-12s",
        "Distance",
        "Train",
        "Test",
        "Dims",
        "M",
        "efConst",
        "efSearch",
        "Avg Time/Query (ms)",
        "Recall@" + K);
    System.out.println(header);
    System.out.println(new String(new char[header.length()]).replace("\0", "-"));

    Map<String, RunResult> searchResultsByMetric =
      results.stream()
        .filter(r -> r.getPrimaryResult().getLabel().equals("search"))
        .collect(Collectors.toMap(r -> r.getParams().getParam("distanceMetric"), r -> r));

    for (String metric : List.of("squared_euclidean", "cosine", "dot_product")) {
      RunResult r = searchResultsByMetric.get(metric);
      if (r == null) {
        continue;
      }

      var params = r.getParams();
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
      double recall = (totalQueries == 0) ? 0 : totalHits / (totalQueries * K);

      // JMH score for throughput is in (ops/ms). An "op" is one full run of the benchmark method.
      double throughputOpsPerMs = r.getPrimaryResult().getScore();
      double throughputError = r.getPrimaryResult().getScoreError();

      // Our benchmark "op" runs searches for the entire test dataset.
      double queriesPerOp = data.testData().size();

      // Calculate the average time for a single query.
      double avgTimePerQuery = 1.0 / (throughputOpsPerMs * queriesPerOp);

      // Propagate the relative error: error_time/time = error_throughput/throughput
      double relativeError = throughputError / throughputOpsPerMs;
      double avgTimeError = avgTimePerQuery * relativeError;
      String avgTimeStr = String.format("%.4f ± %.4f", avgTimePerQuery, avgTimeError);

      String dims =
        String.format(
          "%dx%d",
          data.trainingData().get(0).toFloatVectors().size(),
          data.trainingData().get(0).toFloatVectors().get(0).length());

      System.out.printf(
        "%-20s | %-8d | %-8d | %-8s | %-5d | %-8d | %-8d | %-20s | %.4f\n",
        metric,
        data.trainingData().size(),
        data.testData().size(),
        dims,
        mParam,
        efcParam,
        efsParam,
        avgTimeStr,
        recall);
    }
    System.out.println();
  }
}
