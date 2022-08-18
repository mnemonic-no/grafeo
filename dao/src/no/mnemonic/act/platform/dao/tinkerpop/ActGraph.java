package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.tinkerpop.exceptions.GraphOperationException;
import no.mnemonic.act.platform.dao.tinkerpop.utils.ElementFactory;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Predicate;

import static org.apache.tinkerpop.gremlin.structure.Graph.Exceptions.*;

/**
 * The ActGraph is a {@link Graph} implementation of the Object-Fact-Model on top of the Cassandra storage layer. It is
 * a read-only graph, i.e. the graph can only be traversed and no edges or vertices added. For the mapping of Objects
 * and Facts to vertices and edges see {@link ObjectVertex} and {@link FactEdge}, respectively.
 *
 * @deprecated Will be replaced by the TinkerPop implementation in the service module.
 */
@Deprecated
public class ActGraph implements Graph {

  private static final Features SUPPORTED_FEATURES = new ActGraphFeatures();

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
  public <C extends GraphComputer> C compute(Class<C> graphComputerClass) {
    throw graphComputerNotSupported();
  }

  @Override
  public GraphComputer compute() {
    throw graphComputerNotSupported();
  }

  @Override
  public Iterator<Vertex> vertices(Object... vertexIds) {
    if (SetUtils.set(vertexIds).isEmpty()) throw new GraphOperationException("V() is not supported!");
    return SetUtils.set(this::resolveVertex, vertexIds).iterator();
  }

  @Override
  public Iterator<Edge> edges(Object... edgeIds) {
    if (SetUtils.set(edgeIds).isEmpty()) throw new GraphOperationException("E() is not supported!");
    return SetUtils.set(this::resolveEdge, edgeIds).iterator();
  }

  @Override
  public Transaction tx() {
    throw transactionsNotSupported();
  }

