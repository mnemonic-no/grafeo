package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.cassandra.entity.Direction;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectFactBindingEntity;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static org.apache.tinkerpop.gremlin.structure.Direction.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class ObjectVertexTest extends AbstractGraphTest {

  @Test(expected = RuntimeException.class)
  public void testCreateVertexWithoutGraph() {
    new ObjectVertex(null, UUID.randomUUID());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateVertexWithoutObject() {
    new ObjectVertex(getActGraph(), UUID.randomUUID());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateVertexWithoutObjectType() {
    UUID objectID = UUID.randomUUID();
    when(getObjectManager().getObject(objectID)).thenReturn(new ObjectEntity()
            .setId(objectID)
            .setTypeID(UUID.randomUUID())
            .setValue("value")
    );

    new ObjectVertex(getActGraph(), objectID);
  }

  @Test
  public void testCreateVertexFromObject() {
    UUID objectID = mockObject();
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);
    assertEquals(objectID, vertex.id());
    assertEquals("type", vertex.label());
    assertSame(getActGraph(), vertex.graph());
  }

  @Test
  public void testEdgesWithDirectionBiDirectional() {
    UUID objectID = mockObjectWithFact(Direction.BiDirectional);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.edges(BOTH).hasNext());
    assertTrue(vertex.edges(IN).hasNext());
    assertTrue(vertex.edges(OUT).hasNext());
  }

  @Test
  public void testEdgesWithDirectionFactIsDestination() {
    UUID objectID = mockObjectWithFact(Direction.FactIsDestination);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.edges(BOTH).hasNext());
    assertFalse(vertex.edges(IN).hasNext());
    assertTrue(vertex.edges(OUT).hasNext());
  }

  @Test
  public void testEdgesWithDirectionFactIsSource() {
    UUID objectID = mockObjectWithFact(Direction.FactIsSource);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.edges(BOTH).hasNext());
    assertTrue(vertex.edges(IN).hasNext());
    assertFalse(vertex.edges(OUT).hasNext());
  }

  @Test
  public void testEdgesFilterByLabel() {
    UUID objectID = mockObjectWithFact(Direction.BiDirectional);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.edges(BOTH, "type").hasNext());
    assertFalse(vertex.edges(BOTH, "something").hasNext());
  }

  @Test
  public void testVerticesWithDirectionBiDirectional() {
    UUID objectID = mockObjectWithFact(Direction.BiDirectional);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.vertices(BOTH).hasNext());
    assertTrue(vertex.vertices(IN).hasNext());
    assertTrue(vertex.vertices(OUT).hasNext());
  }

  @Test
  public void testVerticesWithDirectionFactIsDestination() {
    UUID objectID = mockObjectWithFact(Direction.FactIsDestination);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.vertices(BOTH).hasNext());
    assertFalse(vertex.vertices(IN).hasNext());
    assertTrue(vertex.vertices(OUT).hasNext());
  }

  @Test
  public void testVerticesWithDirectionFactIsSource() {
    UUID objectID = mockObjectWithFact(Direction.FactIsSource);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.vertices(BOTH).hasNext());
    assertTrue(vertex.vertices(IN).hasNext());
    assertFalse(vertex.vertices(OUT).hasNext());
  }

  @Test
  public void testVerticesFilterByLabel() {
    UUID objectID = mockObjectWithFact(Direction.BiDirectional);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.vertices(BOTH, "type").hasNext());
    assertFalse(vertex.vertices(BOTH, "something").hasNext());
  }

  @Test
  public void testPropertiesWithAllProperties() {
    Vertex vertex = createVertex();
    assertTrue(vertex.properties().hasNext());
  }

  @Test
  public void testPropertiesWithoutMatchingProperty() {
    Vertex vertex = createVertex();
    assertFalse(vertex.properties("something").hasNext());
  }

  @Test
  public void testPropertiesWithMatchingProperty() {
    Vertex vertex = createVertex();
    assertTrue(vertex.properties("value").hasNext());
  }

  /* The following tests are adapted from gremlin-test VertexTest. */

  @Test
  public void testValidateEquality() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = createVertex();

    assertEquals(vertex1, vertex1);
    assertEquals(vertex2, vertex2);
    assertNotEquals(vertex1, vertex2);
  }

  @Test
  public void testValidateIdEquality() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = createVertex();

    assertEquals(vertex1.id(), vertex1.id());
    assertEquals(vertex2.id(), vertex2.id());
    assertNotEquals(vertex1.id(), vertex2.id());
  }

  @Test
  public void testStandardStringRepresentation() {
    Vertex vertex = createVertex();
    assertEquals(StringFactory.vertexString(vertex), vertex.toString());
  }

  @Test
  public void testAutotypeStringProperties() {
    Vertex vertex = createVertex();
    String value = vertex.value("value");
    assertEquals("value", value);
  }

  @Test
  public void testGetPropertyKeysOnVertex() {
    Vertex vertex = createVertex();
    // Test that the following properties exists on the vertex.
    Map<String, String> expected = MapUtils.map(
            T("value", "value")
    );

    Set<String> keys = vertex.keys();
    Set<VertexProperty<Object>> properties = SetUtils.set(vertex.properties());

    assertEquals(expected.size(), keys.size());
    assertEquals(expected.size(), properties.size());

    for (Map.Entry<String, String> entry : expected.entrySet()) {
      assertTrue(keys.contains(entry.getKey()));

      VertexProperty<Object> property = vertex.property(entry.getKey());
      assertNotNull(property.id());
      assertEquals(entry.getValue(), property.value());
      assertEquals(property.key(), property.label());
      assertEquals(StringFactory.propertyString(property), property.toString());
      assertSame(vertex, property.element());
    }
  }

  @Test
  public void testReturnEmptyPropertyIfKeyNonExistent() {
    Vertex vertex = createVertex();
    VertexProperty property = vertex.property("something");
    assertEquals(VertexProperty.empty(), property);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetValueThatIsNotPresentOnVertex() {
    Vertex vertex = createVertex();
    vertex.value("something");
  }

  private Vertex createVertex() {
    return new ObjectVertex(getActGraph(), mockObject());
  }

  private UUID mockObjectWithFact(Direction inDirection) {
    UUID objectID = mockObject();
    UUID factID = mockFact(new FactEntity.FactObjectBinding()
            .setObjectID(mockObject())
            .setDirection(swapDirection(inDirection))
    );

    when(getObjectManager().fetchObjectFactBindings(objectID)).thenReturn(list(new ObjectFactBindingEntity()
            .setObjectID(objectID)
            .setFactID(factID)
            .setDirection(inDirection)
    ));

    return objectID;
  }

  private Direction swapDirection(Direction direction) {
    switch (direction) {
      case FactIsSource:
        return Direction.FactIsDestination;
      case FactIsDestination:
        return Direction.FactIsSource;
      default:
        return direction;
    }
  }

}
