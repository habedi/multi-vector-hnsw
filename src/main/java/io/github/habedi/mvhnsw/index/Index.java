package io.github.habedi.mvhnsw.index;

import io.github.habedi.mvhnsw.common.FloatVector;
import io.github.habedi.mvhnsw.distance.MultiVectorDistance;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Index {

  void add(long id, List<FloatVector> vectors);

  boolean remove(long id);

  void addAll(Map<Long, List<FloatVector>> items);

  List<SearchResult> search(List<FloatVector> queryVectors, int k);

  Optional<List<FloatVector>> get(long id);

  int size();

  Set<Long> keySet();

  MultiVectorDistance getDistance();

  void save(Path path) throws IOException;

  void clear();

  /**
   * Rebuilds the index to permanently remove items that were marked for deletion via the {@link
   * #remove(long)} method. This operation can be expensive and should be called periodically after
   * a large number of removals to reclaim memory and maintain performance.
   */
  void vacuum();
}
