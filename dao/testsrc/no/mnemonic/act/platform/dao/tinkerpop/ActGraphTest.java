package no.mnemonic.act.platform.dao.tinkerpop;

import no.mnemonic.act.platform.dao.tinkerpop.exceptions.GraphOperationException;
import no.mnemonic.act.platform.entity.cassandra.Direction;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectFactBindingEntity;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ActGraphTest extends AbstractGraphTest {

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutObjectManager() {
    ActGraph.builder().setFactManager(getFactManager()).build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateGraphWithoutFactManager() {
    ActGraph.builder().setObjectManager(getObjectManager()).build();
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
    UUID objectID = mockObject();
    Vertex objectVertex = new ObjectVertex(getActGraph(), objectID);
    Iterator<Vertex> result = getActGraph().vertices(objectVertex);
    assertSame(objectVertex, result.next());
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
