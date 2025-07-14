package io.github.habedi.mvhnsw.index;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.MultiVectorDistance;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Defines the public contract for a multi-vector search index.
 *
 * <p>This interface provides methods for adding, removing, retrieving, and searching for items,
 * where each item is identified by a unique ID and represented by a list of vectors.
 */
public interface Index {

  /**
   * Adds a new item to the index.
   *
   * @param id A unique identifier for the item.
   * @param vectors A list of {@link FloatVector}s that represents the item.
   * @throws IllegalArgumentException if an item with the same ID already exists.
   */
  void add(long id, List<FloatVector> vectors);

  /**
   * Marks an item for deletion.
   *
   * <p>This performs a "soft delete." The item will be excluded from search results, but its data
   * is not immediately removed from the index. Use {@link #vacuum()} to permanently remove all
   * deleted items.
   *
   * @param id The unique identifier of the item to remove.
   * @return {@code true} if the item was found and marked for deletion, {@code false} otherwise.
   */
  boolean remove(long id);

  /**
   * Adds a batch of items to the index.
   *
   * @param items A map where keys are the unique item IDs and values are the corresponding lists of
   *     vectors.
   */
  void addAll(Map<Long, List<FloatVector>> items);

  /**
   * Searches the index for the {@code k} nearest neighbors to a given query.
   *
   * @param queryVectors The list of vectors representing the query.
   * @param k The number of nearest neighbors to return.
   * @return A list of {@link SearchResult}s, sorted by distance in ascending order.
   */
  List<SearchResult> search(List<FloatVector> queryVectors, int k);

  /**
   * Retrieves the list of vectors for a given item ID.
   *
   * @param id The unique identifier of the item.
   * @return An {@link Optional} containing the list of vectors if the item exists and has not been
   *     deleted, or an empty Optional otherwise.
   */
  Optional<List<FloatVector>> get(long id);

  /**
   * Returns the number of active (non-deleted) items in the index.
   *
   * @return The total number of items.
   */
  int size();

  /**
   * Returns a set view of the IDs of all active (non-deleted) items in the index.
   *
   * @return A {@link Set} of unique item identifiers.
   */
  Set<Long> keySet();

  /**
   * Gets the distance function used by the index.
   *
   * @return The {@link MultiVectorDistance} instance configured for this index.
   */
  MultiVectorDistance getDistance();

  /**
   * Saves the current state of the index to a file.
   *
   * @param path The {@link Path} to the file where the index will be saved.
   * @throws IOException if an I/O error occurs during serialization.
   */
  void save(Path path) throws IOException;

  /** Clears all items from the index, resetting it to an empty state. */
  void clear();

  /**
   * Rebuilds the index to permanently remove items that were marked for deletion via the {@link
   * #remove(long)} method. This operation can be expensive and should be called periodically after
   * a large number of removals to reclaim memory and maintain performance.
   */
  void vacuum();
}
