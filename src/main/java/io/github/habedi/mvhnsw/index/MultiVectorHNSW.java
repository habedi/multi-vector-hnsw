package io.github.habedi.mvhnsw.index;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.MultiVectorDistance;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * A thread-safe, serializable implementation of the Hierarchical Navigable Small World (HNSW) graph
 * for approximate nearest neighbor search with multi-vector support.
 *
 * @param <P> The type of the payload associated with each item.
 */
public final class MultiVectorHNSW<P> implements Index<FloatVector, P>, Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final MultiVectorDistance<FloatVector> multiVectorDistance;
    private final int m;
    private final int efConstruction;
    private final double levelLambda;
    private final Map<Long, Item<FloatVector, P>> itemMap;
    private final Map<Long, Node> nodes;
    private transient ReentrantReadWriteLock lock;

    private volatile Node entryPoint;

    private MultiVectorHNSW(Builder<P> builder) {
        this.multiVectorDistance = builder.multiVectorDistance;
        this.m = builder.m;
        this.efConstruction = builder.efConstruction;
        this.levelLambda = 1 / Math.log(m);
        this.itemMap = new ConcurrentHashMap<>();
        this.nodes = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.entryPoint = null;
    }

    @SuppressWarnings("unchecked")
    public static <P> MultiVectorHNSW<P> load(Path path)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path.toFile()))) {
            MultiVectorHNSW<P> index = (MultiVectorHNSW<P>) ois.readObject();
            index.lock = new ReentrantReadWriteLock(); // Re-initialize transient lock
            return index;
        }
    }

    private static <P> Item<FloatVector, P> createQueryItem(List<FloatVector> queryVectors) {
        return new Item<>() {
            @Override
            public long getId() {
                return -1;
            }

            @Override
            public List<FloatVector> getVectors() {
                return queryVectors;
            }

            @Override
            public P getPayload() {
                return null;
            }
        };
    }

    @Override
    public void add(Item<FloatVector, P> item) {
        lock.writeLock().lock();
        try {
            if (itemMap.containsKey(item.getId())) {
                // Use update for existing items to maintain consistency
                update(item);
                return;
            }

            int level = assignLevel();
            Node newNode = new Node(item.getId(), level);
            nodes.put(newNode.id, newNode);
            itemMap.put(item.getId(), item);

            Node currentEntryPoint = entryPoint;
            if (currentEntryPoint == null) {
                entryPoint = newNode;
                return;
            }

            Node nearestNode = findEntryPointForLevel(currentEntryPoint, item, level);

            for (int l = Math.min(level, getLevel(nearestNode)); l >= 0; l--) {
                PriorityQueue<Neighbor> neighbors =
                        searchLayer(nearestNode, item, efConstruction, l);
                List<Long> newConnections = selectNeighbors(neighbors, m);
                newNode.connections.get(l).addAll(newConnections);

                for (long neighborId : newConnections) {
                    Node neighborNode = nodes.get(neighborId);
                    if (neighborNode != null) {
                        List<Long> neighborConnections = neighborNode.connections.get(l);
                        neighborConnections.add(newNode.id);
                        if (neighborConnections.size() > m) {
                            pruneConnections(neighborNode, l);
                        }
                    }
                }
            }

            if (level > getLevel(currentEntryPoint)) {
                entryPoint = newNode;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Item<FloatVector, P> item) {
        lock.writeLock().lock();
        try {
            if (!itemMap.containsKey(item.getId())) {
                throw new NoSuchElementException(
                        "Item with ID " + item.getId() + " not found for update.");
            }
            // Simple and safe implementation: remove the old item and add the new one.
            // This creates a soft-deleted node that can be cleaned up by vacuum().
            remove(item.getId());
            add(item);
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
            node.deleted = true; // Soft delete
            return true; // Item remains in itemMap until vacuumed to allow in-flight searches
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addAll(Collection<Item<FloatVector, P>> items) {
        for (Item<FloatVector, P> item : items) {
            add(item);
        }
    }

    @Override
    public void updateAll(Collection<Item<FloatVector, P>> items) {
        for (Item<FloatVector, P> item : items) {
            update(item);
        }
    }

    @Override
    public int removeAll(Collection<Long> ids) {
        int count = 0;
        for (long id : ids) {
            if (remove(id)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<SearchResult<FloatVector, P>> search(List<FloatVector> queryVectors, int k) {
        lock.readLock().lock();
        try {
            Node currentEntryPoint = entryPoint;
            if (currentEntryPoint == null || itemMap.isEmpty()) {
                return Collections.emptyList();
            }

            Item<FloatVector, P> queryItem = createQueryItem(queryVectors);
            Node nearestNode = findEntryPointForLevel(currentEntryPoint, queryItem, 0);

            PriorityQueue<Neighbor> results =
                    searchLayer(nearestNode, queryItem, Math.max(k, efConstruction), 0);

            return results.stream()
                    .sorted()
                    .limit(k)
                    .map(
                            neighbor ->
                                    new SearchResult<>(itemMap.get(neighbor.id), neighbor.distance))
                    .filter(sr -> sr.item() != null)
                    .collect(Collectors.toList());

        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Item<FloatVector, P>> get(long id) {
        lock.readLock().lock(); // FIX: Use read lock for consistent reads
        try {
            Node node = nodes.get(id);
            if (node != null && !node.deleted) {
                return Optional.ofNullable(itemMap.get(id));
            }
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock(); // FIX: Use read lock for consistent reads
        try {
            // Count only non-deleted nodes for an accurate size.
            return (int) nodes.values().stream().filter(n -> !n.deleted).count();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public MultiVectorDistance<FloatVector> getDistance() {
        return multiVectorDistance;
    }

    @Override
    public void save(Path path) throws IOException {
        lock.readLock().lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            oos.writeObject(this);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            itemMap.clear();
            nodes.clear();
            entryPoint = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void vacuum() {
        lock.writeLock().lock();
        try {
            if (entryPoint == null) {
                return; // Nothing to do
            }
            // Get a list of all items that are not soft-deleted
            List<Item<FloatVector, P>> liveItems =
                    nodes.values().stream()
                            .filter(node -> !node.deleted)
                            .map(node -> itemMap.get(node.id))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            // Clear the index completely
            clear();

            // Re-add all live items. This rebuilds the graph from scratch.
            addAll(liveItems);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Node findEntryPointForLevel(Node entry, Item<FloatVector, P> query, int targetLevel) {
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
            Node entry, Item<FloatVector, P> query, int ef, int level) {
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

        while (!candidates.isEmpty()) {
            Neighbor candidate = candidates.poll();
            if (results.size() >= ef) {
                assert results.peek() != null;
                if (candidate.distance > results.peek().distance) {
                    break;
                }
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
                        if (results.size() < ef
                                || dist < Objects.requireNonNull(results.peek()).distance) {
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

    private double distance(Item<FloatVector, P> item1, long nodeId2) {
        Item<FloatVector, P> item2 = itemMap.get(nodeId2);
        if (item1 == null || item2 == null) return Double.MAX_VALUE;
        return multiVectorDistance.compute(item1, item2);
    }

    private double distance(long nodeId1, long nodeId2) {
        Item<FloatVector, P> item1 = itemMap.get(nodeId1);
        Item<FloatVector, P> item2 = itemMap.get(nodeId2);
        if (item1 == null || item2 == null) return Double.MAX_VALUE;
        return multiVectorDistance.compute(item1, item2);
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
                connections.put(i, Collections.synchronizedList(new ArrayList<>()));
            }
        }
    }

    public static class Builder<P> {
        private final MultiVectorDistance<FloatVector> multiVectorDistance;
        private int m = 16;
        private int efConstruction = 200;

        public Builder(MultiVectorDistance<FloatVector> distance) {
            this.multiVectorDistance =
                    Objects.requireNonNull(distance, "MultiVectorDistance cannot be null.");
        }

        public Builder<P> withM(int m) {
            if (m <= 0) {
                throw new IllegalArgumentException("M must be positive.");
            }
            this.m = m;
            return this;
        }

        public Builder<P> withEfConstruction(int efConstruction) {
            if (efConstruction <= 0) {
                throw new IllegalArgumentException("efConstruction must be positive.");
            }
            this.efConstruction = efConstruction;
            return this;
        }

        public MultiVectorHNSW<P> build() {
            return new MultiVectorHNSW<>(this);
        }
    }
}
