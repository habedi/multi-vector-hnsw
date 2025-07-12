// src/benchmark/java/io/github/habedi/mvhnsw/bench/data/BenchmarkData.java
package io.github.habedi.mvhnsw.bench.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record BenchmarkData(
  List<TestItem> trainingData, List<TestItem> testData, Map<Long, Set<Long>> groundTruth) {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static BenchmarkData load(String datasetName, String distanceMetric, int k)
    throws IOException {
    Path dataPath = Paths.get("benches", "data", datasetName);

    List<TestItem> trainingData =
      MAPPER.readValue(dataPath.resolve("train.json").toFile(), new TypeReference<>() {});

    List<TestItem> testData =
      MAPPER.readValue(dataPath.resolve("test.json").toFile(), new TypeReference<>() {});

    List<GroundTruth> groundTruthData =
      MAPPER.readValue(dataPath.resolve("neighbours.json").toFile(), new TypeReference<>() {});

    String groundTruthKey = String.format("top_%d_%s", k, distanceMetric.toLowerCase());

    Map<Long, Set<Long>> groundTruthMap =
      groundTruthData.stream()
        .collect(
          Collectors.toMap(
            GroundTruth::id,
            gt -> {
              Map<String, List<?>> neighborhood = gt.neighborhoods().get(groundTruthKey);
              if (neighborhood == null) {
                throw new IllegalStateException(
                  "Ground truth for metric '" + groundTruthKey + "' not found.");
              }
              return ((List<Number>) neighborhood.get("ids"))
                .stream()
                .map(Number::longValue)
                .collect(Collectors.toSet());
            }));

    return new BenchmarkData(trainingData, testData, groundTruthMap);
  }
}
