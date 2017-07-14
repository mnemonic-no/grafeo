package no.mnemonic.act.platform.dao.tinkerpop.utils;

import com.google.common.cache.*;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.tinkerpop.ActGraph;
import no.mnemonic.act.platform.dao.tinkerpop.FactEdge;
import no.mnemonic.act.platform.dao.tinkerpop.ObjectVertex;
import no.mnemonic.act.platform.entity.cassandra.Direction;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for creation and retrieval of edges and vertices which implements simple caching.
 */
public class ElementFactory {

  private static final int CACHE_MAXIMUM_SIZE = 10000;

  private final ActGraph owner;
  private final FactManager factManager;

  // Maps the triplet (factID, inVertex, outVertex) to UUID returned by Edge.id().
  // Needed in order to identify entry in 'edgeCache'.
  private final Map<EdgeID, UUID> edgeIdMap;
  // Cache for created edges. This cache is manually populated by createEdges().
  private final Cache<UUID, Edge> edgeCache;
  // Cache for created vertices. This cache is automatically populated.
  private final LoadingCache<UUID, Vertex> vertexCache;

  private ElementFactory(ActGraph owner, FactManager factManager) {
    this.owner = ObjectUtils.notNull(owner, "'owner is null!'");
    this.factManager = ObjectUtils.notNull(factManager, "'factManager' is null!");
    this.edgeIdMap = new ConcurrentHashMap<>();
    this.edgeCache = createEdgeCache();
    this.vertexCache = createVertexCache();
  }

  /**
   * Create edges based on a binding between an Object and a Fact.
   * <p>
   * It will fetch the Fact and create edges between the Object and other Objects bound to the Fact. If only the given
   * Object is bound to the Fact a loop edge is created. If the Fact is bound to multiple Objects an edge for each
   * binding is created where applicable (taking the binding directions into account).
   * <p>
   * Created edges are cached for later retrieval by {@link ElementFactory#getEdge(UUID)}.
   *
   * @param inBinding Binding between an Object and a Fact (incoming vertex).
   * @return Created edges.
   */
  public Set<Edge> createEdges(ObjectFactBindingEntity inBinding) {
    ObjectUtils.notNull(inBinding, "'inBinding' is null!");

    FactEntity fact = factManager.getFact(inBinding.getFactID());
    if (fact == null) {
      return new HashSet<>();
    }

    // If the Fact is only bound to the 'inBinding' Object then this needs to be represented as a loop in the graph.
    if (CollectionUtils.size(fact.getBindings()) == 1 && Objects.equals(fact.getBindings().get(0).getObjectID(), inBinding.getObjectID())) {
      return SetUtils.set(createAndCache(inBinding.getFactID(), inBinding.getObjectID(), inBinding.getObjectID()));
    }

    Set<Edge> edges = new HashSet<>();
    for (FactEntity.FactObjectBinding outBinding : ListUtils.list(fact.getBindings())) {
      // Skip bindings to 'inBinding' Object.
      if (Objects.equals(outBinding.getObjectID(), inBinding.getObjectID())) continue;

      // For all other bindings create an edge where the objectID of the binding is the outgoing vertex.
      // But only if the directions fit together!
      if ((inBinding.getDirection() == Direction.None && outBinding.getDirection() == Direction.None) ||
              (inBinding.getDirection() == Direction.BiDirectional && outBinding.getDirection() == Direction.BiDirectional) ||
              (inBinding.getDirection() == Direction.FactIsDestination && outBinding.getDirection() == Direction.FactIsSource)) {
        edges.add(createAndCache(inBinding.getFactID(), inBinding.getObjectID(), outBinding.getObjectID()));
      }

      // In this case need to swap 'inBinding' and 'outBinding' in order to have the correct edge direction.
      if (inBinding.getDirection() == Direction.FactIsSource && outBinding.getDirection() == Direction.FactIsDestination) {
        edges.add(createAndCache(inBinding.getFactID(), outBinding.getObjectID(), inBinding.getObjectID()));
      }
    }

    return edges;
  }

  /**
   * Retrieve an edge from the cache by its ID.
   * <p>
   * This will return NULL if the edge was not created and cached by {@link ElementFactory#createEdges(ObjectFactBindingEntity)} before.
   *
   * @param id ID of edge, i.e. {@link Edge#id()}.
   * @return Cached edge or NULL.
   */
  public Edge getEdge(UUID id) {
    return edgeCache.getIfPresent(id);
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
    // Use getUnchecked() because CacheLoader.load() of 'vertexCache' does not throw a checked exception.
    return vertexCache.getUnchecked(id);
  }

  public static Builder builder() {
    return new Builder();
  }

  private Edge createAndCache(UUID factID, UUID inVertex, UUID outVertex) {
    // Try to fetch edge from cache first (but only if 'edgeID' is mapped, otherwise edge is not cached).
    EdgeID edgeID = new EdgeID(factID, inVertex, outVertex);
    Edge edge = ObjectUtils.ifNotNull(edgeIdMap.get(edgeID), edgeCache::getIfPresent);

    if (edge == null) {
      // Edge is not present in cache, create new instance and cache it for later access.
      edge = new FactEdge(owner, factID, inVertex, outVertex);
      edgeIdMap.put(edgeID, (UUID) edge.id());
      edgeCache.put((UUID) edge.id(), edge);
    }

    return edge;
  }

  private Cache<UUID, Edge> createEdgeCache() {
    return CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAXIMUM_SIZE)
            .removalListener(createCleanUpListener())
            .build();
  }

  private LoadingCache<UUID, Vertex> createVertexCache() {
    return CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAXIMUM_SIZE)
            .build(new CacheLoader<UUID, Vertex>() {
              @Override
              public Vertex load(UUID key) {
                return new ObjectVertex(owner, key);
              }
            });
  }

  private RemovalListener<Object, Object> createCleanUpListener() {
    return notification -> {
      // Need to clean up 'edgeIdMap' when an entry gets evicted.
      if (notification.wasEvicted()) {
        SetUtils.set(edgeIdMap.entrySet())
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), notification.getKey()))
                .forEach(entry -> edgeIdMap.remove(entry.getKey()));
      }
    };
  }

  public static class Builder {
    private ActGraph owner;
    private FactManager factManager;

    private Builder() {
    }

    public ElementFactory build() {
      return new ElementFactory(owner, factManager);
    }

    public Builder setFactManager(FactManager factManager) {
      this.factManager = factManager;
      return this;
    }

    public Builder setOwner(ActGraph owner) {
      this.owner = owner;
      return this;
    }
  }

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
