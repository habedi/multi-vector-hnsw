package io.github.habedi.mvhnsw.bench.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.habedi.mvhnsw.common.FloatVector;

import java.util.List;
import java.util.stream.Collectors;

public record TestItem(@JsonProperty("id") long id,
                       @JsonProperty("embedding") List<List<Float>> embedding) {

  public List<FloatVector> toFloatVectors() {
    return embedding.stream()
      .map(
        list -> {
          float[] arr = new float[list.size()];
          for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
          }
          return new FloatVector(arr);
        })
      .collect(Collectors.toList());
  }
}
