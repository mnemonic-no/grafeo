package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import com.google.common.cache.*;
import no.mnemonic.act.platform.dao.cassandra.entity.Direction;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectFactBindingEntity;
import no.mnemonic.act.platform.service.ti.tinkerpop.ActGraph;
import no.mnemonic.act.platform.service.ti.tinkerpop.FactEdge;
import no.mnemonic.act.platform.service.ti.tinkerpop.ObjectVertex;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.*;

/**
 * Helper class for creation and retrieval of edges and vertices which implements simple caching.
 */
public class ElementFactory {

  private static final int CACHE_MAXIMUM_SIZE = 10000;

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

    FactEntity fact = owner.getFactManager().getFact(inBinding.getFactID());
    // Only create edges if user has access to Fact.
    if (fact == null || !owner.hasFactAccess(fact)) {
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
      if ((inBinding.getDirection() == Direction.BiDirectional && outBinding.getDirection() == Direction.BiDirectional) ||
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
    if (id == null) return null;
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
    if (id == null) return null;
    try {
      return vertexCache.get(id);
    } catch (Exception ignored) {
      // If vertex cannot be fetched, e.g. because 'id' references a non-existing Object, just return null.
      return null;
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  private Edge createAndCache(UUID factID, UUID inVertex, UUID outVertex) {
    // Try to fetch edge from cache first
    Edge edge = edgeCache.getIfPresent(factID);

    if (edge == null) {
      // Edge is not present in cache, create new instance and cache it for later access.
      edge = new FactEdge(owner, factID, inVertex, outVertex);
      edgeCache.put((UUID) edge.id(), edge);
    }

    return edge;
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
                return new ObjectVertex(owner, key);
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
