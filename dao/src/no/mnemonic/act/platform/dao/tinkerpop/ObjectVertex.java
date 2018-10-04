package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectFactBindingEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.*;

import static no.mnemonic.act.platform.dao.cassandra.entity.Direction.*;
import static no.mnemonic.act.platform.dao.tinkerpop.ObjectProperty.Value;
import static org.apache.tinkerpop.gremlin.structure.Vertex.Exceptions.edgeAdditionsNotSupported;
import static org.apache.tinkerpop.gremlin.structure.Vertex.Exceptions.vertexRemovalNotSupported;

/**
 * A vertex in the graph represents an Object in the Object-Fact-Model. It's a one-to-one relationship, i.e. one Object
 * is represented by one vertex. Because of that, {@link Vertex#id()} will return the Object's UUID.
 * <p>
 * Adjacent edges represent Facts where the edge direction (IN, OUT) is mapped onto the binding's direction between
 * Object and Fact. If a Fact is only bound to one Object the edge will be a loop, and if the Fact is bound to more
 * than two Objects an edge to each Object is created.
 */
public class ObjectVertex implements Vertex {

  private final ActGraph graph;
  private final ObjectEntity object;
  private final ObjectTypeEntity type;
  private final List<ObjectFactBindingEntity> bindings;
  private final Set<VertexProperty> allProperties;

  public ObjectVertex(ActGraph graph, UUID objectID) {
    this.graph = ObjectUtils.notNull(graph, "'graph' is null!");
    this.object = ObjectUtils.notNull(graph.getObjectManager().getObject(objectID), String.format("Object with id = %s does not exist.", objectID));
    this.type = ObjectUtils.notNull(graph.getObjectManager().getObjectType(object.getTypeID()), String.format("ObjectType with id = %s does not exist.", object.getTypeID()));
    this.bindings = Collections.unmodifiableList(ListUtils.list(graph.getObjectManager().fetchObjectFactBindings(objectID)));
    this.allProperties = Collections.unmodifiableSet(getAllProperties()); // Generate properties set only once.
  }

  @Override
  public Edge addEdge(String label, Vertex inVertex, Object... keyValues) {
    throw edgeAdditionsNotSupported();
  }

  @Override
  public Iterator<Edge> edges(Direction direction, String... edgeLabels) {
    Set<Edge> facts = new HashSet<>();

    for (ObjectFactBindingEntity binding : bindings) {
      if (binding.getDirection() == BiDirectional) {
        facts.addAll(graph.getElementFactory().createEdges(binding));
      }

      if (binding.getDirection() == FactIsDestination && (direction == Direction.BOTH || direction == Direction.OUT)) {
        facts.addAll(graph.getElementFactory().createEdges(binding));
      }

      if (binding.getDirection() == FactIsSource && (direction == Direction.BOTH || direction == Direction.IN)) {
        facts.addAll(graph.getElementFactory().createEdges(binding));
      }
    }

    return facts
            .stream()
            .filter(edge -> SetUtils.set(edgeLabels).isEmpty() || SetUtils.in(edge.label(), edgeLabels))
            .iterator();
  }

  @Override
  public Iterator<Vertex> vertices(Direction direction, String... edgeLabels) {
    return SetUtils.set(edges(direction, edgeLabels), e -> SetUtils.set(e.vertices(direction)))
            .stream()
            .reduce(new HashSet<>(), (result, next) -> SetUtils.union(result, next))
            .iterator();
  }

  @Override
  public <V> Iterator<VertexProperty<V>> properties(String... propertyKeys) {
    //noinspection unchecked
    return allProperties.stream()
            .filter(property -> SetUtils.set(propertyKeys).isEmpty() || SetUtils.in(property.key(), propertyKeys))
            .map(property -> (VertexProperty<V>) property)
            .iterator();
  }

  @Override
  public <V> VertexProperty<V> property(VertexProperty.Cardinality cardinality, String key, V value, Object... keyValues) {
    throw new UnsupportedOperationException("Adding properties not supported");
  }

  @Override
  public Object id() {
    return object.getId();
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
  public void remove() {
    throw vertexRemovalNotSupported();
  }

  @Override
  public String toString() {
    return StringFactory.vertexString(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ObjectVertex that = (ObjectVertex) o;
    return Objects.equals(id(), that.id());
  }

  @Override
  public int hashCode() {
    return Objects.hash(id());
  }

  public ObjectEntity getObject() {
    return object;
  }

  private Set<VertexProperty> getAllProperties() {
    // Currently, only one property is exposed. Object statistics would be interesting as well, but this requires an
    // external index in order to allow efficient graph traversals.
    return SetUtils.set(
            new Value(object, this)
    );
  }

}
