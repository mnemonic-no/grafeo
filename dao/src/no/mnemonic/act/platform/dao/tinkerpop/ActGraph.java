package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.tinkerpop.exceptions.GraphOperationException;
import no.mnemonic.act.platform.dao.tinkerpop.utils.ElementFactory;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
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
import java.util.function.Predicate;

import static org.apache.tinkerpop.gremlin.structure.Graph.Exceptions.*;

/**
 * The ActGraph is a {@link Graph} implementation of the Object-Fact-Model on top of the Cassandra storage layer. It is
 * a read-only graph, i.e. the graph can only be traversed and no edges or vertices added. For the mapping of Objects
 * and Facts to vertices and edges see {@link ObjectVertex} and {@link FactEdge}, respectively.
 */
public class ActGraph implements Graph {

  private final ObjectManager objectManager;
  private final FactManager factManager;
  private final Predicate<FactEntity> hasFactAccess;
  private final ElementFactory elementFactory;

  private ActGraph(ObjectManager objectManager, FactManager factManager, Predicate<FactEntity> hasFactAccess) {
    this.objectManager = ObjectUtils.notNull(objectManager, "'objectManager' is null!");
    this.factManager = ObjectUtils.notNull(factManager, "'factManager' is null!");
    this.hasFactAccess = ObjectUtils.notNull(hasFactAccess, "'hasFactAccess' is null!");
    this.elementFactory = ElementFactory.builder().setOwner(this).build();
  }

  @Override
  public Vertex addVertex(Object... keyValues) {
    throw vertexAdditionsNotSupported();
  }

  @Override
  public <C extends GraphComputer> C compute(Class<C> graphComputerClass) throws IllegalArgumentException {
    throw graphComputerNotSupported();
  }

  @Override
  public GraphComputer compute() throws IllegalArgumentException {
    throw graphComputerNotSupported();
  }

  @Override
  public Iterator<Vertex> vertices(Object... vertexIds) {
    if (SetUtils.set(vertexIds).isEmpty()) throw new GraphOperationException("V() is not supported!");
    return SetUtils.set(id -> id instanceof Vertex ? (Vertex) id : elementFactory.getVertex((UUID) id), vertexIds).iterator();
  }

  @Override
  public Iterator<Edge> edges(Object... edgeIds) {
    if (SetUtils.set(edgeIds).isEmpty()) throw new GraphOperationException("E() is not supported!");
    return SetUtils.set(id -> id instanceof Edge ? (Edge) id : elementFactory.getEdge((UUID) id), edgeIds).iterator();
  }

  @Override
  public Transaction tx() {
    throw transactionsNotSupported();
  }

  @Override
  public void close() throws Exception {
    // NOOP, managers are handled outside this graph implementation.
  }

  @Override
  public Variables variables() {
    throw variablesNotSupported();
  }

  @Override
  public Configuration configuration() {
    return null;
  }

  public boolean hasFactAccess(FactEntity fact) {
    return hasFactAccess.test(fact);
  }

  public ObjectManager getObjectManager() {
    return objectManager;
  }

  public FactManager getFactManager() {
    return factManager;
  }

  ElementFactory getElementFactory() {
    return elementFactory;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ObjectManager objectManager;
    private FactManager factManager;
    private Predicate<FactEntity> hasFactAccess;

    private Builder() {
    }

    public ActGraph build() {
      return new ActGraph(objectManager, factManager, hasFactAccess);
    }

    public Builder setObjectManager(ObjectManager objectManager) {
      this.objectManager = objectManager;
      return this;
    }

    public Builder setFactManager(FactManager factManager) {
      this.factManager = factManager;
      return this;
    }

    public Builder setHasFactAccess(Predicate<FactEntity> hasFactAccess) {
      this.hasFactAccess = hasFactAccess;
      return this;
    }
  }

}
