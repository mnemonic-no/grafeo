package no.mnemonic.act.platform.dao.tinkerpop.utils;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.tinkerpop.ActGraph;
import no.mnemonic.act.platform.entity.cassandra.*;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElementFactoryTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private FactManager factManager;

  private ElementFactory elementFactory;

  @Before
  public void setup() {
    initMocks(this);

    ActGraph actGraph = ActGraph.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .setHasFactAccess(f -> true)
            .build();
    elementFactory = ElementFactory.builder().setOwner(actGraph).build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateElementFactoryWithoutOwner() {
    ElementFactory.builder().build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateEdgesWithoutBinding() {
    elementFactory.createEdges(null);
  }

  @Test
  public void testCreateEdgesWithoutFact() {
    ObjectFactBindingEntity binding = createInBinding(Direction.None);
    assertTrue(elementFactory.createEdges(binding).isEmpty());
  }

  @Test
  public void testCreateEdgesWithoutFactAccess() {
    ActGraph graph = ActGraph.builder()
            .setObjectManager(objectManager)
            .setFactManager(factManager)
            .setHasFactAccess(f -> false)
            .build();
    ElementFactory factory = ElementFactory.builder().setOwner(graph).build();

    ObjectFactBindingEntity inBinding = createInBinding(Direction.None);
    FactEntity.FactObjectBinding outBinding = createOutBinding(Direction.None);
    mockObject(inBinding);
    mockObject(outBinding.getObjectID());
    mockFact(inBinding.getFactID(), outBinding);

    assertTrue(factory.createEdges(inBinding).isEmpty());
  }

  @Test
  public void testCreateEdgesReturnsLoop() {
    ObjectFactBindingEntity inBinding = createInBinding(Direction.None);
    FactEntity.FactObjectBinding outBinding = new FactEntity.FactObjectBinding()
            .setObjectID(inBinding.getObjectID())
            .setDirection(inBinding.getDirection());
    mockObject(inBinding);
    mockFact(inBinding.getFactID(), outBinding);

    Edge edge = elementFactory.createEdges(inBinding).iterator().next();
    assertSame(edge.inVertex(), edge.outVertex());
  }

  @Test
  public void testCreateEdgesWithDirectionNone() {
    ObjectFactBindingEntity inBinding = createInBinding(Direction.None);
    FactEntity.FactObjectBinding outBinding = createOutBinding(Direction.None);

    Edge edge = mockAndRunCreateEdges(inBinding, outBinding);

    assertEquals(inBinding.getObjectID(), edge.inVertex().id());
    assertEquals(outBinding.getObjectID(), edge.outVertex().id());
  }

  @Test
  public void testCreateEdgesWithDirectionBiDirectional() {
    ObjectFactBindingEntity inBinding = createInBinding(Direction.BiDirectional);
    FactEntity.FactObjectBinding outBinding = createOutBinding(Direction.BiDirectional);

    Edge edge = mockAndRunCreateEdges(inBinding, outBinding);

    assertEquals(inBinding.getObjectID(), edge.inVertex().id());
    assertEquals(outBinding.getObjectID(), edge.outVertex().id());
  }

  @Test
  public void testCreateEdgesWithDirectionFactIsDestination() {
    ObjectFactBindingEntity inBinding = createInBinding(Direction.FactIsDestination);
    FactEntity.FactObjectBinding outBinding = createOutBinding(Direction.FactIsSource);

    Edge edge = mockAndRunCreateEdges(inBinding, outBinding);

    assertEquals(inBinding.getObjectID(), edge.inVertex().id());
    assertEquals(outBinding.getObjectID(), edge.outVertex().id());
  }

  @Test
  public void testCreateEdgesWithDirectionFactIsSource() {
    ObjectFactBindingEntity inBinding = createInBinding(Direction.FactIsSource);
    FactEntity.FactObjectBinding outBinding = createOutBinding(Direction.FactIsDestination);

    Edge edge = mockAndRunCreateEdges(inBinding, outBinding);

    assertEquals(outBinding.getObjectID(), edge.inVertex().id());
    assertEquals(inBinding.getObjectID(), edge.outVertex().id());
  }

  @Test
  public void testCreateEdgesExecutedTwice() {
    ObjectFactBindingEntity inBinding = createInBinding(Direction.None);
    FactEntity.FactObjectBinding outBinding = createOutBinding(Direction.None);

    Edge first = mockAndRunCreateEdges(inBinding, outBinding);
    Edge second = mockAndRunCreateEdges(inBinding, outBinding);

    assertSame(first, second);
  }

  @Test
  public void testGetEdgeNotCached() {
    assertNull(elementFactory.getEdge(UUID.randomUUID()));
  }

  @Test
  public void testGetEdgeCached() {
    ObjectFactBindingEntity inBinding = createInBinding(Direction.None);
    FactEntity.FactObjectBinding outBinding = createOutBinding(Direction.None);

    Edge first = mockAndRunCreateEdges(inBinding, outBinding);
    Edge second = elementFactory.getEdge((UUID) first.id());

    assertSame(first, second);
  }

  @Test
  public void testGetVertexNotExists() {
    assertNull(elementFactory.getVertex(UUID.randomUUID()));
  }

  @Test
  public void testGetVertexNotCached() {
    UUID objectID = mockObject(UUID.randomUUID());
    assertEquals(objectID, elementFactory.getVertex(objectID).id());
  }

  @Test
  public void testGetVertexCached() {
    UUID objectID = mockObject(UUID.randomUUID());

    Vertex first = elementFactory.getVertex(objectID);
    Vertex second = elementFactory.getVertex(objectID);

    assertSame(first, second);
  }

  private Edge mockAndRunCreateEdges(ObjectFactBindingEntity inBinding, FactEntity.FactObjectBinding outBinding) {
    mockObject(inBinding);
    mockObject(outBinding.getObjectID());
    mockFact(inBinding.getFactID(), outBinding);

    return elementFactory.createEdges(inBinding).iterator().next();
  }

  private UUID mockObject(UUID objectID) {
    UUID typeID = UUID.randomUUID();

    when(objectManager.getObject(objectID)).thenReturn(new ObjectEntity()
            .setId(objectID)
            .setTypeID(typeID)
            .setValue("value")
    );

    when(objectManager.getObjectType(typeID)).thenReturn(new ObjectTypeEntity()
            .setId(typeID)
            .setName("type")
    );

    return objectID;
  }

  private void mockObject(ObjectFactBindingEntity inBinding) {
    mockObject(inBinding.getObjectID());
    when(objectManager.fetchObjectFactBindings(inBinding.getObjectID())).thenReturn(ListUtils.list(inBinding));
  }

  private void mockFact(UUID factID, FactEntity.FactObjectBinding outBinding) {
    UUID typeID = UUID.randomUUID();

    when(factManager.getFact(factID)).thenReturn(new FactEntity()
            .setId(factID)
            .setTypeID(typeID)
            .setValue("value")
            .setBindings(ListUtils.list(outBinding))
    );

    when(factManager.getFactType(typeID)).thenReturn(new FactTypeEntity()
            .setId(typeID)
            .setName("type")
    );
  }

  private ObjectFactBindingEntity createInBinding(Direction inDirection) {
    return new ObjectFactBindingEntity()
            .setObjectID(UUID.randomUUID())
            .setFactID(UUID.randomUUID())
            .setDirection(inDirection);
  }

  private FactEntity.FactObjectBinding createOutBinding(Direction outDirection) {
    return new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(outDirection);
  }

}
