package io.github.habedi.mvhnsw.index;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.MultiVectorDistance;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Index {

    void add(long id, List<FloatVector> vectors);

    void update(long id, List<FloatVector> vectors);

    boolean remove(long id);

    void addAll(Map<Long, List<FloatVector>> items);

    void updateAll(Map<Long, List<FloatVector>> items);

    int removeAll(Collection<Long> ids);

    List<SearchResult> search(List<FloatVector> queryVectors, int k);

    Optional<List<FloatVector>> get(long id);

    int size();

    MultiVectorDistance getDistance();

    void save(Path path) throws IOException;

    void clear();

    void vacuum();
}
