package io.github.habedi.mvhnsw.index;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.Distance;
import io.github.habedi.mvhnsw.distance.MultiVectorDistance;
import io.github.habedi.mvhnsw.distance.WeightedAverageDistance;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A thread-safe, serializable implementation of the {@link Index} interface using a Hierarchical
 * Navigable Small World (HNSW) graph.
 *
 * <p>This class provides a high-performance solution for approximate nearest neighbor search on
 * multi-vector data. All write operations (add, remove, clear, vacuum) are protected by a write
 * lock, and all read operations (search, get, size) are protected by a read lock.
 */
public final class MultiVectorHNSW implements Index, Serializable {

  @Serial private static final long serialVersionUID = 2L;
  private static final Logger log = LogManager.getLogger(MultiVectorHNSW.class);

  private final MultiVectorDistance multiVectorDistance;
  private final int m;
  private final int efConstruction;
  private final double levelLambda;

  /** Stores the vector data for each item ID. */
  private final Map<Long, List<FloatVector>> vectorMap;

  /** Stores the graph structure (nodes and their connections). */
  private final Map<Long, Node> nodes;

  /** A lock to manage concurrent access to the index. */
  private transient ReentrantReadWriteLock lock;

  /**
   * The entry point for all search and insertion operations, always pointing to the top-most layer.
   */
  private volatile Node entryPoint;

  /** Private constructor to be called by the {@link Builder}. */
  private MultiVectorHNSW(Builder builder) {
    this.multiVectorDistance = builder.multiVectorDistance;
    this.m = builder.m;
    this.efConstruction = builder.efConstruction;
    this.levelLambda = 1 / Math.log(m);
    this.vectorMap = new ConcurrentHashMap<>();
    this.nodes = new ConcurrentHashMap<>();
    this.lock = new ReentrantReadWriteLock();
    this.entryPoint = null;
    log.info(
        "Initialized MultiVectorHNSW with M={}, efConstruction={}, distance={}",
        this.m,
        this.efConstruction,
        this.multiVectorDistance.getClass().getSimpleName());
  }

