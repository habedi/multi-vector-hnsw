// src/benchmark/java/io/github/habedi/mvhnsw/bench/data/GroundTruth.java
package io.github.habedi.mvhnsw.bench.data;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroundTruth {

  @JsonProperty("id")
  private long id;

  private final Map<String, Map<String, List<?>>> neighborhoods = new HashMap<>();

  public long id() {
    return id;
  }

  public Map<String, Map<String, List<?>>> neighborhoods() {
    return neighborhoods;
  }

  @JsonAnySetter
  public void setNeighborhood(String name, Map<String, List<?>> value) {
    this.neighborhoods.put(name, value);
  }
}
