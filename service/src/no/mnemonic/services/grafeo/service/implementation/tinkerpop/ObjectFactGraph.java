package no.mnemonic.services.grafeo.service.implementation.tinkerpop;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.handlers.FactRetractionHandler;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.exceptions.GraphOperationException;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ElementFactory;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ObjectFactTypeResolver;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.PropertyHelper;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.apache.tinkerpop.gremlin.structure.Graph.Exceptions.*;

/**
 * The ObjectFactGraph is a {@link Graph} implementation of the Object-Fact-Model on top of the Cassandra/Elasticsearch
 * storage layer. It is a read-only graph, i.e. the graph can only be traversed and no edges or vertices added. For the
 * mapping of Objects and Facts to vertices and edges see {@link ObjectVertex} and {@link FactEdge}, respectively.
 */
public class ObjectFactGraph implements Graph {

  private static final Features SUPPORTED_FEATURES = new SupportedFeatures();

  private final ElementFactory elementFactory;
  private final ObjectFactDao objectFactDao;
  private final ObjectFactTypeResolver objectFactTypeResolver;
  private final FactRetractionHandler factRetractionHandler;
  private final GrafeoSecurityContext securityContext;
  private final TraverseParams traverseParams;
  private final PropertyHelper propertyHelper;

  private ObjectFactGraph(ObjectFactDao objectFactDao,
                          ObjectFactTypeResolver objectFactTypeResolver,
                          FactRetractionHandler factRetractionHandler,
                          GrafeoSecurityContext securityContext,
                          TraverseParams traverseParams,
                          PropertyHelper propertyHelper) {
    this.objectFactDao = ObjectUtils.notNull(objectFactDao, "'objectFactDao' is null!");
    this.objectFactTypeResolver = ObjectUtils.notNull(objectFactTypeResolver, "'objectFactTypeResolver' is null!'");
    this.factRetractionHandler = ObjectUtils.notNull(factRetractionHandler, "'factRetractionHandler' is null!");
    this.securityContext = ObjectUtils.notNull(securityContext, "'securityContext' is null!");
    this.traverseParams = ObjectUtils.notNull(traverseParams, "'traverseParams' is null!");
    this.elementFactory = ElementFactory.builder().setOwner(this).build();
    this.propertyHelper = propertyHelper;
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

  public ObjectFactTypeResolver getObjectFactTypeResolver() {
    return objectFactTypeResolver;
  }

  public ObjectFactDao getObjectFactDao() {
    return objectFactDao;
  }

  public FactRetractionHandler getFactRetractionHandler() {
    return factRetractionHandler;
  }

  public GrafeoSecurityContext getSecurityContext() {
    return securityContext;
  }

  public TraverseParams getTraverseParams() {
    return traverseParams;
  }

  public PropertyHelper getPropertyHelper() {
    return propertyHelper;
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
    private ObjectFactDao objectFactDao;
    private ObjectFactTypeResolver objectFactTypeResolver;
    private GrafeoSecurityContext securityContext;
    private FactRetractionHandler factRetractionHandler;
    private TraverseParams traverseParams;
    private PropertyHelper propertyHelper;

    private Builder() {
    }

    public ObjectFactGraph build() {
      return new ObjectFactGraph(objectFactDao, objectFactTypeResolver, factRetractionHandler, securityContext, traverseParams, propertyHelper);
    }

    public Builder setObjectFactDao(ObjectFactDao objectFactDao) {
      this.objectFactDao = objectFactDao;
      return this;
    }

    public Builder setObjectTypeFactResolver(ObjectFactTypeResolver objectTypeFactResolver) {
      this.objectFactTypeResolver = objectTypeFactResolver;
      return this;
    }

    public Builder setFactRetractionHandler(FactRetractionHandler factRetractionHandler) {
      this.factRetractionHandler = factRetractionHandler;
      return this;
    }

    public Builder setSecurityContext(GrafeoSecurityContext securityContext) {
      this.securityContext = securityContext;
      return this;
    }

    public Builder setTraverseParams(TraverseParams traverseParams) {
      this.traverseParams = traverseParams;
      return this;
    }

    public Builder setPropertyHelper(PropertyHelper propertyHelper) {
      this.propertyHelper = propertyHelper;
      return this;
    }
  }

  private static class SupportedFeatures implements Features {

    private static final SupportedGraphFeatures GRAPH_FEATURES = new SupportedGraphFeatures();
    private static final SupportedVertexFeatures VERTEX_FEATURES = new SupportedVertexFeatures();
    private static final SupportedEdgeFeatures EDGE_FEATURES = new SupportedEdgeFeatures();

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

  private static class SupportedGraphFeatures implements Features.GraphFeatures {

    private static final SupportedVariableFeatures VARIABLE_FEATURES = new SupportedVariableFeatures();

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

  private static class SupportedVertexFeatures extends SupportedElementFeatures implements Features.VertexFeatures {

    private static final SupportedVertexPropertyFeatures VERTEX_PROPERTY_FEATURES = new SupportedVertexPropertyFeatures();

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

  private static class SupportedEdgeFeatures extends SupportedElementFeatures implements Features.EdgeFeatures {

    private static final SupportedEdgePropertyFeatures EDGE_PROPERTY_FEATURES = new SupportedEdgePropertyFeatures();

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

  private static class SupportedElementFeatures implements Features.ElementFeatures {
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

  private static class SupportedVertexPropertyFeatures extends SupportedPropertyFeatures implements Features.VertexPropertyFeatures {
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

  private static class SupportedEdgePropertyFeatures extends SupportedPropertyFeatures implements Features.EdgePropertyFeatures {
    // Inherits everything from SupportedPropertyFeatures.
  }

  private static class SupportedPropertyFeatures extends SupportedDataTypeFeatures implements Features.PropertyFeatures {
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

  private static class SupportedVariableFeatures extends SupportedDataTypeFeatures implements Features.VariableFeatures {
    // Inherits everything from SupportedDataTypeFeatures.
  }

  private static class SupportedDataTypeFeatures implements Features.DataTypeFeatures {
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
