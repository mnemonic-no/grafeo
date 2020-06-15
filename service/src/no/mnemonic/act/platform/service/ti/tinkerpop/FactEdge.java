package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.PropertyEntry;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.*;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.apache.tinkerpop.gremlin.structure.Edge.Exceptions.edgeRemovalNotSupported;

/**
 * An edge represents a binding between two Objects by one Fact in the Object-Fact-Model.
 */
public class FactEdge implements Edge {

  private final ActGraph graph;
  private final FactRecord fact;
  private final FactTypeStruct type;
  private final Vertex inVertex;
  private final Vertex outVertex;
  private final Set<Property<?>> properties;

  private FactEdge(ActGraph graph,
                   FactRecord fact,
                   FactTypeStruct type,
                   Vertex inVertex,
                   Vertex outVertex,
                   List<PropertyEntry<?>> properties) {
    this.graph = ObjectUtils.notNull(graph, "'graph' is null!");
    this.fact = ObjectUtils.notNull(fact, "'fact' is null!");
    this.type = ObjectUtils.notNull(type, "'type' is null!");
    this.inVertex = ObjectUtils.notNull(inVertex, "'inVertex' is null!");
    this.outVertex = ObjectUtils.notNull(outVertex, "'outVertex' is null!");
    this.properties = Collections.unmodifiableSet(getAllProperties(ObjectUtils.notNull(properties, "'properties is null!'")));
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
    return fact.getId();
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
    //noinspection unchecked
    return properties.stream()
            .filter(property -> set(propertyKeys).isEmpty() || SetUtils.in(property.key(), propertyKeys))
            .map(property -> (Property<V>) property)
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

  private Set<Property<?>> getAllProperties(List<PropertyEntry<?>> properties) {
    return properties.stream().map(p -> new FactProperty<>(this, p.getName(), p.getValue())).collect(Collectors.toSet());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private ActGraph graph;
    private FactRecord fact;
    private FactTypeStruct type;
    private Vertex inVertex;
    private Vertex outVertex;
    private List<PropertyEntry<?>> properties;

    private Builder() {
    }

    public FactEdge build() {
      return new FactEdge(graph, fact, type, inVertex, outVertex, ObjectUtils.ifNull(properties, list()));
    }

    public Builder setGraph(ActGraph graph) {
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

    public Builder setProperties(List<PropertyEntry<?>> properties) {
      this.properties = properties;
      return this;
    }

    public Builder addProperty(PropertyEntry<?> property) {
      this.properties = ListUtils.addToList(this.properties, property);
      return this;
    }
  }
}
