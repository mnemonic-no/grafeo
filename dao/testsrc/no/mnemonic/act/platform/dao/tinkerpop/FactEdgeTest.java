package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class FactEdgeTest extends AbstractGraphTest {

  @Test(expected = RuntimeException.class)
  public void testCreateEdgeWithoutGraph() {
    new FactEdge(null, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateEdgeWithoutFact() {
    new FactEdge(getActGraph(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateEdgeWithoutFactType() {
    UUID factID = UUID.randomUUID();
    when(getFactManager().getFact(factID)).thenReturn(new FactEntity()
            .setId(factID)
            .setTypeID(UUID.randomUUID())
            .setValue("value")
    );

    new FactEdge(getActGraph(), factID, UUID.randomUUID(), UUID.randomUUID());
  }

  @Test
  public void testCreateEdgeFromFact() {
    Edge edge = createEdge();
    assertNotNull(edge.id());
    assertEquals("type", edge.label());
    assertSame(getActGraph(), edge.graph());
  }

  @Test
  public void testVerticesWithDirectionIn() {
    UUID factID = mockFact(null);
    UUID inVertexObjectID = mockObject();
    UUID outVertexObjectID = mockObject();
    Edge edge = new FactEdge(getActGraph(), factID, inVertexObjectID, outVertexObjectID);

    Iterator<Vertex> vertices = edge.vertices(Direction.IN);
    assertSame(inVertexObjectID, vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  @Test
  public void testVerticesWithDirectionOut() {
    UUID factID = mockFact(null);
    UUID inVertexObjectID = mockObject();
    UUID outVertexObjectID = mockObject();
    Edge edge = new FactEdge(getActGraph(), factID, inVertexObjectID, outVertexObjectID);

    Iterator<Vertex> vertices = edge.vertices(Direction.OUT);
    assertSame(outVertexObjectID, vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  @Test
  public void testVerticesWithDirectionBoth() {
    UUID factID = mockFact(null);
    UUID inVertexObjectID = mockObject();
    UUID outVertexObjectID = mockObject();
    Edge edge = new FactEdge(getActGraph(), factID, inVertexObjectID, outVertexObjectID);

    Iterator<Vertex> vertices = edge.vertices(Direction.BOTH);
    assertSame(outVertexObjectID, vertices.next().id());
    assertSame(inVertexObjectID, vertices.next().id());
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
  public void testAutotypeEnumProperties() {
    Edge edge = createEdge();
    AccessMode accessMode = edge.value("accessMode");
    assertEquals(AccessMode.Public, accessMode);
  }

  @Test
  public void testAutotypeUuidProperties() {
    Edge edge = createEdge();
    UUID originID = edge.value("originID");
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000003"), originID);
  }

  @Test
  public void testGetPropertyKeysOnEdge() {
    UUID factID = mockFact(null);
    Edge edge = new FactEdge(getActGraph(), factID, mockObject(), mockObject());

    // Test that the following properties exists on the edge.
    Map<String, Object> expected = MapUtils.map(
            T("factID", factID),
            T("value", "value"),
            T("inReferenceToID", UUID.fromString("00000000-0000-0000-0000-000000000001")),
            T("organizationID", UUID.fromString("00000000-0000-0000-0000-000000000002")),
            T("originID", UUID.fromString("00000000-0000-0000-0000-000000000003")),
            T("trust", 0.3f),
            T("confidence", 0.5f),
            T("certainty", 0.15f),
            T("accessMode", AccessMode.Public),
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
    UUID factID = mockFact(null);
    UUID inVertexObjectID = mockObject();
    UUID outVertexObjectID = mockObject();
    Edge edge = new FactEdge(getActGraph(), factID, inVertexObjectID, outVertexObjectID);

    assertEquals(outVertexObjectID, edge.outVertex().id());
    assertEquals(inVertexObjectID, edge.inVertex().id());

    Iterator<Vertex> vertices = edge.vertices(Direction.BOTH);
    assertTrue(vertices.hasNext());
    assertEquals(outVertexObjectID, vertices.next().id());
    assertTrue(vertices.hasNext());
    assertEquals(inVertexObjectID, vertices.next().id());
    assertFalse(vertices.hasNext());
  }

  private Edge createEdge() {
    UUID factID = mockFact(null);
    UUID inVertexObjectID = mockObject();
    UUID outVertexObjectID = mockObject();
    return new FactEdge(getActGraph(), factID, inVertexObjectID, outVertexObjectID);
  }

}
