package io.github.habedi.mvhnsw.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public final class FloatVector implements Vector<Float>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private static final VectorSpecies<Float> SPECIES =
      jdk.incubator.vector.FloatVector.SPECIES_PREFERRED;

  private final float[] data;
  private transient volatile double norm = -1;

  public FloatVector(float[] data) {
    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("Vector data cannot be null or empty.");
    }
    this.data = data.clone();
  }

  public static FloatVector of(float... data) {
    return new FloatVector(data);
  }

  @Override
  public int length() {
    return data.length;
  }

  @Override
  public Float get(int i) {
    return data[i];
  }

  public float getPrimitive(int i) {
    return data[i];
  }

  @Override
  public Float[] toArray() {
    Float[] boxed = new Float[data.length];
    for (int i = 0; i < data.length; i++) {
      boxed[i] = data[i];
    }
    return boxed;
  }

  public float[] toPrimitiveArray() {
    return data.clone();
  }

  @Override
  public Vector<Float> add(Vector<Float> other) {
    if (other instanceof FloatVector fv) {
      return add(fv);
    }
    throw new UnsupportedOperationException("Addition with non-FloatVector not supported.");
  }

  public FloatVector add(FloatVector other) {
    if (this.length() != other.length()) {
      throw new IllegalArgumentException("Vector lengths must be equal for addition.");
    }
    float[] result = new float[data.length];
    for (int i = 0; i < data.length; i++) {
      result[i] = this.data[i] + other.data[i];
    }
    return new FloatVector(result);
  }

  @Override
  public Vector<Float> mul(Vector<Float> other) {
    if (other instanceof FloatVector fv) {
      return mul(fv);
    }
    throw new UnsupportedOperationException("Multiplication with non-FloatVector not supported.");
  }

  public FloatVector mul(FloatVector other) {
    if (this.length() != other.length()) {
      throw new IllegalArgumentException("Vector lengths must be equal for multiplication.");
    }
    float[] result = new float[data.length];
    for (int i = 0; i < data.length; i++) {
      result[i] = this.data[i] * other.data[i];
    }
    return new FloatVector(result);
  }

  @Override
  public double dot(Vector<Float> other) {
    if (other instanceof FloatVector fv) {
      return dot(fv);
    }
    throw new UnsupportedOperationException("Dot product with non-FloatVector not supported.");
  }

  public double dot(FloatVector other) {
    if (this.length() != other.length()) {
      throw new IllegalArgumentException("Vector lengths must be equal for dot product.");
    }

    float[] a = this.data;
    float[] b = other.data;
    double sum = 0.0;
    int bound = SPECIES.loopBound(a.length);
    int i = 0;

    for (; i < bound; i += SPECIES.length()) {
      var va = jdk.incubator.vector.FloatVector.fromArray(SPECIES, a, i);
      var vb = jdk.incubator.vector.FloatVector.fromArray(SPECIES, b, i);
      sum += va.mul(vb).reduceLanes(VectorOperators.ADD);
    }

    for (; i < a.length; i++) {
      sum += (double) a[i] * b[i];
    }
    return sum;
  }

  @Override
  public double norm() {
    if (norm < 0) { // First check (no lock)
      synchronized (this) {
        if (norm < 0) { // Second check (with lock)
          double sumSq = 0.0;
          int bound = SPECIES.loopBound(data.length);
          int i = 0;

          for (; i < bound; i += SPECIES.length()) {
            var va = jdk.incubator.vector.FloatVector.fromArray(SPECIES, data, i);
            sumSq += va.mul(va).reduceLanes(VectorOperators.ADD);
          }

          for (; i < data.length; i++) {
            sumSq += (double) data[i] * data[i];
          }
          this.norm = Math.sqrt(sumSq);
        }
      }
    }
    return norm;
  }

  @Override
  public double cosine(Vector<Float> other) {
    double dot = dot(other);
    double norms = this.norm() * other.norm();
    if (norms == 0.0) {
      return 0.0;
    }
    return dot / norms;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FloatVector that = (FloatVector) o;
    return Arrays.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public String toString() {
    return "FloatVector{" + "data=" + Arrays.toString(data) + '}';
  }
}
