package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import static no.mnemonic.act.platform.service.ti.tinkerpop.FactProperty.*;
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
  private final Set<Property> allProperties;

  public FactEdge(ActGraph graph, FactRecord fact, FactTypeStruct type, Vertex inVertex, Vertex outVertex) {
    this.graph = ObjectUtils.notNull(graph, "'graph' is null!");
    this.fact = ObjectUtils.notNull(fact, "'fact' is null!");
    this.type = ObjectUtils.notNull(type, "'type' is null!");
    this.inVertex = ObjectUtils.notNull(inVertex, "'inVertex' is null!");
    this.outVertex = ObjectUtils.notNull(outVertex, "'outVertex' is null!");
    this.allProperties = Collections.unmodifiableSet(getAllProperties()); // Generate properties set only once.
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
    return allProperties.stream()
            .filter(property -> SetUtils.set(propertyKeys).isEmpty() || SetUtils.in(property.key(), propertyKeys))
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

  private Set<Property> getAllProperties() {
    // Currently, those properties only expose information directly from a Fact. Some additional interesting properties
    // would be e.g. organizationName or originName, but those are not directly available. Maybe it would be good to
    // expose complex OrganizationProperty and OriginProperty properties instead of one simple property per field?
    return SetUtils.set(
            new FactID(fact, this),
            new Value(fact, this),
            new InReferenceToID(fact, this),
            new OrganizationID(fact, this),
            new OriginID(fact, this),
            new Trust(fact, this),
            new Confidence(fact, this),
            new Certainty(fact, this),
            new AccessMode(fact, this),
            new Timestamp(fact, this),
            new LastSeenTimestamp(fact, this)
    );
  }
}
