package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.PropertyEntry;
import no.mnemonic.commons.utilities.collections.MapUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactEdgeTest {

  @Mock
  private ActGraph actGraph;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  public void testCreateEdgeWithoutGraph() {
    assertThrows(RuntimeException.class, () -> {
      FactEdge.builder()
              .setFactRecord(new FactRecord())
              .setFactType(FactTypeStruct.builder().build())
              .setInVertex(mock(Vertex.class))
              .setOutVertex(mock(Vertex.class))
              .build();
    });
  }

  @Test
  public void testCreateEdgeWithoutFact() {
    assertThrows(RuntimeException.class, () -> FactEdge.builder()
            .setGraph(actGraph)
            .setFactType(FactTypeStruct.builder().build())
            .setInVertex(mock(Vertex.class))
            .setOutVertex(mock(Vertex.class))
            .build());
  }

  @Test
  public void testCreateEdgeWithoutFactType() {
    assertThrows(RuntimeException.class, () -> FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(new FactRecord())
            .setInVertex(mock(Vertex.class))
            .setOutVertex(mock(Vertex.class))
            .build());
  }

  @Test
  public void testCreateEdgeWithoutInVertex() {
    assertThrows(RuntimeException.class, () -> FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(new FactRecord())
            .setFactType(FactTypeStruct.builder().build())
            .setOutVertex(mock(Vertex.class))
            .build());
  }

  @Test
  public void testCreateEdgeWithoutOutVertex() {
    assertThrows(RuntimeException.class, () -> FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(new FactRecord())
            .setFactType(FactTypeStruct.builder().build())
            .setInVertex(mock(Vertex.class))
            .build());
  }

  @Test
  public void testCreateEdge() {
    UUID factId = UUID.randomUUID();
    FactEdge edge = FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(new FactRecord().setId(factId))
            .setFactType(FactTypeStruct.builder().setId(UUID.randomUUID()).setName("someType").build())
            .setInVertex(mock(Vertex.class))
            .setOutVertex(mock(Vertex.class))
            .build();

    assertEquals(factId, edge.id());
    assertSame(actGraph, edge.graph());
    assertEquals("someType", edge.label());
  }

  @Test
  public void testVerticesWithDirectionIn() {
    ObjectVertex destination = createVertex();
    ObjectVertex source = createVertex();

    Edge edge = FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(new FactRecord())
            .setFactType(FactTypeStruct.builder().build())
            .setInVertex(destination)
            .setOutVertex(source)
            .build();

    Iterator<Vertex> vertices = edge.vertices(Direction.IN);
    assertSame(destination.id(), vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  @Test
  public void testVerticesWithDirectionOut() {
    ObjectVertex destination = createVertex();
    ObjectVertex source = createVertex();

    Edge edge = FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(new FactRecord())
            .setFactType(FactTypeStruct.builder().build())
            .setInVertex(destination)
            .setOutVertex(source)
            .build();

    Iterator<Vertex> vertices = edge.vertices(Direction.OUT);
    assertSame(source.id(), vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  @Test
  public void testVerticesWithDirectionBoth() {
    ObjectVertex destination = createVertex();
    ObjectVertex source = createVertex();

    Edge edge = FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(new FactRecord())
            .setFactType(FactTypeStruct.builder().build())
            .setInVertex(destination)
            .setOutVertex(source)
            .build();

    Iterator<Vertex> vertices = edge.vertices(Direction.BOTH);
    assertSame(source.id(), vertices.next().id());
    assertSame(destination.id(), vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  @Test
  public void testPropertiesWithDefaultProperties() {
    Edge edge = createEdge();
    assertFalse(edge.properties().hasNext());
  }

  @Test
  public void testPropertiesWithoutMatchingProperty() {
    Edge edge = createEdge();
    assertFalse(edge.properties("something").hasNext());
  }

  @Test
  public void testPropertiesWithMatchingProperty() {
    Edge edge = createEdge(list(new PropertyEntry<>("value", "test")));
    assertTrue(edge.properties("value").hasNext());
  }

  @Test
  public void testPropertiesWithMetaFacts() {
    Edge edge = FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(new FactRecord())
            .setFactType(FactTypeStruct.builder()
                    .setId(UUID.randomUUID())
                    .setName("someFactType")
                    .build())
            .setInVertex(createVertex())
            .setOutVertex(createVertex())
            .setProperties(list(
                    new PropertyEntry<>("meta/tlp", "green"),
                    new PropertyEntry<>("meta/observationTime", "2")))
            .build();

    Map<String, Property<Object>> props = MapUtils.map(edge.properties(), p -> MapUtils.Pair.T(p.key(), p));
    assertEquals("green", props.get("meta/tlp").value());
    assertEquals("2", props.get("meta/observationTime").value());
  }

  /* The following tests are adapted from gremlin-test EdgeTest. */

  @Test
  public void testValidateEquality() {
    Edge edge1 = createEdge();
    Edge edge2 = createEdge();

    assertEquals(edge1, edge1);
    assertEquals(edge2, edge2);
    assertNotEquals(edge1, edge2);
  }

  @Test
  public void testValidateIdEquality() {
    Edge edge1 = createEdge();
    Edge edge2 = createEdge();

    assertEquals(edge1.id(), edge1.id());
    assertEquals(edge2.id(), edge2.id());
    assertNotEquals(edge1.id(), edge2.id());
  }

  @Test
  public void testStandardStringRepresentation() {
    Edge edge = createEdge();
    assertEquals(StringFactory.edgeString(edge), edge.toString());
  }

  @Test
  public void testAutotypeStringProperties() {
    Edge edge = createEdge(list(new PropertyEntry<>("value", "value")));
    String value = edge.value("value");
    assertEquals("value", value);
  }

  @Test
  public void testAutotypeLongProperties() {
    Edge edge = createEdge(list(new PropertyEntry<>("timestamp", 123456789L)));
    long timestamp = edge.value("timestamp");
    assertEquals(123456789L, timestamp);
  }

  @Test
  public void testAutotypeFloatProperties() {
    Edge edge = createEdge(list(new PropertyEntry<>("trust", 0.3f)));
    float trust = edge.value("trust");
    assertEquals(0.3f, trust, 0.0);
  }

  @Test
  public void testReturnEmptyPropertyIfKeyNonExistent() {
    Edge edge = createEdge();
    Property property = edge.property("something");
    assertEquals(Property.empty(), property);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetValueThatIsNotPresentOnEdge() {
    Edge edge = createEdge();
    edge.value("something");
  }

  @Test
  public void testReturnOutThenInOnVertexIterator() {
    FactTypeStruct factType = FactTypeStruct.builder().setId(UUID.randomUUID()).setName("someFactType").build();
    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(factType.getId())
            .setValue("value")
            .setInReferenceToID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .setOriginID(UUID.fromString("00000000-0000-0000-0000-000000000003"))
            .setTrust(0.3f)
            .setConfidence(0.5f)
            .setAccessMode(FactRecord.AccessMode.Public)
            .setTimestamp(123456789L)
            .setLastSeenTimestamp(987654321L);

    ObjectVertex source = ObjectVertex.builder()
            .setGraph(actGraph)
            .setObjectRecord(new ObjectRecord().setId(UUID.randomUUID()))
            .setObjectType(ObjectTypeStruct.builder().setId(UUID.randomUUID()).setName("someObjectType").build())
            .build();
    ObjectVertex destination = ObjectVertex.builder()
            .setGraph(actGraph)
            .setObjectRecord(new ObjectRecord().setId(UUID.randomUUID()))
            .setObjectType(ObjectTypeStruct.builder().setId(UUID.randomUUID()).setName("someOtherObjectType").build())
            .build();

    FactEdge edge = FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(factRecord)
            .setFactType(factType)
            .setInVertex(destination)
            .setOutVertex(source)
            .build();

    assertEquals(source.id(), edge.outVertex().id());
    assertEquals(destination.id(), edge.inVertex().id());

    Iterator<Vertex> vertices = edge.vertices(Direction.BOTH);
    assertTrue(vertices.hasNext());
    assertEquals(source.id(), vertices.next().id());
    assertTrue(vertices.hasNext());
    assertEquals(destination.id(), vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  private ObjectVertex createVertex() {
    return ObjectVertex.builder()
            .setGraph(actGraph)
            .setObjectType(ObjectTypeStruct.builder().build())
            .setObjectRecord(new ObjectRecord().setId(UUID.randomUUID()))
            .build();
  }

  private Edge createEdge() {
    return createEdge(list());
  }

  private Edge createEdge(List<PropertyEntry<?>> props) {

    FactTypeStruct factType = FactTypeStruct.builder()
            .setId(UUID.randomUUID())
            .setName("someFactType")
            .build();

    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(factType.getId())
            .setValue("value")
            .setInReferenceToID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .setOriginID(UUID.fromString("00000000-0000-0000-0000-000000000003"))
            .setAddedByID(UUID.fromString("00000000-0000-0000-0000-000000000004"))
            .setTrust(0.3f)
            .setConfidence(0.5f)
            .setAccessMode(FactRecord.AccessMode.Public)
            .setTimestamp(123456789L)
            .setLastSeenTimestamp(987654321L);

    ObjectTypeStruct objectType = ObjectTypeStruct.builder()
            .setId(UUID.randomUUID())
            .setName("someObjectType")
            .build();
    ObjectVertex source = ObjectVertex.builder()
            .setGraph(actGraph)
            .setObjectRecord(
                    new ObjectRecord()
                            .setId(UUID.randomUUID())
                            .setValue("someObjectValue")
                            .setTypeID(objectType.getId()))
            .setObjectType(objectType)
            .build();

    ObjectVertex destination = ObjectVertex.builder()
            .setGraph(actGraph)
            .setObjectRecord(
                    new ObjectRecord()
                            .setId(UUID.randomUUID())
                            .setValue("someOtherObjectValue")
                            .setTypeID(objectType.getId()))
            .setObjectType(objectType)
            .build();

    return FactEdge.builder()
            .setGraph(actGraph)
            .setFactRecord(factRecord)
            .setFactType(factType)
            .setInVertex(destination)
            .setOutVertex(source)
            .setProperties(props)
            .build();
  }
}
