package io.github.habedi.mvhnsw.common;

import static jdk.incubator.vector.FloatVector.SPECIES_PREFERRED;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * A final, serializable implementation of a vector of floats.
 *
 * <p>This class provides common vector operations and is optimized with the Java Vector API for
 * performance. It is immutable by cloning the input data array.
 */
public final class FloatVector implements Vector<Float>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private static final VectorSpecies<Float> SPECIES = SPECIES_PREFERRED;

  /** The internal, private array storing the vector's components. */
  private final float[] data;

  /** A transient, volatile field to cache the calculated L2 norm for performance. */
  private transient volatile double norm = -1;

  /**
   * Constructs a new FloatVector.
   *
   * @param data The float array to create the vector from. The data is cloned to maintain
   *     immutability.
   * @throws IllegalArgumentException if the data array is null or empty.
   */
  public FloatVector(float[] data) {
    if (data == null || data.length == 0) {
      throw new IllegalArgumentException("Vector data cannot be null or empty.");
    }
    this.data = data.clone();
  }

  /**
   * A factory method for creating a FloatVector from a varargs array of floats.
   *
   * @param data The float values to include in the vector.
   * @return A new FloatVector instance.
   */
  public static FloatVector of(float... data) {
    return new FloatVector(data);
  }

  /**
   * Provides direct, read-only access to the internal float array.
   *
   * <p><b>Warning:</b> This method is intended for performance-critical operations within the
   * library only. The returned array should NOT be modified, as it would corrupt the vector's
   * state. It is not cloned for performance reasons.
   *
   * @return The raw internal float array.
   */
  public float[] getRawData() {
    return data;
  }

  @Override
  public int length() {
    return data.length;
  }

  @Override
  public Float get(int i) {
    return data[i];
  }

  /**
   * Gets the primitive float element at the specified index.
   *
   * @param i the index of the element to return.
   * @return the primitive float at the specified position.
   */
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

  /**
   * Returns a clone of the underlying primitive float array.
   *
   * @return A new array containing all the elements in this vector.
   */
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

  /**
   * Adds another FloatVector to this vector, performing element-wise addition.
   *
   * @param other The FloatVector to be added to this vector.
   * @return A new FloatVector that is the sum of this vector and the other.
   * @throws IllegalArgumentException if the vector lengths are not equal.
   */
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

  /**
   * Multiplies this vector by another FloatVector, performing element-wise multiplication.
   *
   * @param other The FloatVector to be multiplied by this vector.
   * @return A new FloatVector that is the product of this vector and the other.
   * @throws IllegalArgumentException if the vector lengths are not equal.
   */
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

  /**
   * Computes the dot product of this vector and another FloatVector. This operation is optimized
   * using the Java Vector API.
   *
   * @param other The other FloatVector.
   * @return The dot product of the two vectors.
   * @throws IllegalArgumentException if the vector lengths are not equal.
   */
  public double dot(FloatVector other) {
    if (this.length() != other.length()) {
      throw new IllegalArgumentException("Vector lengths must be equal for dot product.");
    }

    float[] a = this.data;
    float[] b = other.getRawData();
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
