package io.github.habedi.mvhnsw.distance;

import io.github.habedi.mvhnsw.common.FloatVector;
import java.util.List;

public interface MultiVectorDistance {

  double compute(List<FloatVector> vectors1, List<FloatVector> vectors2);
}