  /**
   * Creates a new {@link Builder} to configure and construct a MultiVectorHNSW index.
   *
   * @return A new Builder instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Loads an index from a file.
   *
   * @param path The path to the serialized index file.
   * @return A new instance of MultiVectorHNSW with the loaded data.
   * @throws IOException if an I/O error occurs while reading the file.
   * @throws ClassNotFoundException if the class of a serialized object cannot be found.
   */
  public static MultiVectorHNSW load(Path path) throws IOException, ClassNotFoundException {
    log.info("Loading index from {}", path);
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path.toFile()))) {
      MultiVectorHNSW index = (MultiVectorHNSW) ois.readObject();
      index.lock = new ReentrantReadWriteLock();
      log.info("Successfully loaded index with {} items.", index.size());
      return index;
    }
  }

  @Override
  public void add(long id, List<FloatVector> vectors) {
    lock.writeLock().lock();
    try {
      Node existingNode = nodes.get(id);
      if (existingNode != null && !existingNode.deleted) {
        throw new IllegalArgumentException(
            "Item with ID " + id + " already exists. Please remove it first to update.");
      }

      int level = assignLevel();
      log.debug("Adding item {} at level {}", id, level);
      Node newNode = new Node(id, level, m);
      nodes.put(id, newNode);
      vectorMap.put(id, vectors);

      Node currentEntryPoint = entryPoint;
      if (currentEntryPoint == null) {
        entryPoint = newNode;
        return;
      }

      int entryPointLevel = currentEntryPoint.level;
      Node nearestNode = currentEntryPoint;

      // Phase 1: Find the nearest neighbor in the upper layers
      for (int l = entryPointLevel; l > level; l--) {
        PriorityQueue<Neighbor> candidates = searchLayer(nearestNode, vectors, 1, l);
        if (candidates.isEmpty()) {
          break;
        }
        nearestNode = nodes.get(candidates.peek().id);
      }

      // Phase 2: Insert the new node by connecting it to its neighbors layer by layer
      for (int l = Math.min(level, entryPointLevel); l >= 0; l--) {
        PriorityQueue<Neighbor> candidates = searchLayer(nearestNode, vectors, efConstruction, l);
        if (candidates.isEmpty()) {
          break;
        }

        List<Neighbor> neighbors = selectNeighborsHeuristic(candidates, m);
        newNode.setConnections(l, neighbors.stream().map(n -> n.id).collect(Collectors.toList()));

        for (Neighbor neighbor : neighbors) {
          Node neighborNode = nodes.get(neighbor.id);
          if (neighborNode != null) {
            addConnection(neighborNode, id, neighbor.distance, l);
          }
        }
        assert candidates.peek() != null;
        nearestNode = nodes.get(candidates.peek().id);
      }

      if (level > entryPointLevel) {
        entryPoint = newNode;
        log.debug("New entry point: Node {} at level {}", newNode.id, level);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Adds a connection to a target node, ensuring the number of connections does not exceed M. If
   * the connections are full, it may replace the furthest neighbor.
   */
  private void addConnection(
      Node targetNode, long newConnectionId, double newConnectionDistance, int level) {
    List<Long> connections = targetNode.getConnections(level);

    if (connections.size() < m) {
      connections.add(newConnectionId);
      return;
    }

    long furthestNodeId = -1;
    double maxDist = -1.0;

    for (long currentConnectionId : connections) {
      double dist = distance(targetNode.id, currentConnectionId);
      if (dist > maxDist) {
        maxDist = dist;
        furthestNodeId = currentConnectionId;
      }
    }

    if (newConnectionDistance < maxDist) {
      connections.remove(furthestNodeId);
      connections.add(newConnectionId);
    }
  }

  /** A simple heuristic to select the best neighbors from a candidate set. */
  private List<Neighbor> selectNeighborsHeuristic(PriorityQueue<Neighbor> candidates, int count) {
    return candidates.stream().sorted().limit(count).collect(Collectors.toList());
  }

  @Override
  public boolean remove(long id) {
    lock.writeLock().lock();
    try {
      Node node = nodes.get(id);
      if (node == null || node.deleted) {
        return false;
      }
      node.deleted = true;
      log.debug("Marked item {} for deletion", id);
      return true;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void addAll(Map<Long, List<FloatVector>> items) {
    log.info("Adding {} items to the index.", items.size());
    items.forEach(this::add);
  }

  @Override
  public List<SearchResult> search(List<FloatVector> queryVectors, int k, int efSearch) {
    if (efSearch < k) {
      throw new IllegalArgumentException("efSearch must be greater than or equal to k");
    }

    lock.readLock().lock();
    try {
      Node currentEntryPoint = entryPoint;
      if (currentEntryPoint == null || vectorMap.isEmpty()) {
        return Collections.emptyList();
      }

      if (currentEntryPoint.deleted) {
        Optional<Node> newEntryPoint =
            nodes.values().stream().filter(node -> !node.deleted).findAny();
        if (newEntryPoint.isEmpty()) {
          return Collections.emptyList();
        }
        currentEntryPoint = newEntryPoint.get();
        log.debug(
            "Original entry point was deleted. Using temporary entry point: {}",
            currentEntryPoint.id);
      }

      Node nearestNode = currentEntryPoint;
      for (int l = currentEntryPoint.level; l > 0; l--) {
        PriorityQueue<Neighbor> candidates = searchLayer(nearestNode, queryVectors, 1, l);
        if (candidates.isEmpty()) {
          break;
        }
        Node bestCandidateNode = nodes.get(candidates.peek().id);
        if (bestCandidateNode != null && !bestCandidateNode.deleted) {
          nearestNode = bestCandidateNode;
        }
      }

      PriorityQueue<Neighbor> results = searchLayer(nearestNode, queryVectors, efSearch, 0);

      return results.stream()
          .sorted()
          .limit(k)
          .map(neighbor -> new SearchResult(neighbor.id, neighbor.distance))
          .collect(Collectors.toList());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Optional<List<FloatVector>> get(long id) {
    lock.readLock().lock();
    try {
      Node node = nodes.get(id);
      if (node != null && !node.deleted) {
        return Optional.ofNullable(vectorMap.get(id));
      }
      return Optional.empty();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int size() {
    lock.readLock().lock();
    try {
      return (int) nodes.values().stream().filter(n -> !n.deleted).count();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Set<Long> keySet() {
    lock.readLock().lock();
    try {
      return nodes.values().stream()
          .filter(node -> !node.deleted)
          .map(node -> node.id)
          .collect(Collectors.toSet());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public MultiVectorDistance getDistance() {
    return multiVectorDistance;
  }

  @Override
  public void save(Path path) throws IOException {
    log.info("Saving index with {} items to {}", size(), path);
    lock.readLock().lock();
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
      oos.writeObject(this);
      log.info("Save complete.");
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void clear() {
    lock.writeLock().lock();
    try {
      vectorMap.clear();
      nodes.clear();
      entryPoint = null;
      log.info("Index cleared.");
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void vacuum() {
    lock.writeLock().lock();
    try {
      if (entryPoint == null) {
        return;
      }
      Map<Long, List<FloatVector>> liveItems =
          nodes.values().stream()
              .filter(node -> !node.deleted)
              .map(node -> Map.entry(node.id, vectorMap.get(node.id)))
              .filter(entry -> entry.getValue() != null)
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      log.info("Starting vacuum. Rebuilding index with {} live items.", liveItems.size());
      clear();
      addAll(liveItems);
      log.info("Vacuum complete.");
    } finally {
      lock.writeLock().unlock();
    }
  }

  /** Performs a search for the nearest neighbors on a single layer of the graph. */
  private PriorityQueue<Neighbor> searchLayer(
      Node entry, List<FloatVector> query, int ef, int level) {
    PriorityQueue<Neighbor> results = new PriorityQueue<>(Collections.reverseOrder());
    PriorityQueue<Neighbor> candidates = new PriorityQueue<>();
    Set<Long> visited = new HashSet<>();

    if (entry == null || entry.deleted) {
      return results;
    }

    double entryDist = distance(query, entry.id);
    Neighbor entryNeighbor = new Neighbor(entry.id, entryDist);
    candidates.add(entryNeighbor);
    results.add(entryNeighbor);
    visited.add(entry.id);
    log.trace("L{}: Start search at {}, dist={}", level, entry.id, entryDist);

    while (!candidates.isEmpty()) {
      Neighbor candidate = candidates.poll();
      if (results.size() >= ef) {
        assert results.peek() != null;
        if (candidate.distance > results.peek().distance) {
          break;
        }
      }

      Node node = nodes.get(candidate.id);
      if (node == null || level > node.level) {
        continue;
      }

      for (long neighborId : node.getConnections(level)) {
        if (visited.add(neighborId)) {
          Node neighborNode = nodes.get(neighborId);
          if (neighborNode != null && !neighborNode.deleted) {
            double dist = distance(query, neighborId);
            log.trace(
                "L{}: Visiting neighbor {} of {}, dist={}", level, neighborId, candidate.id, dist);
            if (results.size() < ef || dist < Objects.requireNonNull(results.peek()).distance) {
              Neighbor newNeighbor = new Neighbor(neighborId, dist);
              candidates.add(newNeighbor);
              results.add(newNeighbor);
              if (results.size() > ef) {
                results.poll();
              }
            }
          }
        }
      }
    }
    return results;
  }

  /** Assigns a random level for a new node based on a logarithmic distribution. */
  private int assignLevel() {
    return (int) (-Math.log(ThreadLocalRandom.current().nextDouble()) * levelLambda);
  }

  /** Calculates the distance between a query vector list and the vectors of a stored node. */
  private double distance(List<FloatVector> vectors, long nodeId2) {
    List<FloatVector> vectors2 = vectorMap.get(nodeId2);
    if (vectors2 == null) {
      return Double.MAX_VALUE;
    }
    return multiVectorDistance.compute(vectors, vectors2);
  }

  /** Calculates the distance between two stored nodes. */
  private double distance(long nodeId1, long nodeId2) {
    List<FloatVector> vectors1 = vectorMap.get(nodeId1);
    List<FloatVector> vectors2 = vectorMap.get(nodeId2);
    if (vectors1 == null || vectors2 == null) {
      return Double.MAX_VALUE;
    }
    return multiVectorDistance.compute(vectors1, vectors2);
  }

  /** Custom deserialization method to re-initialize the transient lock. */
  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    this.lock = new ReentrantReadWriteLock();
  }

  /** A private record to represent a neighbor in the graph during a search. */
  private record Neighbor(long id, double distance) implements Comparable<Neighbor> {
    @Override
    public int compareTo(Neighbor other) {
      return Double.compare(this.distance, other.distance);
    }
  }

  /** A private class representing a node in the HNSW graph. */
  private static class Node implements Serializable {
    @Serial private static final long serialVersionUID = 2L;
    private final long id;
    private final int level;
    private final List<Long>[] connections;
    private volatile boolean deleted = false;

    @SuppressWarnings("unchecked")
    Node(long id, int level, int m) {
      this.id = id;
      this.level = level;
      this.connections = (List<Long>[]) new ArrayList[level + 1];
      for (int i = 0; i <= level; i++) {
        connections[i] = new ArrayList<>(m);
      }
    }

    List<Long> getConnections(int level) {
      return connections[level];
    }

    void setConnections(int level, List<Long> newConnections) {
      connections[level].clear();
      connections[level].addAll(newConnections);
    }
  }

  /**
   * A builder for configuring and creating a {@link MultiVectorHNSW} index. This provides a fluent
   * API for setting parameters.
   */
  public static class Builder {
    private MultiVectorDistance multiVectorDistance;
    private int m = 16;
    private int efConstruction = 200;

    /**
     * Sets the maximum number of connections per node per layer (M).
     *
     * @param m A positive integer, typically between 5 and 48.
     * @return This builder instance.
     */
    public Builder withM(int m) {
      if (m <= 0) {
        throw new IllegalArgumentException("M must be positive.");
      }
      this.m = m;
      return this;
    }

    /**
     * Sets the size of the dynamic list for neighbors during index construction (efConstruction).
     *
     * @param efConstruction A positive integer, typically between 64 and 512.
     * @return This builder instance.
     */
    public Builder withEfConstruction(int efConstruction) {
      if (efConstruction <= 0) {
        throw new IllegalArgumentException("efConstruction must be positive.");
      }
      this.efConstruction = efConstruction;
      return this;
    }

    /**
     * Sets a custom distance function that implements {@link MultiVectorDistance}.
     *
     * @param distance The distance function to use.
     * @return This builder instance.
     */
    public Builder withDistance(MultiVectorDistance distance) {
      this.multiVectorDistance = distance;
      return this;
    }

    /**
     * Returns a specialized builder for creating a {@link WeightedAverageDistance}.
     *
     * @return A new {@link WeightedAverageDistanceBuilder} instance.
     */
    public WeightedAverageDistanceBuilder withWeightedAverageDistance() {
      return new WeightedAverageDistanceBuilder(this);
    }

    /**
     * Builds the {@link MultiVectorHNSW} index with the configured parameters.
     *
     * @return A new MultiVectorHNSW instance.
     * @throws NullPointerException if a distance function has not been configured.
     */
    public MultiVectorHNSW build() {
      Objects.requireNonNull(multiVectorDistance, "A distance function must be configured.");
      return new MultiVectorHNSW(this);
    }

    /**
     * A specialized builder for configuring a {@link WeightedAverageDistance} as part of the main
     * builder chain.
     */
    public static class WeightedAverageDistanceBuilder {
      private final Builder parentBuilder;
      private final List<Distance<FloatVector>> distances = new ArrayList<>();
      private final List<Float> weights = new ArrayList<>();

      WeightedAverageDistanceBuilder(Builder parentBuilder) {
        this.parentBuilder = parentBuilder;
      }

      /**
       * Adds a distance function and its corresponding weight.
       *
       * @param distance The {@link Distance} function for a specific vector in the list.
       * @param weight The weight to assign to this distance's score.
       * @return This builder instance.
       */
      public WeightedAverageDistanceBuilder addDistance(
          Distance<FloatVector> distance, float weight) {
        this.distances.add(distance);
        this.weights.add(weight);
        return this;
      }

      /**
       * Conditionally adds a distance function and its corresponding weight.
       *
       * @param condition If true, the distance and weight are added.
       * @param distance The {@link Distance} function.
       * @param weight The weight for the distance.
       * @return This builder instance.
       */
      public WeightedAverageDistanceBuilder addDistanceIf(
          boolean condition, Distance<FloatVector> distance, float weight) {
        if (condition) {
          this.distances.add(distance);
          this.weights.add(weight);
        }
        return this;
      }

      /**
       * Finalizes the weighted distance configuration and returns to the parent {@link Builder}.
       *
       * @return The parent Builder instance.
       */
      public Builder and() {
        float[] weightsArray = new float[weights.size()];
        for (int i = 0; i < weights.size(); i++) {
          weightsArray[i] = weights.get(i);
        }
        this.parentBuilder.multiVectorDistance =
            new WeightedAverageDistance(distances, weightsArray);
        return this.parentBuilder;
      }
    }
  }
}