  @Override
  public void close() {
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

  @Override
  public Features features() {
    return SUPPORTED_FEATURES;
  }

  @Override
  public String toString() {
    return StringFactory.graphString(this, "");
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

  private Vertex resolveVertex(Object id) {
    Vertex vertex = elementFactory.getVertex(resolveId(id));
    if (vertex == null) {
      throw new NoSuchElementException(String.format("Vertex with id = %s does not exist.", id));
    }

    return vertex;
  }

  private Edge resolveEdge(Object id) {
    Edge edge = elementFactory.getEdge(resolveId(id));
    if (edge == null) {
      throw new NoSuchElementException(String.format("Edge with id = %s does not exist.", id));
    }

    return edge;
  }

  private UUID resolveId(Object id) {
    if (id instanceof UUID) {
      return (UUID) id;
    }

    if (id instanceof String) {
      return UUID.fromString((String) id);
    }

    if (id instanceof Element) {
      return (UUID) Element.class.cast(id).id();
    }

    throw new IllegalArgumentException(String.format("ID of class %s is not supported.", id.getClass().getSimpleName()));
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

  private static class ActGraphFeatures implements Features {

    private static final ActGraphGraphFeatures GRAPH_FEATURES = new ActGraphGraphFeatures();
    private static final ActGraphVertexFeatures VERTEX_FEATURES = new ActGraphVertexFeatures();
    private static final ActGraphEdgeFeatures EDGE_FEATURES = new ActGraphEdgeFeatures();

    @Override
    public GraphFeatures graph() {
      return GRAPH_FEATURES;
    }

    @Override
    public VertexFeatures vertex() {
      return VERTEX_FEATURES;
    }

    @Override
    public EdgeFeatures edge() {
      return EDGE_FEATURES;
    }

    @Override
    public String toString() {
      return StringFactory.featureString(this);
    }
  }

  private static class ActGraphGraphFeatures implements Features.GraphFeatures {

    private static final ActGraphVariableFeatures VARIABLE_FEATURES = new ActGraphVariableFeatures();

    @Override
    public boolean supportsComputer() {
      return false;
    }

    @Override
    public boolean supportsPersistence() {
      return false;
    }

    @Override
    public boolean supportsConcurrentAccess() {
      return true;
    }

    @Override
    public boolean supportsTransactions() {
      return false;
    }

    @Override
    public boolean supportsThreadedTransactions() {
      return false;
    }

    @Override
    public boolean supportsIoRead() {
      return false;
    }

    @Override
    public boolean supportsIoWrite() {
      return false;
    }

    @Override
    public boolean supportsOrderabilitySemantics() {
      return false;
    }

    @Override
    public boolean supportsServiceCall() {
      return false;
    }

    @Override
    public Features.VariableFeatures variables() {
      return VARIABLE_FEATURES;
    }
  }

  private static class ActGraphVertexFeatures extends ActGraphElementFeatures implements Features.VertexFeatures {

    private static final ActGraphVertexPropertyFeatures VERTEX_PROPERTY_FEATURES = new ActGraphVertexPropertyFeatures();

    @Override
    public VertexProperty.Cardinality getCardinality(String key) {
      return VertexProperty.Cardinality.single;
    }

    @Override
    public boolean supportsAddVertices() {
      return false;
    }

    @Override
    public boolean supportsRemoveVertices() {
      return false;
    }

    @Override
    public boolean supportsMultiProperties() {
      return false;
    }

    @Override
    public boolean supportsDuplicateMultiProperties() {
      return false;
    }

    @Override
    public boolean supportsMetaProperties() {
      return false;
    }

    @Override
    public boolean supportsUpsert() {
      return false;
    }

    @Override
    public Features.VertexPropertyFeatures properties() {
      return VERTEX_PROPERTY_FEATURES;
    }
  }

  private static class ActGraphEdgeFeatures extends ActGraphElementFeatures implements Features.EdgeFeatures {

    private static final ActGraphEdgePropertyFeatures EDGE_PROPERTY_FEATURES = new ActGraphEdgePropertyFeatures();

    @Override
    public boolean supportsAddEdges() {
      return false;
    }

    @Override
    public boolean supportsRemoveEdges() {
      return false;
    }

    @Override
    public boolean supportsUpsert() {
      return false;
    }

    @Override
    public Features.EdgePropertyFeatures properties() {
      return EDGE_PROPERTY_FEATURES;
    }
  }

  private static class ActGraphElementFeatures implements Features.ElementFeatures {
    @Override
    public boolean supportsNullPropertyValues() {
      return true;
    }

    @Override
    public boolean supportsAddProperty() {
      return false;
    }

    @Override
    public boolean supportsRemoveProperty() {
      return false;
    }

    @Override
    public boolean supportsUserSuppliedIds() {
      return false;
    }

    @Override
    public boolean supportsNumericIds() {
      return false;
    }

    @Override
    public boolean supportsStringIds() {
      return false;
    }

    @Override
    public boolean supportsUuidIds() {
      return true;
    }

    @Override
    public boolean supportsCustomIds() {
      return false;
    }

    @Override
    public boolean supportsAnyIds() {
      return false;
    }
  }

  private static class ActGraphVertexPropertyFeatures extends ActGraphPropertyFeatures implements Features.VertexPropertyFeatures {
    @Override
    public boolean supportsNullPropertyValues() {
      return true;
    }

    @Override
    public boolean supportsRemoveProperty() {
      return false;
    }

    @Override
    public boolean supportsUserSuppliedIds() {
      return false;
    }

    @Override
    public boolean supportsNumericIds() {
      return false;
    }

    @Override
    public boolean supportsStringIds() {
      return false;
    }

    @Override
    public boolean supportsUuidIds() {
      return true;
    }

    @Override
    public boolean supportsCustomIds() {
      return false;
    }

    @Override
    public boolean supportsAnyIds() {
      return false;
    }
  }

  private static class ActGraphEdgePropertyFeatures extends ActGraphPropertyFeatures implements Features.EdgePropertyFeatures {
    // Inherits everything from ActGraphPropertyFeatures.
  }

  private static class ActGraphPropertyFeatures extends ActGraphDataTypeFeatures implements Features.PropertyFeatures {
    @Override
    public boolean supportsLongValues() {
      return true;
    }

    @Override
    public boolean supportsFloatValues() {
      return true;
    }

    @Override
    public boolean supportsStringValues() {
      return true;
    }
  }

  private static class ActGraphVariableFeatures extends ActGraphDataTypeFeatures implements Features.VariableFeatures {
    // Inherits everything from ActGraphDataTypeFeatures.
  }

  private static class ActGraphDataTypeFeatures implements Features.DataTypeFeatures {
    @Override
    public boolean supportsBooleanValues() {
      return false;
    }

    @Override
    public boolean supportsByteValues() {
      return false;
    }

    @Override
    public boolean supportsDoubleValues() {
      return false;
    }

    @Override
    public boolean supportsFloatValues() {
      return false;
    }

    @Override
    public boolean supportsIntegerValues() {
      return false;
    }

    @Override
    public boolean supportsLongValues() {
      return false;
    }

    @Override
    public boolean supportsMapValues() {
      return false;
    }

    @Override
    public boolean supportsMixedListValues() {
      return false;
    }

    @Override
    public boolean supportsBooleanArrayValues() {
      return false;
    }

    @Override
    public boolean supportsByteArrayValues() {
      return false;
    }

    @Override
    public boolean supportsDoubleArrayValues() {
      return false;
    }

    @Override
    public boolean supportsFloatArrayValues() {
      return false;
    }

    @Override
    public boolean supportsIntegerArrayValues() {
      return false;
    }

    @Override
    public boolean supportsStringArrayValues() {
      return false;
    }

    @Override
    public boolean supportsLongArrayValues() {
      return false;
    }

    @Override
    public boolean supportsSerializableValues() {
      return false;
    }

    @Override
    public boolean supportsStringValues() {
      return false;
    }

    @Override
    public boolean supportsUniformListValues() {
      return false;
    }
  }
}
