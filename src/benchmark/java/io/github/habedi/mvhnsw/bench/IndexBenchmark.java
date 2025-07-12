package io.github.habedi.mvhnsw.bench;

import io.github.habedi.mvhnsw.bench.data.BenchmarkData;
import io.github.habedi.mvhnsw.bench.data.TestItem;
import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Cosine;
import io.github.habedi.mvhnsw.distance.Distance;
import io.github.habedi.mvhnsw.distance.Euclidean;
import io.github.habedi.mvhnsw.index.Index;
import io.github.habedi.mvhnsw.index.MultiVectorHNSW;
import io.github.habedi.mvhnsw.index.SearchResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 3, time = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class IndexBenchmark {

    private static final int K = 100;
    private static final LongAdder HITS = new LongAdder();
    private static final LongAdder TOTAL_QUERIES = new LongAdder();

    @Param({"glove-25-angular"})
    public String datasetName;

    @Param({"16"})
    public int m;

    @Param({"200"})
    public int efConstruction;

    private BenchmarkData benchmarkData;
    private Index index;

    @Setup(Level.Trial)
    public void setupTrial() throws IOException {
        benchmarkData = BenchmarkData.load(datasetName);
        index = buildIndex(); // Build index once per trial
    }

    @Setup(Level.Invocation)
    public void setupInvocation() {
        HITS.reset();
        TOTAL_QUERIES.reset();
    }

    @TearDown(Level.Invocation)
    public void tearDownInvocation() {
        long totalHits = HITS.sum();
        long totalQueries = TOTAL_QUERIES.sum();
        double recall = (totalQueries == 0) ? 0 : (double) totalHits / (totalQueries * K);
        System.out.printf("\nRecall@%d: %.4f%n", K, recall);
    }

    @Benchmark
    public Index build() {
        return buildIndex();
    }

    @Benchmark
    public void search(Blackhole bh) {
        for (TestItem testItem : benchmarkData.testData()) {
            List<SearchResult> results = index.search(testItem.toFloatVectors(), K);
            updateRecall(testItem.id(), results, benchmarkData.groundTruth());
            bh.consume(results);
        }
    }

    private Index buildIndex() {
        List<FloatVector> vectors = benchmarkData.trainingData().get(0).toFloatVectors();
        int numVectors = vectors.size();
        float weight = 1.0f / numVectors;

        MultiVectorHNSW.Builder.WeightedAverageDistanceBuilder distanceBuilder =
            MultiVectorHNSW.builder()
                .withM(m)
                .withEfConstruction(efConstruction)
                .withWeightedAverageDistance();

        Distance<FloatVector> distance =
            datasetName.contains("angular") ? new Cosine() : new Euclidean();
        for (int i = 0; i < numVectors; i++) {
            distanceBuilder.addDistance(distance, weight);
        }

        Index newIndex = distanceBuilder.and().build();
        for (TestItem item : benchmarkData.trainingData()) {
            newIndex.add(item.id(), item.toFloatVectors());
        }
        return newIndex;
    }

    private void updateRecall(
        long queryId, List<SearchResult> results, Map<Long, Set<Long>> groundTruth) {
        Set<Long> truth = groundTruth.get(queryId);
        if (truth != null) {
            long hits = results.stream().filter(r -> truth.contains(r.id())).count();
            HITS.add(hits);
            TOTAL_QUERIES.increment();
        }
    }
}
