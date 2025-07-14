// Module declaration for the Multi-Vector HNSW library
module io.github.habedi.mvhnsw {

  // Core dependencies
  requires org.apache.logging.log4j;
  requires jdk.incubator.vector;

  // Public API exports
  exports io.github.habedi.mvhnsw.common;
  exports io.github.habedi.mvhnsw.distance;
  exports io.github.habedi.mvhnsw.index;
}
