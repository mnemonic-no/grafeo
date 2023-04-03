package no.mnemonic.services.grafeo.service.implementation.tinkerpop;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.PropertyEntry;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.apache.tinkerpop.gremlin.structure.Edge.Exceptions.edgeRemovalNotSupported;

/**
 * An edge represents a binding between two Objects by one Fact in the Object-Fact-Model. A Fact can be represented by
 * multiple edges if the Fact is bidirectional. Because of that, {@link Edge#id()} will return an
 * edge-specific UUID and NOT the Fact's UUID. This UUID is randomly generated when the edge is created.
 *
 */
public class FactEdge implements Edge {

  private final UUID edgeID;
  private final ObjectFactGraph graph;
  private final FactRecord fact;
  private final FactTypeStruct type;
  private final Vertex inVertex;
  private final Vertex outVertex;
  // Key is the property name and value the actual property. Note that an edge can only have one property for the same
  // name (see https://tinkerpop.apache.org/docs/current/reference/#vertex-properties). The data model on the other hand
  // allows multiple properties (i.e. meta Facts) with the same name. Because of that, the implementation will pick the
  // newest meta Fact for a given name to represent the property.
  private Map<String, Property<?>> properties = new HashMap<>();
  private boolean hasFetchedAllProperties = false;

  private FactEdge(ObjectFactGraph graph,
                   FactRecord fact,
                   FactTypeStruct type,
                   Vertex inVertex,
                   Vertex outVertex) {
    this.graph = ObjectUtils.notNull(graph, "'graph' is null!");
    this.fact = ObjectUtils.notNull(fact, "'fact' is null!");
    this.type = ObjectUtils.notNull(type, "'type' is null!");
    this.inVertex = ObjectUtils.notNull(inVertex, "'inVertex' is null!");
    this.outVertex = ObjectUtils.notNull(outVertex, "'outVertex' is null!");
    this.edgeID = UUID.randomUUID(); // Generate a random ID for each new edge.
  }

  @Override
  public Iterator<Vertex> vertices(Direction direction) {
    switch (direction) {
      case OUT:
        return IteratorUtils.of(outVertex);
      case IN:
        return IteratorUtils.of(inVertex);
      case BOTH:
        return IteratorUtils.of(outVertex, inVertex);
      default:
        throw new IllegalArgumentException(String.format("Unknown direction %s.", direction));
    }
  }

  @Override
  public Object id() {
    return edgeID;
  }

  @Override
  public String label() {
    return type.getName();
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public <V> Iterator<Property<V>> properties(String... propertyKeys) {
    // If no keys are specified all properties need to be returned.
    boolean shouldReturnAllProperties = set(propertyKeys).isEmpty();

    // 1. Step: Fetch all requested properties and cache them for later.
    if (shouldReturnAllProperties) {
      if (!hasFetchedAllProperties) {
        properties = getAllProperties();
        hasFetchedAllProperties = true;
      }
    } else {
      // Only fetch the requested properties to avoid unnecessary calls towards ElasticSearch for instance.
      for (String key : propertyKeys) {
        properties.computeIfAbsent(key, this::getPropertiesForKey);
      }
    }

    // 2. Step: Filter and return requested properties.
    //noinspection unchecked
    return properties.entrySet()
            .stream()
            .filter(entry -> shouldReturnAllProperties || SetUtils.in(entry.getKey(), propertyKeys))
            .map(entry -> (Property<V>) entry.getValue())
            .iterator();
  }

  @Override
  public <V> Property<V> property(String key, V value) {
    throw new UnsupportedOperationException("Adding properties not supported");
  }

  @Override
  public void remove() {
    throw edgeRemovalNotSupported();
  }

  public FactRecord getFactRecord() {
    return this.fact;
  }

  @Override
  public String toString() {
    return StringFactory.edgeString(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FactEdge that = (FactEdge) o;
    return Objects.equals(id(), that.id());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id());
  }

  private Map<String, Property<?>> getAllProperties() {
    return graph.getPropertyHelper()
            .getFactProperties(fact, graph.getTraverseParams())
            .stream()
            .collect(Collectors.toMap(PropertyEntry::getName,
                    Function.identity(),
                    // Pick out the newest property in order to remove duplicates.
                    (p1, p2) -> p1.getTimestamp() >= p2.getTimestamp() ? p1 : p2
            ))
            .values()
            .stream()
            // Need to create a new Map to convert from PropertyEntry to FactProperty. This cannot be done before
            // the first collect() because FactProperty doesn't contain the timestamp field.
            .collect(Collectors.toMap(PropertyEntry::getName, p -> new FactProperty<>(this, p.getName(), p.getValue())));
  }

  private Property<?> getPropertiesForKey(String key) {
    return graph.getPropertyHelper()
            .getFactProperties(fact, graph.getTraverseParams(), key)
            .stream()
            // Pick out the newest property in order to remove duplicates.
            .max(Comparator.comparingLong(PropertyEntry::getTimestamp))
            .map(p -> new FactProperty<>(this, p.getName(), p.getValue()))
            .orElse(null);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private ObjectFactGraph graph;
    private FactRecord fact;
    private FactTypeStruct type;
    private Vertex inVertex;
    private Vertex outVertex;

    private Builder() {
    }

    public FactEdge build() {
      return new FactEdge(graph, fact, type, inVertex, outVertex);
    }

    public Builder setGraph(ObjectFactGraph graph) {
      this.graph = graph;
      return this;
    }

    public Builder setFactRecord(FactRecord fact) {
      this.fact = fact;
      return this;
    }

    public Builder setFactType(FactTypeStruct type) {
      this.type = type;
      return this;
    }

    public Builder setInVertex(Vertex inVertex) {
      this.inVertex = inVertex;
      return this;
    }

    public Builder setOutVertex(Vertex outVertex) {
      this.outVertex = outVertex;
      return this;
    }
  }
}
