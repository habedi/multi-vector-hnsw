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

public final class MultiVectorHNSW implements Index, Serializable {

  @Serial private static final long serialVersionUID = 1L;
  private static final Logger log = LogManager.getLogger(MultiVectorHNSW.class);

  private final MultiVectorDistance multiVectorDistance;
  private final int m;
  private final int efConstruction;
  private final double levelLambda;
  private final Map<Long, List<FloatVector>> vectorMap;
  private final Map<Long, Node> nodes;
  private transient ReentrantReadWriteLock lock;

  private volatile Node entryPoint;

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

  public static Builder builder() {
    return new Builder();
  }

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
      Node newNode = new Node(id, level);
      nodes.put(id, newNode);
      vectorMap.put(id, vectors);

      Node currentEntryPoint = entryPoint;
      if (currentEntryPoint == null) {
        entryPoint = newNode;
        return;
      }

      Node nearestNode = findEntryPointForLevel(currentEntryPoint, vectors, level);

      for (int l = Math.min(level, getLevel(nearestNode)); l >= 0; l--) {
        PriorityQueue<Neighbor> neighbors = searchLayer(nearestNode, vectors, efConstruction, l);
        List<Long> newConnections = selectNeighbors(neighbors, m);
        newNode.connections.get(l).addAll(newConnections);

        for (long neighborId : newConnections) {
          Node neighborNode = nodes.get(neighborId);
          if (neighborNode != null) {
            List<Long> neighborConnections = neighborNode.connections.get(l);
            neighborConnections.add(id);
            if (neighborConnections.size() > m) {
              pruneConnections(neighborNode, l);
            }
          }
        }
      }

      if (level > getLevel(currentEntryPoint)) {
        entryPoint = newNode;
        log.debug("New entry point: Node {} at level {}", newNode.id, level);
      }
    } finally {
      lock.writeLock().unlock();
    }
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
  public List<SearchResult> search(List<FloatVector> queryVectors, int k) {
    lock.readLock().lock();
    try {
      Node currentEntryPoint = entryPoint;
      if (currentEntryPoint == null || vectorMap.isEmpty()) {
        return Collections.emptyList();
      }

      Node nearestNode = findEntryPointForLevel(currentEntryPoint, queryVectors, 0);

      PriorityQueue<Neighbor> results =
          searchLayer(nearestNode, queryVectors, Math.max(k, efConstruction), 0);

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

  private Node findEntryPointForLevel(Node entry, List<FloatVector> query, int targetLevel) {
    Node nearestNode = entry;
    for (int l = getLevel(entry); l > targetLevel; l--) {
      PriorityQueue<Neighbor> candidates = searchLayer(nearestNode, query, 1, l);
      if (candidates.isEmpty()) {
        break;
      }
      nearestNode = nodes.get(candidates.peek().id);
    }
    return nearestNode;
  }

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
      if (results.size() >= ef && candidate.distance > results.peek().distance) {
        break;
      }

      Node node = nodes.get(candidate.id);
      if (node == null || !node.connections.containsKey(level)) {
        continue;
      }

      for (long neighborId : node.connections.get(level)) {
        if (visited.add(neighborId)) {
          Node neighborNode = nodes.get(neighborId);
          if (neighborNode != null && !neighborNode.deleted) {
            double dist = distance(query, neighborId);
            log.trace(
                "L{}: Visiting neighbor {} of {}, dist={}", level, neighborId, candidate.id, dist);
            if (results.size() < ef || dist < results.peek().distance) {
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

  private int assignLevel() {
    return (int) (-Math.log(ThreadLocalRandom.current().nextDouble()) * levelLambda);
  }

  private int getLevel(Node node) {
    return Collections.max(node.connections.keySet());
  }

  private double distance(List<FloatVector> vectors, long nodeId2) {
    List<FloatVector> vectors2 = vectorMap.get(nodeId2);
    if (vectors2 == null) {
      return Double.MAX_VALUE;
    }
    return multiVectorDistance.compute(vectors, vectors2);
  }

  private double distance(long nodeId1, long nodeId2) {
    List<FloatVector> vectors1 = vectorMap.get(nodeId1);
    List<FloatVector> vectors2 = vectorMap.get(nodeId2);
    if (vectors1 == null || vectors2 == null) {
      return Double.MAX_VALUE;
    }
    return multiVectorDistance.compute(vectors1, vectors2);
  }

  private List<Long> selectNeighbors(PriorityQueue<Neighbor> candidates, int count) {
    return candidates.stream()
        .sorted()
        .limit(count)
        .map(n -> n.id)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private void pruneConnections(Node node, int level) {
    List<Long> connections = node.connections.get(level);
    if (connections.size() <= m) {
      return;
    }
    log.debug(
        "Pruning connections for node {} at level {}. From {} to {}",
        node.id,
        level,
        connections.size(),
        m);
    PriorityQueue<Neighbor> neighbors = new PriorityQueue<>(connections.size());
    for (long neighborId : connections) {
      neighbors.add(new Neighbor(neighborId, distance(node.id, neighborId)));
    }

    List<Long> newConnections = new ArrayList<>(m);
    while (newConnections.size() < m && !neighbors.isEmpty()) {
      newConnections.add(neighbors.poll().id);
    }
    node.connections.put(level, newConnections);
  }

  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    this.lock = new ReentrantReadWriteLock();
  }

  private record Neighbor(long id, double distance) implements Comparable<Neighbor> {
    @Override
    public int compareTo(Neighbor other) {
      return Double.compare(this.distance, other.distance);
    }
  }

  private static class Node implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private final long id;
    private final Map<Integer, List<Long>> connections;
    private volatile boolean deleted = false;

    Node(long id, int level) {
      this.id = id;
      this.connections = new ConcurrentHashMap<>(level + 1);
      for (int i = 0; i <= level; i++) {
        connections.put(i, new ArrayList<>());
      }
    }
  }

  public static class Builder {
    private MultiVectorDistance multiVectorDistance;
    private int m = 16;
    private int efConstruction = 200;

    public Builder withM(int m) {
      if (m <= 0) {
        throw new IllegalArgumentException("M must be positive.");
      }
      this.m = m;
      return this;
    }

    public Builder withEfConstruction(int efConstruction) {
      if (efConstruction <= 0) {
        throw new IllegalArgumentException("efConstruction must be positive.");
      }
      this.efConstruction = efConstruction;
      return this;
    }

    public Builder withDistance(MultiVectorDistance distance) {
      this.multiVectorDistance = distance;
      return this;
    }

    public WeightedAverageDistanceBuilder withWeightedAverageDistance() {
      return new WeightedAverageDistanceBuilder(this);
    }

    public MultiVectorHNSW build() {
      Objects.requireNonNull(multiVectorDistance, "A distance function must be configured.");
      return new MultiVectorHNSW(this);
    }

    public static class WeightedAverageDistanceBuilder {
      private final Builder parentBuilder;
      private final List<Distance<FloatVector>> distances = new ArrayList<>();
      private final List<Float> weights = new ArrayList<>();

      WeightedAverageDistanceBuilder(Builder parentBuilder) {
        this.parentBuilder = parentBuilder;
      }

      public WeightedAverageDistanceBuilder addDistance(
          Distance<FloatVector> distance, float weight) {
        this.distances.add(distance);
        this.weights.add(weight);
        return this;
      }

      public WeightedAverageDistanceBuilder addDistanceIf(
          boolean condition, Distance<FloatVector> distance, float weight) {
        if (condition) {
          this.distances.add(distance);
          this.weights.add(weight);
        }
        return this;
      }

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
