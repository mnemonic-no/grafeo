package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Iterator;
import java.util.UUID;

import static org.apache.tinkerpop.gremlin.structure.Graph.Exceptions.transactionsNotSupported;
import static org.apache.tinkerpop.gremlin.structure.Graph.Exceptions.vertexAdditionsNotSupported;

public class ActGraph implements Graph {

  private final ObjectManager objectManager;
  private final FactManager factManager;

  private ActGraph(ObjectManager objectManager, FactManager factManager) {
    this.objectManager = ObjectUtils.notNull(objectManager, "'objectManager' is null!");
    this.factManager = ObjectUtils.notNull(factManager, "'factManager' is null!");
  }

  @Override
  public Vertex addVertex(Object... keyValues) {
    throw vertexAdditionsNotSupported();
  }

  @Override
  public <C extends GraphComputer> C compute(Class<C> graphComputerClass) throws IllegalArgumentException {
    throw new UnsupportedOperationException("GraphComputer not supported");
  }

  @Override
  public GraphComputer compute() throws IllegalArgumentException {
    throw new UnsupportedOperationException("GraphComputer not supported");
  }

  @Override
  public Iterator<Vertex> vertices(Object... vertexIds) {
    return SetUtils.set(id -> id instanceof Vertex ? (Vertex) id : new ObjectVertex(this, (UUID) id), vertexIds).iterator();
  }

  @Override
  public Iterator<Edge> edges(Object... edgeIds) {
    return SetUtils.set(id -> id instanceof Edge ? (Edge) id : new FactEdge(this, (UUID) id), edgeIds).iterator();
  }

  @Override
  public Transaction tx() {
    throw transactionsNotSupported();
  }

  @Override
  public void close() throws Exception {

  }

  @Override
  public Variables variables() {
    return null;
  }

  @Override
  public Configuration configuration() {
    return null;
  }

  ObjectManager getObjectManager() {
    return objectManager;
  }

  FactManager getFactManager() {
    return factManager;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ObjectManager objectManager;
    private FactManager factManager;

    private Builder() {
    }

    public ActGraph build() {
      return new ActGraph(objectManager, factManager);
    }

    public Builder setObjectManager(ObjectManager objectManager) {
      this.objectManager = objectManager;
      return this;
    }

    public Builder setFactManager(FactManager factManager) {
      this.factManager = factManager;
      return this;
    }
  }

}
