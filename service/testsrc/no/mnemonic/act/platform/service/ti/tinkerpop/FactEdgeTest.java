package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
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

  @Test(expected = RuntimeException.class)
  public void testCreateEdgeWithoutGraph() {
    new FactEdge(null, new FactRecord(), FactTypeStruct.builder().build(), mock(Vertex.class), mock(Vertex.class));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateEdgeWithoutFact() {
    new FactEdge(actGraph, null, FactTypeStruct.builder().build(), mock(Vertex.class), mock(Vertex.class));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateEdgeWithoutFactType() {
    new FactEdge(actGraph, new FactRecord(), null, mock(Vertex.class), mock(Vertex.class));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateEdgeWithoutInVertex() {
    new FactEdge(actGraph, new FactRecord(), FactTypeStruct.builder().build(), null, mock(Vertex.class));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateEdgeWithoutOutVertex() {
    new FactEdge(actGraph, new FactRecord(), FactTypeStruct.builder().build(), mock(Vertex.class), null);
  }

  @Test
  public void testCreateEdge() {
    UUID factId = UUID.randomUUID();
    FactEdge edge = new FactEdge(
            actGraph,
            new FactRecord().setId(factId),
            FactTypeStruct.builder().setId(UUID.randomUUID()).setName("someType").build(),
            mock(Vertex.class),
            mock(Vertex.class));

    assertEquals(factId, edge.id());
    assertSame(actGraph, edge.graph());
    assertEquals("someType", edge.label());
  }

  @Test
  public void testVerticesWithDirectionIn() {
    ObjectVertex destination = new ObjectVertex(
            actGraph,
            new ObjectRecord().setId(UUID.randomUUID()),
            ObjectTypeStruct.builder().build());
    ObjectVertex source = new ObjectVertex(
            actGraph,
            new ObjectRecord().setId(UUID.randomUUID()),
            ObjectTypeStruct.builder().build());

    Edge edge = new FactEdge(
            actGraph,
            new FactRecord(),
            FactTypeStruct.builder().build(),
            destination,
            source);

    Iterator<Vertex> vertices = edge.vertices(Direction.IN);
    assertSame(destination.id(), vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  @Test
  public void testVerticesWithDirectionOut() {
    ObjectVertex destination = new ObjectVertex(
            actGraph,
            new ObjectRecord().setId(UUID.randomUUID()),
            ObjectTypeStruct.builder().build());
    ObjectVertex source = new ObjectVertex(
            actGraph,
            new ObjectRecord().setId(UUID.randomUUID()),
            ObjectTypeStruct.builder().build());

    Edge edge = new FactEdge(
            actGraph,
            new FactRecord(),
            FactTypeStruct.builder().build(),
            destination,
            source);

    Iterator<Vertex> vertices = edge.vertices(Direction.OUT);
    assertSame(source.id(), vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  @Test
  public void testVerticesWithDirectionBoth() {
    ObjectVertex destination = new ObjectVertex(
            actGraph,
            new ObjectRecord().setId(UUID.randomUUID()),
            ObjectTypeStruct.builder().build());
    ObjectVertex source = new ObjectVertex(
            actGraph,
            new ObjectRecord().setId(UUID.randomUUID()),
            ObjectTypeStruct.builder().build());

    Edge edge = new FactEdge(
            actGraph,
            new FactRecord(),
            FactTypeStruct.builder().build(),
            destination,
            source);

    Iterator<Vertex> vertices = edge.vertices(Direction.BOTH);
    assertSame(source.id(), vertices.next().id());
    assertSame(destination.id(), vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  @Test
  public void testPropertiesWithAllProperties() {
    Edge edge = createEdge();
    assertTrue(edge.properties().hasNext());
  }

  @Test
  public void testPropertiesWithoutMatchingProperty() {
    Edge edge = createEdge();
    assertFalse(edge.properties("something").hasNext());
  }

  @Test
  public void testPropertiesWithMatchingProperty() {
    Edge edge = createEdge();
    assertTrue(edge.properties("value").hasNext());
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
    Edge edge = createEdge();
    String value = edge.value("value");
    assertEquals("value", value);
  }

  @Test
  public void testAutotypeLongProperties() {
    Edge edge = createEdge();
    long timestamp = edge.value("timestamp");
    assertEquals(123456789L, timestamp);
  }

  @Test
  public void testAutotypeFloatProperties() {
    Edge edge = createEdge();
    float trust = edge.value("trust");
    assertEquals(0.3f, trust, 0.0);
  }

  @Test
  public void testGetPropertyKeysOnEdge() {

    Edge edge = createEdge();

    // Test that the following properties exists on the edge.
    Map<String, Object> expected = MapUtils.map(
            T("factID", edge.id().toString()),
            T("value", "value"),
            T("inReferenceToID", "00000000-0000-0000-0000-000000000001"),
            T("organizationID", "00000000-0000-0000-0000-000000000002"),
            T("originID", "00000000-0000-0000-0000-000000000003"),
            T("trust", 0.3f),
            T("confidence", 0.5f),
            T("certainty", 0.15f),
            T("accessMode", "Public"),
            T("timestamp", 123456789L),
            T("lastSeenTimestamp", 987654321L)
    );

    Set<String> keys = edge.keys();
    Set<Property<Object>> properties = SetUtils.set(edge.properties());

    assertEquals(expected.size(), keys.size());
    assertEquals(expected.size(), properties.size());

    for (Map.Entry<String, Object> entry : expected.entrySet()) {
      assertTrue(keys.contains(entry.getKey()));

      Property<Object> property = edge.property(entry.getKey());
      assertEquals(entry.getValue(), property.value());
      assertEquals(StringFactory.propertyString(property), property.toString());
      assertSame(edge, property.element());
    }
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
    FactTypeStruct factTypeStruct = FactTypeStruct.builder().setId(UUID.randomUUID()).setName("someFactType").build();
    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(factTypeStruct.getId())
            .setValue("value")
            .setInReferenceToID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .setOriginID(UUID.fromString("00000000-0000-0000-0000-000000000003"))
            .setTrust(0.3f)
            .setConfidence(0.5f)
            .setAccessMode(FactRecord.AccessMode.Public)
            .setTimestamp(123456789L)
            .setLastSeenTimestamp(987654321L);

    ObjectVertex source = new ObjectVertex(
            actGraph,
            new ObjectRecord().setId(UUID.randomUUID()),
            ObjectTypeStruct.builder().setId(UUID.randomUUID()).setName("someObjectType").build());
    ObjectVertex destination = new ObjectVertex(
            actGraph,
            new ObjectRecord().setId(UUID.randomUUID()),
            ObjectTypeStruct.builder().setId(UUID.randomUUID()).setName("someOtherObjectType").build());

    FactEdge edge = new FactEdge(
            actGraph,
            factRecord,
            factTypeStruct,
            destination,
            source
    );

    assertEquals(source.id(), edge.outVertex().id());
    assertEquals(destination.id(), edge.inVertex().id());

    Iterator<Vertex> vertices = edge.vertices(Direction.BOTH);
    assertTrue(vertices.hasNext());
    assertEquals(source.id(), vertices.next().id());
    assertTrue(vertices.hasNext());
    assertEquals(destination.id(), vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  private Edge createEdge() {

    FactTypeStruct factTypeStruct = FactTypeStruct.builder()
            .setId(UUID.randomUUID())
            .setName("someFactType")
            .build();

    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(factTypeStruct.getId())
            .setValue("value")
            .setInReferenceToID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .setOriginID(UUID.fromString("00000000-0000-0000-0000-000000000003"))
            .setTrust(0.3f)
            .setConfidence(0.5f)
            .setAccessMode(FactRecord.AccessMode.Public)
            .setTimestamp(123456789L)
            .setLastSeenTimestamp(987654321L);

    ObjectTypeStruct objectTypeStruct = ObjectTypeStruct.builder()
            .setId(UUID.randomUUID())
            .setName("someObjectType")
            .build();
    ObjectVertex source = new ObjectVertex(
            actGraph,
            new ObjectRecord()
                    .setId(UUID.randomUUID())
                    .setValue("someObjectValue")
                    .setTypeID(objectTypeStruct.getId()),
            objectTypeStruct);

    ObjectVertex destination = new ObjectVertex(
            actGraph,
            new ObjectRecord()
                    .setId(UUID.randomUUID())
                    .setValue("someOtherObjectValue")
                    .setTypeID(objectTypeStruct.getId()),
            objectTypeStruct);

    return new FactEdge(
            actGraph,
            factRecord,
            factTypeStruct,
            destination,
            source
    );
  }
}
