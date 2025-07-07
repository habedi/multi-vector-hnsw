package io.github.habedi.mvhnsw.common;

/**
 * Represents a generic vector of numbers.
 *
 * @param <T> the type of number this vector holds.
 */
public interface Vector<T extends Number> {

    /**
     * Returns the number of elements in the vector.
     *
     * @return the length of the vector.
     */
    int length();

    /**
     * Gets the element at the specified index.
     *
     * @param i the index of the element to return.
     * @return the element at the specified position in this vector.
     */
    T get(int i);

    /**
     * Returns an array containing all the elements in this vector in proper sequence.
     *
     * @return an array containing all the elements in this vector.
     */
    T[] toArray();

    /**
     * Adds another vector to this vector, performing element-wise addition.
     *
     * @param other the vector to be added to this vector.
     * @return a new vector that is the sum of this vector and the other vector.
     */
    Vector<T> add(Vector<T> other);

    /**
     * Multiplies this vector by another vector, performing element-wise multiplication.
     *
     * @param other the vector to be multiplied by this vector.
     * @return a new vector that is the product of this vector and the other vector.
     */
    Vector<T> mul(Vector<T> other);

    /**
     * Computes the dot product of this vector and another vector.
     *
     * @param other the other vector.
     * @return the dot product of the two vectors.
     */
    double dot(Vector<T> other);

    /**
     * Computes the Euclidean norm (L2 norm) of the vector.
     *
     * @return the Euclidean norm of the vector.
     */
    double norm();

    /**
     * Computes the cosine similarity between this vector and another vector.
     *
     * @param other the other vector.
     * @return the cosine similarity, a value between -1 and 1.
     */
    double cosine(Vector<T> other);
}
