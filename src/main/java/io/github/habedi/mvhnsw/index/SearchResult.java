package io.github.habedi.mvhnsw.index;

/**
 * Represents a single result from a search operation.
 *
 * <p>This is a data-centric record that holds the identifier of a found item and its corresponding
 * score, which typically represents the distance from the query.
 *
 * @param id The unique identifier of the item found.
 * @param score The score of the item, usually the distance from the query vector. Lower scores are
 *     generally better.
 */
public record SearchResult(long id, double score) {}
