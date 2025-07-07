package com.example.projectname.benchmarks;

import com.example.projectname.internal.StringUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class AppBenchmark {

    @Param({"10", "100", "1000"})
    private int length;

    private String testString;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(AppBenchmark.class.getSimpleName())
            .build();
        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        testString = sb.toString();
    }

    @Benchmark
    public String benchmarkStringReverse() {
        return StringUtils.reverse(testString);
    }
}
