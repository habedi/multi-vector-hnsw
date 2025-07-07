package io.github.habedi.mvhnsw.index;

import io.github.habedi.mvhnsw.common.Vector;
import io.github.habedi.mvhnsw.distance.MultiVectorDistance;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for a searchable index supporting full CRUD and bulk operations.
 *
 * @param <V> The type of vector used in the items.
 * @param <P> The type of payload stored in the items.
 */
public interface Index<V extends Vector<?>, P> {

    /**
     * Adds a single item to the index. If an item with the same ID already exists, it will be
     * updated.
     *
     * @param item the item to add.
     */
    void add(Item<V, P> item);

    /**
     * Updates an existing item in the index.
     *
     * @param item the item to update.
     * @throws java.util.NoSuchElementException if no item with the given ID exists.
     */
    void update(Item<V, P> item);

    /**
     * Removes an item from the index by its ID. This is a soft delete; the node is marked as
     * deleted but not immediately removed from the graph structure.
     *
     * @param id the ID of the item to remove.
     * @return true if the item was found and removed, false otherwise.
     */
    boolean remove(long id);

    /**
     * Adds a collection of items to the index.
     *
     * @param items the collection of items to add.
     */
    void addAll(Collection<Item<V, P>> items);

    /**
     * Updates a collection of existing items in the index.
     *
     * @param items the collection of items to update.
     */
    void updateAll(Collection<Item<V, P>> items);

    /**
     * Removes a collection of items from the index by their IDs.
     *
     * @param ids the IDs of the items to remove.
     * @return the number of items that were actually removed.
     */
    int removeAll(Collection<Long> ids);

    /**
     * Searches the index for the {@code k} nearest neighbors to the given query vectors.
     *
     * @param queryVectors the list of query vectors.
     * @param k the number of nearest neighbors to return.
     * @return a list of search results, sorted by score (distance).
     */
    List<SearchResult<V, P>> search(List<V> queryVectors, int k);

    /**
     * Retrieves an item from the index by its ID.
     *
     * @param id the ID of the item to retrieve.
     * @return an {@link Optional} containing the item if found, or an empty Optional otherwise.
     */
    Optional<Item<V, P>> get(long id);

    /**
     * Returns the number of items currently in the index.
     *
     * @return the size of the index.
     */
    int size();

    /**
     * Gets the distance function used by the index.
     *
     * @return the multi-vector distance function.
     */
    MultiVectorDistance<V> getDistance();

    /**
     * Saves the current state of the index to the specified path.
     *
     * @param path the path to save the index file to.
     * @throws IOException if an I/O error occurs.
     */
    void save(Path path) throws IOException;

    /** Removes all items from the index, leaving it empty. */
    void clear();

    /**
     * Rebuilds the index to physically remove items that were soft-deleted. This is a potentially
     * long-running operation that locks the index. It should be called periodically if the index
     * has a high rate of deletions or updates.
     */
    void vacuum();
}
