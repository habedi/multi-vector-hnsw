module io.github.habedi.mvhnsw {
  // Core dependencies
  requires org.apache.logging.log4j;
  requires jdk.incubator.vector;

  // Exports for your library's public API
  exports io.github.habedi.mvhnsw.common;
  exports io.github.habedi.mvhnsw.distance;
  exports io.github.habedi.mvhnsw.index;
}
