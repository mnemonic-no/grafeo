package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.tinkerpop.properties.ObjectValueProperty;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.util.iterator.EmptyIterator;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import static org.apache.tinkerpop.gremlin.structure.Vertex.Exceptions.edgeAdditionsNotSupported;
import static org.apache.tinkerpop.gremlin.structure.Vertex.Exceptions.vertexRemovalNotSupported;

public class ObjectVertex implements Vertex {

  private final Graph graph;
  private final ObjectEntity object;
  private final ObjectTypeEntity type;
  private final ObjectManager objectManager;

  ObjectVertex(Graph graph, UUID objectID) {
    if (!(graph instanceof ActGraph)) throw new IllegalArgumentException("Provided Graph instance is not an ActGraph!");
    this.graph = graph;
    this.objectManager = ActGraph.class.cast(graph).getObjectManager();
    this.object = objectManager.getObject(objectID);
    this.type = objectManager.getObjectType(object.getTypeID());
  }

  @Override
  public Edge addEdge(String label, Vertex inVertex, Object... keyValues) {
    throw edgeAdditionsNotSupported();
  }

  @Override
  public Iterator<Edge> edges(Direction direction, String... edgeLabels) {
    Set<Edge> facts = new HashSet<>();

    for (ObjectFactBindingEntity binding : objectManager.fetchObjectFactBindings(object.getId())) {
      if (binding.getDirection() == no.mnemonic.act.platform.entity.cassandra.Direction.BiDirectional) {
        facts.add(new FactEdge(graph, binding.getFactID()));
      }

      if (binding.getDirection() == no.mnemonic.act.platform.entity.cassandra.Direction.FactIsDestination
              && (direction == Direction.BOTH || direction == Direction.OUT)) {
        facts.add(new FactEdge(graph, binding.getFactID()));
      }

      if (binding.getDirection() == no.mnemonic.act.platform.entity.cassandra.Direction.FactIsSource
              && (direction == Direction.BOTH || direction == Direction.IN)) {
        facts.add(new FactEdge(graph, binding.getFactID()));
      }
    }

    return facts
            .stream()
            .filter(edge -> CollectionUtils.isEmpty(SetUtils.set(edgeLabels)) || SetUtils.in(edge.label(), edgeLabels))
            .iterator();
  }

  @Override
  public Iterator<Vertex> vertices(Direction direction, String... edgeLabels) {
    return SetUtils.set(edges(direction, edgeLabels), e -> IteratorUtils.set(e.vertices(direction)))
            .stream()
            .reduce(new HashSet<>(), (result, next) -> SetUtils.union(result, next))
            .iterator();
  }

  @Override
  public <V> Iterator<VertexProperty<V>> properties(String... propertyKeys) {
    Set<String> keys = SetUtils.set(propertyKeys);
    if (!keys.contains("value")) {
      return EmptyIterator.instance();
    }

    return IteratorUtils.of((VertexProperty<V>) new ObjectValueProperty(object, this));
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

}
