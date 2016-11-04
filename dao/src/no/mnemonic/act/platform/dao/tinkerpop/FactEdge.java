package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.tinkerpop.properties.FactValueProperty;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.FactTypeEntity;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.util.iterator.EmptyIterator;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import static org.apache.tinkerpop.gremlin.structure.Edge.Exceptions.edgeRemovalNotSupported;

public class FactEdge implements Edge {

  private final Graph graph;
  private final FactEntity fact;
  private final FactTypeEntity type;

  FactEdge(Graph graph, UUID factID) {
    if (!(graph instanceof ActGraph)) throw new IllegalArgumentException("Provided Graph instance is not an ActGraph!");
    this.graph = graph;
    FactManager factManager = ActGraph.class.cast(graph).getFactManager();
    this.fact = factManager.getFact(factID);
    this.type = factManager.getFactType(fact.getTypeID());
  }

  @Override
  public Iterator<Vertex> vertices(Direction direction) {
    Set<Vertex> objects = new HashSet<>();

    for (FactEntity.FactObjectBinding binding : fact.getBindings()) {
      if (binding.getDirection() == no.mnemonic.act.platform.entity.cassandra.Direction.BiDirectional) {
        objects.add(new ObjectVertex(graph, binding.getObjectID()));
      }

      if (binding.getDirection() == no.mnemonic.act.platform.entity.cassandra.Direction.FactIsDestination
              && (direction == Direction.BOTH || direction == Direction.IN)) {
        objects.add(new ObjectVertex(graph, binding.getObjectID()));
      }

      if (binding.getDirection() == no.mnemonic.act.platform.entity.cassandra.Direction.FactIsSource
              && (direction == Direction.BOTH || direction == Direction.OUT)) {
        objects.add(new ObjectVertex(graph, binding.getObjectID()));
      }
    }

    return objects.iterator();
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
    Set<String> keys = SetUtils.set(propertyKeys);
    if (!keys.contains("value")) {
      return EmptyIterator.instance();
    }

    return IteratorUtils.of((VertexProperty<V>) new FactValueProperty(fact, this));
  }

  @Override
  public <V> Property<V> property(String key, V value) {
    throw new UnsupportedOperationException("Adding properties not supported");
  }

  @Override
  public void remove() {
    throw edgeRemovalNotSupported();
  }

}
