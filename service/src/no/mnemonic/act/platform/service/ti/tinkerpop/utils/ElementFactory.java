package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import com.google.common.cache.*;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.tinkerpop.ActGraph;
import no.mnemonic.act.platform.service.ti.tinkerpop.FactEdge;
import no.mnemonic.act.platform.service.ti.tinkerpop.ObjectVertex;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Helper class for creation and retrieval of edges and vertices which implements simple caching.
 */
public class ElementFactory {

  private static final int CACHE_MAXIMUM_SIZE = 100_000;
  private static final Logger LOGGER = Logging.getLogger(ElementFactory.class);

  private final ActGraph owner;

  // Maps the triplet (factID, inVertex, outVertex) to UUID returned by Edge.id().
  // Needed in order to identify entry in 'edgeCache'.
  private final Map<EdgeID, UUID> edgeIdMap;
  // Cache for created edges. This cache is manually populated by createEdges().
  private final Cache<UUID, Edge> edgeCache;
  // Cache for created vertices. This cache is automatically populated.
  private final LoadingCache<UUID, Vertex> vertexCache;

  private boolean evictionLogged = false;

  private ElementFactory(ActGraph owner) {
    this.owner = ObjectUtils.notNull(owner, "'owner is null!'");
    this.edgeIdMap = new ConcurrentHashMap<>();
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
   * Creates an Edge based on a FactRecord. The result is cached. The sourceId and direction is needed
   * in order to represent bidirectional facts as edges. A bidirectional factRecord is either
   * an incoming or outgoing edge based on the vertex you are currently on when traversing (sourceId)
   *
   * @param factRecord A factRecord
   * @param sourceId   The vertex from which you are traversing
   * @return An Edge
   */
  public Edge createEdge(FactRecord factRecord, UUID sourceId) {
    if (factRecord == null || factRecord.getSourceObject() == null || factRecord.getDestinationObject() == null) {
      return null;
    }

    // We need to figure out which vertexes should be source/destination
    boolean shouldFlip = shouldFlipSourceAndDestination(factRecord, sourceId);
    UUID inVertexId = shouldFlip ? factRecord.getDestinationObject().getId() : factRecord.getSourceObject().getId();
    UUID outVertexId = shouldFlip ? factRecord.getSourceObject().getId() : factRecord.getDestinationObject().getId();

    return createAndCache(factRecord, inVertexId, outVertexId);
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
      LOGGER.warning(ignored, "Failed to get vertex with id = %s.", id);
      return null;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Given a source from which you are traversing, decide if the vertexes should
   * be flipped or not when converting a FactRecord to an Edge
   * <p>
   * Edges must be flipped for bidirectional facts if the sourceId is not the same as the factRecord source.
   *
   * @param factRecord The factrecord
   * @param sourceId   Identifies the vertex from which one is traversing
   * @return true if the source and destination should be flipped
   */
  private boolean shouldFlipSourceAndDestination(FactRecord factRecord, UUID sourceId) {
    if (!factRecord.isBidirectionalBinding()) return false;

    boolean sameSource = Objects.equals(factRecord.getSourceObject().getId(), sourceId);
    return !sameSource;
  }

  private Edge createAndCache(FactRecord factRecord, UUID inVertexId, UUID outVertexId) {
    // Try to fetch edge from cache first (but only if 'edgeID' is mapped, otherwise edge is not cached).
    EdgeID edgeID = new EdgeID(factRecord.getId(), inVertexId, outVertexId);

    Edge edge = ObjectUtils.ifNotNull(edgeIdMap.get(edgeID), edgeCache::getIfPresent);

    if (edge == null) {
      // Edge is not present in cache, create new instance and cache it for later access.
      edge = fetchEdge(factRecord, inVertexId, outVertexId);
      edgeIdMap.put(edgeID, (UUID) edge.id());
      edgeCache.put((UUID) edge.id(), edge);
    }

    return edge;
  }

  private Edge fetchEdge(FactRecord factRecord, UUID inVertexId, UUID outVertexId) {
    FactTypeStruct factTypeStruct = this.owner
            .getObjectFactTypeResolver()
            .toFactTypeStruct(factRecord.getTypeID());
    Vertex inVertex = getVertex(inVertexId);
    Vertex outVertex = getVertex(outVertexId);

    return FactEdge.builder()
            .setGraph(owner)
            .setFactRecord(factRecord)
            .setFactType(factTypeStruct)
            .setInVertex(inVertex)
            .setOutVertex(outVertex)
            .build();
  }


  private Cache<UUID, Edge> createEdgeCache() {
    return CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAXIMUM_SIZE)
            .removalListener(this::cleanUpEdgeCache)
            .build();
  }

  private LoadingCache<UUID, Vertex> createVertexCache() {
    return CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAXIMUM_SIZE)
            .removalListener(this::logEviction)
            .build(new CacheLoader<UUID, Vertex>() {
              @Override
              public Vertex load(UUID key) {
                ObjectRecord objectRecord = ObjectUtils.notNull(
                        owner.getObjectFactDao().getObject(key),
                        String.format("Object with id = %s does not exist.", key));
                ObjectTypeStruct objectTypeStruct = ObjectUtils.notNull(
                        owner.getObjectFactTypeResolver().toObjectTypeStruct(objectRecord.getTypeID()),
                        String.format("ObjectType with id = %s does not exist.", objectRecord.getTypeID()));

                return ObjectVertex.builder()
                        .setGraph(owner)
                        .setObjectRecord(objectRecord)
                        .setObjectType(objectTypeStruct)
                        .build();
              }
            });
  }

  private void cleanUpEdgeCache(RemovalNotification<?, ?> notification) {
    logEviction(notification);

    // Need to clean up 'edgeIdMap' when an entry gets evicted.
    if (notification.wasEvicted()) {
      SetUtils.set(edgeIdMap.entrySet())
              .stream()
              .filter(entry -> Objects.equals(entry.getValue(), notification.getKey()))
              .forEach(entry -> edgeIdMap.remove(entry.getKey()));
    }
  }

  private void logEviction(RemovalNotification<?, ?> notification) {
    if (notification.wasEvicted() && !evictionLogged) {
      LOGGER.warning("ElementFactory started to evict entries from its caches. This causes a performance decrease.");
      evictionLogged = true;
    }
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

  /**
   * Required since an edge in the datamodel can be bidirectional. EdgeID makes it possible for a single
   * bidirectional edge to be used by Tinkerpop in both directions.
   */
  private static class EdgeID {
    private final UUID factID;
    private final UUID inVertex;
    private final UUID outVertex;

    private EdgeID(UUID factID, UUID inVertex, UUID outVertex) {
      this.factID = factID;
      this.inVertex = inVertex;
      this.outVertex = outVertex;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      EdgeID that = (EdgeID) o;
      return Objects.equals(factID, that.factID) &&
              Objects.equals(inVertex, that.inVertex) &&
              Objects.equals(outVertex, that.outVertex);
    }

    @Override
    public int hashCode() {
      return Objects.hash(factID, inVertex, outVertex);
    }
  }
}
