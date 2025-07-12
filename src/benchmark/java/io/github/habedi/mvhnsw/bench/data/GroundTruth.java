package io.github.habedi.mvhnsw.bench.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GroundTruth(
    @JsonProperty("id") long id,
    @JsonProperty("top_100_euclidean") Map<String, List<?>> topKNeighborhood) {
}
