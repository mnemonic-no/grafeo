package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.*;

import static no.mnemonic.act.platform.dao.tinkerpop.FactProperty.*;
import static org.apache.tinkerpop.gremlin.structure.Edge.Exceptions.edgeRemovalNotSupported;

/**
 * An edge represents a binding between two Objects by one Fact in the Object-Fact-Model. A Fact can be represented by
 * multiple edges if the Fact is bound to more than two Objects. Because of that, {@link Edge#id()} will return an
 * edge-specific UUID and NOT the Fact's UUID. This UUID is randomly generated when the edge is created.
 */
public class FactEdge implements Edge {

  private final ActGraph graph;
  private final FactEntity fact;
  private final FactTypeEntity type;
  private final Vertex inVertex;
  private final Vertex outVertex;
  private final UUID edgeID;
  private final Set<Property> allProperties;

  public FactEdge(ActGraph graph, UUID factID, UUID inVertexObjectID, UUID outVertexObjectID) {
    this.graph = ObjectUtils.notNull(graph, "'graph' is null!");
    this.fact = ObjectUtils.notNull(graph.getFactManager().getFact(factID), String.format("Fact with id = %s does not exist.", factID));
    this.type = ObjectUtils.notNull(graph.getFactManager().getFactType(fact.getTypeID()), String.format("FactType with id = %s does not exist.", fact.getTypeID()));
    this.inVertex = graph.getElementFactory().getVertex(inVertexObjectID);
    this.outVertex = graph.getElementFactory().getVertex(outVertexObjectID);
    this.edgeID = UUID.randomUUID(); // Generate a random ID for each new edge.
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

  public FactEntity getFact() {
    return fact;
  }

  private Set<Property> getAllProperties() {
    // Currently, those properties only expose information directly from a Fact. Some additional interesting properties
    // would be e.g. confidenceLevel, organizationName, sourceName, sourceTrustLevel, but those are not stored yet and
    // therefore not easily available. Maybe it would be good to expose complex OrganizationProperty and SourceProperty
    // properties instead of one simple property per field?
    return SetUtils.set(
            new FactID(fact, this),
            new Value(fact, this),
            new InReferenceToID(fact, this),
            new OrganizationID(fact, this),
            new SourceID(fact, this),
            new AccessMode(fact, this),
            new Timestamp(fact, this),
            new LastSeenTimestamp(fact, this)
    );
  }

}
