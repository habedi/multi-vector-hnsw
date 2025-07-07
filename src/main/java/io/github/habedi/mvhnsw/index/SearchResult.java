package io.github.habedi.mvhnsw.index;

import io.github.habedi.mvhnsw.common.Vector;

/**
 * Represents a single result from a search query.
 *
 * @param item The item that was found.
 * @param score The distance or similarity score of the item to the query.
 * @param <V> The type of vector used in the item.
 * @param <P> The type of payload stored in the item.
 */
public record SearchResult<V extends Vector<?>, P>(Item<V, P> item, double score) {}
