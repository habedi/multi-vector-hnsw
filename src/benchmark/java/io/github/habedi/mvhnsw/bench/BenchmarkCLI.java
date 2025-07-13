package io.github.habedi.mvhnsw.bench;

import io.github.habedi.mvhnsw.bench.IndexBenchmark.BenchmarkResult;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

  @Option(
    names = {"-d", "--dataset"},
    description = "The name of the dataset to use for the benchmark.",
    defaultValue = "se_cs_768")
  private String datasetName;

  @Option(
    names = {"--data-path"},
    description = "The base path to the benchmark data directory.",
    defaultValue = "benches/data")
  private String dataPath;

  @Option(names = {"-m"}, description = "The M parameter for HNSW.", defaultValue = "16")
  private int m;

  @Option(
    names = {"-ef", "--ef-construction"},
    description = "The efConstruction parameter for HNSW.",
    defaultValue = "200")
  private int efConstruction;

  @Option(
    names = {"-p", "--profiler"},
    description = "Enable a JMH profiler (e.g., 'stack', 'jfr').")
  private String profiler;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new BenchmarkCLI()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws RunnerException {
    log.info(
      "Starting benchmark with dataset={}, m={}, efConstruction={}",
      datasetName,
      m,
      efConstruction);

    ChainedOptionsBuilder builder =
      new OptionsBuilder()
        .include(IndexBenchmark.class.getSimpleName())
        .param("datasetName", datasetName)
        .param("dataPath", dataPath)
        .param("m", String.valueOf(m))
        .param("efConstruction", String.valueOf(efConstruction));

    if (profiler != null && !profiler.isBlank()) {
      log.info("Enabling JMH profiler: {}", profiler);
      builder.addProfiler(profiler);
    }

    Options opt = builder.build();
    Collection<RunResult> results = new Runner(opt).run();
    printSummaryTable(results);

    return 0;
  }

  private void printSummaryTable(Collection<RunResult> results) {
    System.out.println("\n\n--- HNSW Benchmark Summary ---");
    String header =
      String.format(
        "%-20s | %-8s | %-8s | %-8s | %-5s | %-8s | %-22s | %-12s",
        "Distance", "Train", "Test", "Dims", "M", "efConst", "Throughput (ops/ms)", "Recall@100");
    System.out.println(header);
    System.out.println(new String(new char[header.length()]).replace("\0", "-"));

    for (RunResult r : results) {
      String benchmarkName = r.getPrimaryResult().getLabel();
      if (!benchmarkName.equals("search")) {
        continue;
      }

      var params = r.getParams();
      String metric = params.getParam("distanceMetric");
      int mParam = Integer.parseInt(params.getParam("m"));
      int efcParam = Integer.parseInt(params.getParam("efConstruction"));

      String key =
        IndexBenchmark.getParamKey(
          params.getParam("datasetName"), metric, mParam, efcParam);
      BenchmarkResult data = IndexBenchmark.benchmarkData.get(key);
      if (data == null) {
        continue;
      }

      double throughput = r.getPrimaryResult().getScore();
      double throughputError = r.getPrimaryResult().getScoreError();
      String throughputStr = String.format("%.4f ± %.4f", throughput, throughputError);
      String dims = String.format("%dx%d", data.numVectors(), data.vectorDim());

      System.out.printf(
        "%-20s | %-8d | %-8d | %-8s | %-5d | %-8d | %-22s | %.4f\n",
        metric,
        data.trainSize(),
        data.testSize(),
        dims,
        mParam,
        efcParam,
        throughputStr,
        data.recall());
    }
    System.out.println();
  }
}
