package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.tinkerpop.ActGraph;
import no.mnemonic.act.platform.service.ti.tinkerpop.FactEdge;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.ObjectVertex;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.UUID;

/**
 * Helper class for creation and retrieval of edges and vertices which implements simple caching.
 */
public class ElementFactory {

  private static final int CACHE_MAXIMUM_SIZE = 10000;
  private static final Logger LOGGER = Logging.getLogger(ElementFactory.class);

  private final ActGraph owner;
  // Cache for created edges. This cache is manually populated by createEdges().
  private final Cache<UUID, Edge> edgeCache;
  // Cache for created vertices. This cache is automatically populated.
  private final LoadingCache<UUID, Vertex> vertexCache;

  private ElementFactory(ActGraph owner) {
    this.owner = ObjectUtils.notNull(owner, "'owner is null!'");
    this.edgeCache = createEdgeCache();
    this.vertexCache = createVertexCache();
  }

  /**
   * Retrieve an edge from the cache by its ID.
   *
   * @param id ID of edge, i.e. {@link Edge#id()}.
   * @return Cached edge or NULL.
   */
  public Edge getEdge(UUID id) {
    if (id == null) return null;
    return edgeCache.getIfPresent(id);
  }

  /**
   * Convert FactRecord to Edge
   * <p>
   * Edges are cached. New edges will be created and cached if it is not
   * found in the cache.
   *
   * Returns null if the conversion failed.
   *
   * @param factRecord FactRecord to convert
   * @return Edge      The edge, or null if the record is invalid.
   */
  public Edge createEdge(FactRecord factRecord) {
    if (factRecord == null) return null;

    try {
      return edgeCache.get(factRecord.getId(), () -> fetchEdge(factRecord));
    } catch (Exception e) {
      // Ignore the exception and just return null
      LOGGER.warning(e, "Failed to create edge for FactRecord with id = %s.", factRecord.getId());
      return null;
    }
  }

  /**
   * Retrieve a vertex from the cache by its ID.
   * <p>
   * This will automatically create the vertex if it was not already cached.
   *
   * @param id ID of vertex, i.e. {@link Vertex#id()}.
   * @return Cached vertex.
   */
  public Vertex getVertex(UUID id) {
    if (id == null) return null;
    try {
      return vertexCache.get(id);
    } catch (Exception ignored) {
      // If vertex cannot be fetched, e.g. because 'id' references a non-existing Object, just return null.
      return null;
    }
  }

  private Edge fetchEdge(FactRecord factRecord) {
    FactTypeStruct factTypeStruct = this.owner
            .getObjectFactTypeResolver()
            .toFactTypeStruct(factRecord.getTypeID());
    Vertex inVertex = getVertex(factRecord.getSourceObject().getId());
    Vertex outVertex = getVertex(factRecord.getDestinationObject().getId());

    return new FactEdge(owner, factRecord, factTypeStruct, inVertex, outVertex);
  }

  public static Builder builder() {
    return new Builder();
  }

  private Cache<UUID, Edge> createEdgeCache() {
    return CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAXIMUM_SIZE)
            .build();
  }

  private LoadingCache<UUID, Vertex> createVertexCache() {
    return CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAXIMUM_SIZE)
            .build(new CacheLoader<UUID, Vertex>() {
              @Override
              public Vertex load(UUID key) {
                ObjectRecord objectRecord = ObjectUtils.notNull(
                        owner.getObjectFactDao().getObject(key),
                        String.format("Object with id = %s does not exist.", key));
                ObjectTypeStruct objectTypeStruct = ObjectUtils.notNull(
                        owner.getObjectFactTypeResolver().toObjectTypeStruct(objectRecord.getTypeID()),
                        String.format("ObjectType with id = %s does not exist.", objectRecord.getTypeID()));
                return new ObjectVertex(owner, objectRecord, objectTypeStruct);
              }
            });
  }

  public static class Builder {
    private ActGraph owner;

    private Builder() {
    }

    public ElementFactory build() {
      return new ElementFactory(owner);
    }

    public Builder setOwner(ActGraph owner) {
      this.owner = owner;
      return this;
    }
  }
}
