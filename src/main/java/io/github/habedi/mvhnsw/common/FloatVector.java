package io.github.habedi.mvhnsw.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

/**
 * An immutable, serializable implementation of a {@link Vector} using a primitive float array for
 * efficiency.
 */
public final class FloatVector implements Vector<Float>, Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final float[] data;
    private transient volatile double norm = -1;

    /**
     * Constructs a FloatVector from a float array. The input array is cloned to maintain
     * immutability.
     *
     * @param data The float array to create the vector from. Cannot be null or empty.
     */
    public FloatVector(float[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Vector data cannot be null or empty.");
        }
        this.data = data.clone(); // FIX: Clone array to guarantee immutability
    }

    /**
     * Static factory method for creating a FloatVector from a varargs float array.
     *
     * @param data The float values.
     * @return a new FloatVector.
     */
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
     * @return a clone of the internal data array.
     */
    public float[] toPrimitiveArray() {
        return data.clone();
    }

    @Override
    public Vector<Float> add(Vector<Float> other) {
        if (!(other instanceof FloatVector)) {
            throw new UnsupportedOperationException("Addition with non-FloatVector not supported.");
        }
        return add((FloatVector) other);
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
        if (!(other instanceof FloatVector)) {
            throw new UnsupportedOperationException(
                    "Multiplication with non-FloatVector not supported.");
        }
        return mul((FloatVector) other);
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
        if (!(other instanceof FloatVector)) {
            throw new UnsupportedOperationException(
                    "Dot product with non-FloatVector not supported.");
        }
        return dot((FloatVector) other);
    }

    public double dot(FloatVector other) {
        if (this.length() != other.length()) {
            throw new IllegalArgumentException("Vector lengths must be equal for dot product.");
        }
        double sum = 0.0;
        for (int i = 0; i < data.length; i++) {
            sum += (double) this.data[i] * other.data[i];
        }
        return sum;
    }

    @Override
    public double norm() {
        if (norm < 0) {
            synchronized (this) {
                if (norm < 0) {
                    double sumSq = 0.0;
                    for (float v : data) {
                        sumSq += (double) v * v;
                    }
                    this.norm = Math.sqrt(sumSq);
                }
            }
        }
        return this.norm;
    }

    @Override
    public double cosine(Vector<Float> other) {
        double dot = dot(other);
        double norms = this.norm() * other.norm();
        if (norms == 0.0) {
            return 0.0; // Conventionally, cosine is 0 if either vector is the zero vector.
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
