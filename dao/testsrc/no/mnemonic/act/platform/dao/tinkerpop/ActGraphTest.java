package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.tinkerpop.exceptions.GraphOperationException;
import no.mnemonic.act.platform.entity.cassandra.Direction;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedEdge;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedFactory;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertex;
import org.apache.tinkerpop.gremlin.structure.util.reference.ReferenceEdge;
import org.apache.tinkerpop.gremlin.structure.util.reference.ReferenceFactory;
import org.apache.tinkerpop.gremlin.structure.util.reference.ReferenceVertex;
import org.apache.tinkerpop.gremlin.structure.util.star.StarGraph;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

public class ActGraphTest extends AbstractGraphTest {

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutObjectManager() {
    ActGraph.builder()
            .setFactManager(getFactManager())
            .setHasFactAccess(b -> true)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutFactManager() {
    ActGraph.builder()
            .setObjectManager(getObjectManager())
            .setHasFactAccess(b -> true)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutHasFactAccess() {
    ActGraph.builder()
            .setObjectManager(getObjectManager())
            .setFactManager(getFactManager())
            .build();
  }

  @Test(expected = GraphOperationException.class)
  public void testFetchingAllVerticesNotAllowed() {
    getActGraph().vertices();
  }

  @Test
  public void testFetchingVerticesWithId() {
    UUID objectID = mockObject();
    Iterator<Vertex> result = getActGraph().vertices(objectID);
    assertEquals(objectID, result.next().id());
  }

  @Test
  public void testFetchingVerticesWithVertex() {
    Vertex objectVertex = createVertex();
    Iterator<Vertex> result = getActGraph().vertices(objectVertex);
    assertEquals(objectVertex, result.next());
  }

  @Test(expected = GraphOperationException.class)
  public void testFetchingAllEdgesNotAllowed() {
    getActGraph().edges();
  }

  @Test
  public void testFetchingEdgesWithId() {
    ObjectFactBindingEntity inBinding = mockFactWithObject();
    Set<Edge> expected = getActGraph().getElementFactory().createEdges(inBinding);
    Set<Edge> actual = SetUtils.set(getActGraph().edges(expected.iterator().next().id()));
    assertEquals(expected, actual);
  }

  @Test
  public void testFetchingEdgesWithEdge() {
    ObjectFactBindingEntity binding = mockFactWithObject();
    Set<Edge> expected = getActGraph().getElementFactory().createEdges(binding);
    Set<Edge> actual = SetUtils.set(getActGraph().edges(expected.iterator().next()));
    assertEquals(expected, actual);
  }

  /* The following tests are adapted from gremlin-test GraphTest. */

  @Test
  public void testStandardStringRepresentation() {
    assertTrue(getActGraph().toString().matches(".*\\[.*\\]"));
  }

  @Test(expected = NoSuchElementException.class)
  public void testHaveExceptionConsistencyWhenFindVertexByIdThatIsNonExistent() {
    getActGraph().vertices(UUID.randomUUID());
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingVertex() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getActGraph().vertices(vertex1).next();
    assertEquals(vertex1.id(), vertex2.id());
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingDetachedVertex() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getActGraph().vertices(DetachedFactory.detach(vertex1, true)).next();
    assertEquals(vertex1.id(), vertex2.id());
    assertFalse(vertex2 instanceof DetachedVertex);
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingReferenceVertex() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getActGraph().vertices(ReferenceFactory.detach(vertex1)).next();
    assertEquals(vertex1.id(), vertex2.id());
    assertFalse(vertex2 instanceof ReferenceVertex);
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingStarVertex() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getActGraph().vertices(StarGraph.of(vertex1).getStarVertex()).next();
    assertEquals(vertex1.id(), vertex2.id());
    assertFalse(vertex2 instanceof StarGraph.StarVertex);
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingVertexId() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getActGraph().vertices(vertex1.id()).next();
    assertEquals(vertex1.id(), vertex2.id());
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingStringRepresentation() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getActGraph().vertices(vertex1.id().toString()).next();
    assertEquals(vertex1.id(), vertex2.id());
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingVertices() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = createVertex();
    assertEquals(2, IteratorUtils.count(getActGraph().vertices(vertex1, vertex2)));
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingVertexIds() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = createVertex();
    assertEquals(2, IteratorUtils.count(getActGraph().vertices(vertex1.id(), vertex2.id())));
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingStringRepresentations() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = createVertex();
    assertEquals(2, IteratorUtils.count(getActGraph().vertices(vertex1.id().toString(), vertex2.id().toString())));
  }

  @Test(expected = NoSuchElementException.class)
  public void testHaveExceptionConsistencyWhenFindEdgeByIdThatIsNonExistent() {
    getActGraph().edges(UUID.randomUUID());
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingEdge() {
    Edge edge1 = createEdge();
    Edge edge2 = getActGraph().edges(edge1).next();
    assertEquals(edge1.id(), edge2.id());
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingDetachedEdge() {
    Edge edge1 = createEdge();
    Edge edge2 = getActGraph().edges(DetachedFactory.detach(edge1, true)).next();
    assertEquals(edge1.id(), edge2.id());
    assertFalse(edge2 instanceof DetachedEdge);
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingReferenceEdge() {
    Edge edge1 = createEdge();
    Edge edge2 = getActGraph().edges(ReferenceFactory.detach(edge1)).next();
    assertEquals(edge1.id(), edge2.id());
    assertFalse(edge2 instanceof ReferenceEdge);
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingEdgeId() {
    Edge edge1 = createEdge();
    Edge edge2 = getActGraph().edges(edge1.id()).next();
    assertEquals(edge1.id(), edge2.id());
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingStringRepresentation() {
    Edge edge1 = createEdge();
    Edge edge2 = getActGraph().edges(edge1.id().toString()).next();
    assertEquals(edge1.id(), edge2.id());
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingEdges() {
    Edge edge1 = createEdge();
    Edge edge2 = createEdge();
    assertEquals(2, IteratorUtils.count(getActGraph().edges(edge1, edge2)));
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingEdgeIds() {
    Edge edge1 = createEdge();
    Edge edge2 = createEdge();
    assertEquals(2, IteratorUtils.count(getActGraph().edges(edge1.id(), edge2.id())));
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingStringRepresentations() {
    Edge edge1 = createEdge();
    Edge edge2 = createEdge();
    assertEquals(2, IteratorUtils.count(getActGraph().edges(edge1.id().toString(), edge2.id().toString())));
  }

  private Vertex createVertex() {
    return new ObjectVertex(getActGraph(), mockObject());
  }

  private Edge createEdge() {
    return getActGraph().getElementFactory().createEdges(mockFactWithObject()).iterator().next();
  }

  private ObjectFactBindingEntity mockFactWithObject() {
    UUID objectID = mockObject();
    UUID factID = mockFact(new FactEntity.FactObjectBinding()
            .setDirection(Direction.BiDirectional)
            .setObjectID(objectID)
    );

    return new ObjectFactBindingEntity()
            .setFactID(factID)
            .setObjectID(objectID)
            .setDirection(Direction.BiDirectional);
  }

}
