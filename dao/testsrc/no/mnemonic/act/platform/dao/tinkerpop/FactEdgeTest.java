package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import java.util.Iterator;
import java.util.UUID;

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
  public void testPropertiesWithValueProperty() {
    Edge edge = createEdge();
    Iterator<Property<Object>> result = edge.properties("value");
    assertTrue(result.hasNext());
    Property<Object> valueProperty = result.next();
    assertEquals("value", valueProperty.key());
    assertEquals("value", valueProperty.value());
    assertTrue(valueProperty.isPresent());
    assertSame(edge, valueProperty.element());
  }

  private Edge createEdge() {
    UUID factID = mockFact(null);
    UUID inVertexObjectID = mockObject();
    UUID outVertexObjectID = mockObject();
    return new FactEdge(getActGraph(), factID, inVertexObjectID, outVertexObjectID);
  }

}
