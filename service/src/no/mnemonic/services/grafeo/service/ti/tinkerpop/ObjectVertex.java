package no.mnemonic.services.grafeo.service.ti.tinkerpop;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.api.result.ResultContainer;
import no.mnemonic.services.grafeo.service.ti.tinkerpop.utils.ObjectFactTypeResolver;
import no.mnemonic.services.grafeo.service.ti.tinkerpop.utils.PropertyEntry;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.*;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.apache.tinkerpop.gremlin.structure.Direction.*;
import static org.apache.tinkerpop.gremlin.structure.Vertex.Exceptions.edgeAdditionsNotSupported;
import static org.apache.tinkerpop.gremlin.structure.Vertex.Exceptions.vertexRemovalNotSupported;

/**
 * A vertex in the graph represents an Object in the Object-Fact-Model. It's a one-to-one relationship, i.e. one Object
 * is represented by one vertex. Because of that, {@link Vertex#id()} will return the Object's UUID.
 * <p>
 * Adjacent edges represent Facts where the edge direction (IN, OUT) is mapped onto the binding's direction between
 * Object and Fact.
 */
public class ObjectVertex implements Vertex {

  private final ActGraph graph;
  private final ObjectRecord object;
  private final ObjectFactTypeResolver.ObjectTypeStruct type;
  // Key is the property name and value the actual property. Note that a vertex can have multiple properties for the
  // same name (see https://tinkerpop.apache.org/docs/current/reference/#vertex-properties), hence using a list.
  private Map<String, List<VertexProperty<?>>> properties = new HashMap<>();
  private boolean hasFetchedAllProperties = false;

  private ObjectVertex(ActGraph graph,
                       ObjectRecord object,
                       ObjectFactTypeResolver.ObjectTypeStruct type) {
    this.graph = ObjectUtils.notNull(graph, "'graph' is null!");
    this.object = ObjectUtils.notNull(object, "'object' is null!");
    this.type = ObjectUtils.notNull(type, "'type' is null!");
  }

  @Override
  public Edge addEdge(String label, Vertex inVertex, Object... keyValues) {
    throw edgeAdditionsNotSupported();
  }

  @Override
  public Iterator<Edge> edges(Direction direction, String... edgeLabels) {
    Set<UUID> factTypeIds = graph.getObjectFactTypeResolver().factTypeNamesToIds(set(edgeLabels));

    ResultContainer<FactRecord> factRecords = graph.getObjectFactDao().searchFacts(
            graph.getTraverseParams().getBaseSearchCriteria()
                    .toBuilder()
                    .addObjectID(object.getId())
                    .setFactTypeID(factTypeIds)
                    .build());

    return factRecords
            .stream()
            .filter(record -> matchesDirection(record, object, direction))
            .filter(graph.getSecurityContext()::hasReadPermission)
            .filter(record -> graph.getTraverseParams().isIncludeRetracted() ||
                    !graph.getFactRetractionHandler().isRetracted(record))
            .map(record -> graph.getElementFactory().createEdge(record, object.getId()))
            .filter(Objects::nonNull)
            .iterator();
  }

  @Override
  public Iterator<Vertex> vertices(Direction direction, String... edgeLabels) {
    return IteratorUtils.stream(edges(direction, edgeLabels))
            .map(e -> set(e.vertices(direction)))
            .reduce(new HashSet<>(), SetUtils::union)
            .iterator();
  }

  @Override
  public <V> Iterator<VertexProperty<V>> properties(String... propertyKeys) {
    // If no keys are specified all properties need to be returned.
    boolean shouldReturnAllProperties = set(propertyKeys).isEmpty();

    // 1. Step: Fetch all requested properties and cache them for later.
    if (shouldReturnAllProperties) {
      if (!hasFetchedAllProperties) {
        properties = getAllProperties();
        hasFetchedAllProperties = true;
      }
    } else {
      // Only fetch the requested properties to avoid unnecessary calls towards ElasticSearch for instance.
      for (String key : propertyKeys) {
        properties.computeIfAbsent(key, this::getPropertiesForKey);
      }
    }

    // 2. Step: Filter and return requested properties.
    //noinspection unchecked
    return properties.entrySet()
            .stream()
            .filter(entry -> shouldReturnAllProperties || SetUtils.in(entry.getKey(), propertyKeys))
            .flatMap(entry -> entry.getValue().stream())
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

  public ObjectRecord getObjectRecord() {
    return this.object;
  }

  public static Builder builder() {
    return new Builder();
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

  private Map<String, List<VertexProperty<?>>> getAllProperties() {
    return graph.getPropertyHelper()
            .getObjectProperties(object, graph.getTraverseParams())
            .stream()
            .collect(Collectors.toMap(
                    PropertyEntry::getName,
                    // If only one property for a given name exists this will create a list with one element.
                    p -> list(new ObjectProperty<>(this, p.getName(), p.getValue())),
                    // If there are multiple properties for a given name merge all into one list.
                    ListUtils::concatenate
            ));
  }

  private List<VertexProperty<?>> getPropertiesForKey(String key) {
    return list(graph.getPropertyHelper().getObjectProperties(object, graph.getTraverseParams(), key),
            p -> new ObjectProperty<>(this, p.getName(), p.getValue()));
  }

  static boolean matchesDirection(FactRecord fact, ObjectRecord object, Direction direction) {
    ObjectRecord sourceObject = fact.getSourceObject();
    ObjectRecord destinationObject = fact.getDestinationObject();

    // One legged facts are not supported when traversing
    if (sourceObject == null || destinationObject == null) return false;

    boolean isLoop = Objects.equals(sourceObject.getId(), destinationObject.getId());

    // Loops are not supported when traversing, nor are they possible to create in the API
    if (isLoop) return false;

    boolean matchesBidirectional = (fact.isBidirectionalBinding() || direction == BOTH);

    return matchesBidirectional ||
            // object --- fact --> anyObject
            (Objects.equals(object.getId(), sourceObject.getId()) && direction == OUT) ||
            // object <-- fact -- anyObject
            (Objects.equals(object.getId(), destinationObject.getId()) && direction == IN);
  }

  public static class Builder {
    private ActGraph graph;
    private ObjectRecord objectRecord;
    private ObjectFactTypeResolver.ObjectTypeStruct objectType;

    private Builder() {}

    public ObjectVertex build() {
      return new ObjectVertex(graph, objectRecord, objectType);
    }

    public Builder setGraph(ActGraph graph) {
      this.graph = graph;
      return this;
    }

    public Builder setObjectRecord(ObjectRecord objectRecord) {
      this.objectRecord = objectRecord;
      return this;
    }

    public Builder setObjectType(ObjectFactTypeResolver.ObjectTypeStruct objectType) {
      this.objectType = objectType;
      return this;
    }
  }
}
