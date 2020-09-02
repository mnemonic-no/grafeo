package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.tinkerpop.exceptions.GraphOperationException;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ActGraphTest extends AbstractGraphTest {

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutObjectFactDao() {
    ActGraph.builder()
            .setSecurityContext(getSecurityContext())
            .setObjectTypeFactResolver(getObjectFactTypeResolver())
            .setTraverseParams(TraverseParams.builder().build())
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutObjectFactTypeResolver() {
    ActGraph.builder()
            .setObjectFactDao(getObjectFactDao())
            .setSecurityContext(getSecurityContext())
            .setTraverseParams(TraverseParams.builder().build())
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutSecurityContext() {
    ActGraph.builder()
            .setObjectTypeFactResolver(getObjectFactTypeResolver())
            .setObjectFactDao(getObjectFactDao())
            .setTraverseParams(TraverseParams.builder().build())
            .build();
  }

  @Test(expected = GraphOperationException.class)
  public void testFetchingAllVerticesNotAllowed() {
    getActGraph().vertices();
  }

  @Test
  public void testFetchingVerticesWithId() {
    UUID objectID = mockObjectRecord(mockObjectType(), "someValue").getId();
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
    Edge edge = createEdge();

    Set<Edge> expected = SetUtils.set(getActGraph().getElementFactory().getEdge((UUID)edge.id()));
    Set<Edge> actual = SetUtils.set(getActGraph().edges(expected.iterator().next().id()));
    assertEquals(expected, actual);
  }

  @Test
  public void testFetchingEdgesWithEdge() {
    Edge edge = createEdge();

    Set<Edge> expected = SetUtils.set(getActGraph().getElementFactory().getEdge((UUID)edge.id()));
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
    when(getObjectFactDao().searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
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
    ObjectTypeStruct objectType = ObjectTypeStruct.builder()
            .setId(UUID.randomUUID())
            .setName("someObjectType")
            .build();

    ObjectRecord objectRecord = new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(objectType.getId())
            .setValue("someObjectValue");

    when(getObjectFactTypeResolver().toObjectTypeStruct(objectType.getId())).thenReturn(objectType);
    when(getObjectFactDao().getObject(objectRecord.getId())).thenReturn(objectRecord);

    return ObjectVertex.builder()
            .setGraph(getActGraph())
            .setObjectRecord(objectRecord)
            .setObjectType(objectType)
            .build();
  }

  private Edge createEdge() {
    FactRecord fact = mockFact(
            mockObjectRecord(mockObjectType(), "someValue"),
            mockObjectRecord(mockObjectType(), "someOtherValue"));
    return getActGraph().getElementFactory().createEdge(fact, fact.getSourceObject().getId());
  }
}
