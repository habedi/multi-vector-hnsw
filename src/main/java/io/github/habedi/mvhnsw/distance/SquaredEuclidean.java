// src/main/java/io/github/habedi/mvhnsw/distance/SquaredEuclidean.java
package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.io.Serial;
import java.io.Serializable;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class SquaredEuclidean implements Distance<FloatVector>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private static final VectorSpecies<java.lang.Float> SPECIES =
      jdk.incubator.vector.FloatVector.SPECIES_PREFERRED;

  @Override
  public double compute(FloatVector a, FloatVector b) {
    if (a.length() != b.length()) {
      throw new IllegalArgumentException("Vector lengths must be equal.");
    }

    float[] v1 = a.getRawData();
    float[] v2 = b.getRawData();
    double sumSq = 0.0;
    int bound = SPECIES.loopBound(v1.length);
    int i = 0;

    for (; i < bound; i += SPECIES.length()) {
      var va = jdk.incubator.vector.FloatVector.fromArray(SPECIES, v1, i);
      var vb = jdk.incubator.vector.FloatVector.fromArray(SPECIES, v2, i);
      var diff = va.sub(vb);
      sumSq += diff.mul(diff).reduceLanes(VectorOperators.ADD);
    }

    for (; i < v1.length; i++) {
      double diff = v1[i] - v2[i];
      sumSq += diff * diff;
    }

    return sumSq;
  }

  @Override
  public double computeSquared(FloatVector a, FloatVector b) {
    return compute(a, b);
  }

  @Override
  public String getName() {
    return "SquaredEuclidean";
  }
}
