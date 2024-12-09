package no.mnemonic.services.grafeo.service.implementation.tinkerpop;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.api.result.ResultContainer;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.exceptions.GraphOperationException;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
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
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ObjectFactGraphTest extends AbstractGraphTest {

  @Test
  public void testFetchingAllVerticesNotAllowed() {
    assertThrows(GraphOperationException.class, () -> getGraph().vertices());
  }

  @Test
  public void testFetchingVerticesWithId() {
    UUID objectID = mockObjectRecord(mockObjectType(), "someValue").getId();
    Iterator<Vertex> result = getGraph().vertices(objectID);
    assertEquals(objectID, result.next().id());
  }

  @Test
  public void testFetchingVerticesWithVertex() {
    Vertex objectVertex = createVertex();
    Iterator<Vertex> result = getGraph().vertices(objectVertex);
    assertEquals(objectVertex, result.next());
  }

  @Test
  public void testFetchingAllEdgesNotAllowed() {
    assertThrows(GraphOperationException.class, () -> getGraph().edges());
  }

  @Test
  public void testFetchingEdgesWithId() {
    Edge edge = createEdge();

    Set<Edge> expected = SetUtils.set(getGraph().getElementFactory().getEdge((UUID) edge.id()));
    Set<Edge> actual = SetUtils.set(getGraph().edges(expected.iterator().next().id()));
    assertEquals(expected, actual);
  }

  @Test
  public void testFetchingEdgesWithEdge() {
    Edge edge = createEdge();

    Set<Edge> expected = SetUtils.set(getGraph().getElementFactory().getEdge((UUID) edge.id()));
    Set<Edge> actual = SetUtils.set(getGraph().edges(expected.iterator().next()));

    assertEquals(expected, actual);
  }

  /* The following tests are adapted from gremlin-test GraphTest. */

  @Test
  public void testStandardStringRepresentation() {
    assertTrue(getGraph().toString().matches(".*\\[.*\\]"));
  }

  @Test
  public void testHaveExceptionConsistencyWhenFindVertexByIdThatIsNonExistent() {
    assertThrows(NoSuchElementException.class, () -> getGraph().vertices(UUID.randomUUID()));
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
    when(getObjectFactDao().searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
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

  @Test
  public void testHaveExceptionConsistencyWhenFindEdgeByIdThatIsNonExistent() {
    assertThrows(NoSuchElementException.class, () -> getGraph().edges(UUID.randomUUID()));
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
            .setGraph(getGraph())
            .setObjectRecord(objectRecord)
            .setObjectType(objectType)
            .build();
  }

  private Edge createEdge() {
    FactRecord fact = mockFact(
            mockObjectRecord(mockObjectType(), "someValue"),
            mockObjectRecord(mockObjectType(), "someOtherValue"));
    return getGraph().getElementFactory().createEdge(fact, fact.getSourceObject().getId());
  }
}
