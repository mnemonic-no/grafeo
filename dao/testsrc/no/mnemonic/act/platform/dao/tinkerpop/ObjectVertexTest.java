package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.entity.cassandra.Direction;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.Test;

import java.util.Iterator;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
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
  public void testEdgesWithDirectionNone() {
    UUID objectID = mockObjectWithFact(Direction.None);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.edges(BOTH).hasNext());
    assertTrue(vertex.edges(IN).hasNext());
    assertTrue(vertex.edges(OUT).hasNext());
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
  public void testVerticesWithDirectionNone() {
    UUID objectID = mockObjectWithFact(Direction.None);
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    assertTrue(vertex.vertices(BOTH).hasNext());
    assertTrue(vertex.vertices(IN).hasNext());
    assertTrue(vertex.vertices(OUT).hasNext());
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
    UUID objectID = mockObject();
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);
    assertTrue(vertex.properties().hasNext());
  }

  @Test
  public void testPropertiesWithoutMatchingProperty() {
    UUID objectID = mockObject();
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);
    assertFalse(vertex.properties("something").hasNext());
  }

  @Test
  public void testPropertiesWithValueProperty() {
    UUID objectID = mockObject();
    Vertex vertex = new ObjectVertex(getActGraph(), objectID);

    Iterator<VertexProperty<Object>> result = vertex.properties("value");
    assertTrue(result.hasNext());
    VertexProperty<Object> valueProperty = result.next();
    assertEquals("value", valueProperty.key());
    assertEquals("value", valueProperty.value());
    assertTrue(valueProperty.isPresent());
    assertSame(vertex, valueProperty.element());
    assertNotNull(valueProperty.id());
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
        // None or BiDirectional
        return direction;
    }
  }

}
