package io.github.habedi.mvhnsw.bench;

import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "benchmark",
    mixinStandardHelpOptions = true,
    description = "Runs the benchmark for Multi-Vector HNSW.",
public class BenchmarkCLI implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(BenchmarkCLI.class);

    @Option(
        names = {"-d", "--dataset"},
        description = "The name of the dataset to use for the benchmark.",
        defaultValue = "se_cs_768")
    private String datasetName;

    @Option(names = {"-m"}, description = "The M parameter for HNSW.", defaultValue = "16")
    private int m;

    @Option(
        names = {"-ef", "--ef-construction"},
        description = "The efConstruction parameter for HNSW.",
        defaultValue = "200")
    private int efConstruction;

    @Override
    public Integer call() throws Exception {
        log.info(
            "Starting benchmark with dataset={}, m={}, efConstruction={}",
            datasetName,
            m,
            efConstruction);

        Options opt =
            new OptionsBuilder()
                .include(IndexBenchmark.class.getSimpleName())
                .param("datasetName", datasetName)
                .param("m", String.valueOf(m))
                .param("efConstruction", String.valueOf(efConstruction))
                .jvmArgs("--add-modules", "jdk.incubator.vector")
                .build();

        new Runner(opt).run();
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BenchmarkCLI()).execute(args);
        System.exit(exitCode);
    }
}
