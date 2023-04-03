package no.mnemonic.services.grafeo.dao.tinkerpop;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.cassandra.entity.Direction;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectFactBindingEntity;
import no.mnemonic.services.grafeo.dao.tinkerpop.exceptions.GraphOperationException;
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

public class ObjectFactGraphTest extends AbstractGraphTest {

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutObjectManager() {
    ObjectFactGraph.builder()
            .setFactManager(getFactManager())
            .setHasFactAccess(b -> true)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutFactManager() {
    ObjectFactGraph.builder()
            .setObjectManager(getObjectManager())
            .setHasFactAccess(b -> true)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutHasFactAccess() {
    ObjectFactGraph.builder()
            .setObjectManager(getObjectManager())
            .setFactManager(getFactManager())
            .build();
  }

  @Test(expected = GraphOperationException.class)
  public void testFetchingAllVerticesNotAllowed() {
    getGraph().vertices();
  }

  @Test
  public void testFetchingVerticesWithId() {
    UUID objectID = mockObject();
    Iterator<Vertex> result = getGraph().vertices(objectID);
    assertEquals(objectID, result.next().id());
  }

  @Test
  public void testFetchingVerticesWithVertex() {
    Vertex objectVertex = createVertex();
    Iterator<Vertex> result = getGraph().vertices(objectVertex);
    assertEquals(objectVertex, result.next());
  }

  @Test(expected = GraphOperationException.class)
  public void testFetchingAllEdgesNotAllowed() {
    getGraph().edges();
  }

  @Test
  public void testFetchingEdgesWithId() {
    ObjectFactBindingEntity inBinding = mockFactWithObject();
    Set<Edge> expected = getGraph().getElementFactory().createEdges(inBinding);
    Set<Edge> actual = SetUtils.set(getGraph().edges(expected.iterator().next().id()));
    assertEquals(expected, actual);
  }

  @Test
  public void testFetchingEdgesWithEdge() {
    ObjectFactBindingEntity binding = mockFactWithObject();
    Set<Edge> expected = getGraph().getElementFactory().createEdges(binding);
    Set<Edge> actual = SetUtils.set(getGraph().edges(expected.iterator().next()));
    assertEquals(expected, actual);
  }

  /* The following tests are adapted from gremlin-test GraphTest. */

  @Test
  public void testStandardStringRepresentation() {
    assertTrue(getGraph().toString().matches(".*\\[.*\\]"));
  }

  @Test(expected = NoSuchElementException.class)
  public void testHaveExceptionConsistencyWhenFindVertexByIdThatIsNonExistent() {
    getGraph().vertices(UUID.randomUUID());
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingVertex() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getGraph().vertices(vertex1).next();
    assertEquals(vertex1.id(), vertex2.id());
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingDetachedVertex() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getGraph().vertices(DetachedFactory.detach(vertex1, true)).next();
    assertEquals(vertex1.id(), vertex2.id());
    assertFalse(vertex2 instanceof DetachedVertex);
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingReferenceVertex() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getGraph().vertices(ReferenceFactory.detach(vertex1)).next();
    assertEquals(vertex1.id(), vertex2.id());
    assertFalse(vertex2 instanceof ReferenceVertex);
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingStarVertex() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getGraph().vertices(StarGraph.of(vertex1).getStarVertex()).next();
    assertEquals(vertex1.id(), vertex2.id());
    assertFalse(vertex2 instanceof StarGraph.StarVertex);
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingVertexId() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getGraph().vertices(vertex1.id()).next();
    assertEquals(vertex1.id(), vertex2.id());
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingStringRepresentation() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = getGraph().vertices(vertex1.id().toString()).next();
    assertEquals(vertex1.id(), vertex2.id());
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingVertices() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = createVertex();
    assertEquals(2, IteratorUtils.count(getGraph().vertices(vertex1, vertex2)));
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingVertexIds() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = createVertex();
    assertEquals(2, IteratorUtils.count(getGraph().vertices(vertex1.id(), vertex2.id())));
  }

  @Test
  public void testIterateVerticesWithUuidIdSupportUsingStringRepresentations() {
    Vertex vertex1 = createVertex();
    Vertex vertex2 = createVertex();
    assertEquals(2, IteratorUtils.count(getGraph().vertices(vertex1.id().toString(), vertex2.id().toString())));
  }

  @Test(expected = NoSuchElementException.class)
  public void testHaveExceptionConsistencyWhenFindEdgeByIdThatIsNonExistent() {
    getGraph().edges(UUID.randomUUID());
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingEdge() {
    Edge edge1 = createEdge();
    Edge edge2 = getGraph().edges(edge1).next();
    assertEquals(edge1.id(), edge2.id());
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingDetachedEdge() {
    Edge edge1 = createEdge();
    Edge edge2 = getGraph().edges(DetachedFactory.detach(edge1, true)).next();
    assertEquals(edge1.id(), edge2.id());
    assertFalse(edge2 instanceof DetachedEdge);
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingReferenceEdge() {
    Edge edge1 = createEdge();
    Edge edge2 = getGraph().edges(ReferenceFactory.detach(edge1)).next();
    assertEquals(edge1.id(), edge2.id());
    assertFalse(edge2 instanceof ReferenceEdge);
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingEdgeId() {
    Edge edge1 = createEdge();
    Edge edge2 = getGraph().edges(edge1.id()).next();
    assertEquals(edge1.id(), edge2.id());
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingStringRepresentation() {
    Edge edge1 = createEdge();
    Edge edge2 = getGraph().edges(edge1.id().toString()).next();
    assertEquals(edge1.id(), edge2.id());
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingEdges() {
    Edge edge1 = createEdge();
    Edge edge2 = createEdge();
    assertEquals(2, IteratorUtils.count(getGraph().edges(edge1, edge2)));
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingEdgeIds() {
    Edge edge1 = createEdge();
    Edge edge2 = createEdge();
    assertEquals(2, IteratorUtils.count(getGraph().edges(edge1.id(), edge2.id())));
  }

  @Test
  public void testIterateEdgesWithUuidIdSupportUsingStringRepresentations() {
    Edge edge1 = createEdge();
    Edge edge2 = createEdge();
    assertEquals(2, IteratorUtils.count(getGraph().edges(edge1.id().toString(), edge2.id().toString())));
  }

  private Vertex createVertex() {
    return new ObjectVertex(getGraph(), mockObject());
  }

  private Edge createEdge() {
    return getGraph().getElementFactory().createEdges(mockFactWithObject()).iterator().next();
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
