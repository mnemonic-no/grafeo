package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.*;

import static no.mnemonic.act.platform.service.ti.tinkerpop.ObjectProperty.Value;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static no.mnemonic.commons.utilities.collections.SetUtils.union;
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
  private final Set<VertexProperty> allProperties;

  public ObjectVertex(ActGraph graph, ObjectRecord object, ObjectFactTypeResolver.ObjectTypeStruct type) {
    this.graph = ObjectUtils.notNull(graph, "'graph' is null!");
    this.object = ObjectUtils.notNull(object, "'object' is null!");
    this.type = ObjectUtils.notNull(type, "'type' is null!");
    this.allProperties = Collections.unmodifiableSet(getAllProperties()); // Generate properties set only once.
  }

  @Override
  public Edge addEdge(String label, Vertex inVertex, Object... keyValues) {
    throw edgeAdditionsNotSupported();
  }

  @Override
  public Iterator<Edge> edges(Direction direction, String... edgeLabels) {
    Set<UUID> factTypeIds = graph.getObjectFactTypeResolver().factTypeNamesToIds(set(edgeLabels));

    ResultContainer<FactRecord> factRecords = graph.getObjectFactDao().searchFacts(
            FactSearchCriteria.builder()
                    .addObjectID(object.getId())
                    .setFactTypeID(factTypeIds)
                    .setStartTimestamp(graph.getTraverseParams().getAfterTimestamp())
                    .setEndTimestamp(graph.getTraverseParams().getBeforeTimestamp())
                    .addTimeFieldStrategy(FactSearchCriteria.TimeFieldStrategy.timestamp)
                    .setCurrentUserID(graph.getSecurityContext().getCurrentUserID())
                    .setAvailableOrganizationID(graph.getSecurityContext().getAvailableOrganizationID())
                    .build());

    return factRecords
            .stream()
            .filter(record -> matchesDirection(record, object, direction))
            .filter(graph.getSecurityContext()::hasReadPermission)
            .map(graph.getElementFactory()::createEdge)
            .filter(Objects::nonNull)
            .iterator();
  }

  @Override
  public Iterator<Vertex> vertices(Direction direction, String... edgeLabels) {
    return IteratorUtils.stream(edges(direction, edgeLabels))
            .map(e -> set(e.vertices(direction)))
            .reduce(new HashSet<>(), (result, next) -> union(result, next))
            .iterator();
  }

  @Override
  public <V> Iterator<VertexProperty<V>> properties(String... propertyKeys) {
    //noinspection unchecked
    return allProperties.stream()
            .filter(property -> set(propertyKeys).isEmpty() || SetUtils.in(property.key(), propertyKeys))
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

  private Set<VertexProperty> getAllProperties() {
    // Currently, only one property is exposed. Object statistics would be interesting as well, but this requires an
    // external index in order to allow efficient graph traversals.
    return set(new Value(object, this));
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
}
