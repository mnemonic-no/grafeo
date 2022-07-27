package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.act.platform.service.ti.tinkerpop.ActGraph;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
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
  private ObjectFactDao objectFactDao;
  @Mock
  private ObjectFactTypeResolver objectFactTypeResolver;
  @Mock
  private FactRetractionHandler factRetractionHandler;
  @Mock
  private PropertyHelper propertyHelper;
  @Mock
  private TiSecurityContext securityContext;

  private ElementFactory elementFactory;

  @Before
  public void setup() {
    initMocks(this);

    ActGraph actGraph = ActGraph.builder()
            .setObjectFactDao(objectFactDao)
            .setObjectTypeFactResolver(objectFactTypeResolver)
            .setSecurityContext(securityContext)
            .setFactRetractionHandler(factRetractionHandler)
            .setPropertyHelper(propertyHelper)
            .setTraverseParams(TraverseParams.builder().build())
            .build();
    elementFactory = ElementFactory.builder().setOwner(actGraph).build();
  }

  @Test
  public void testCreateElementFactoryWithoutOwner() {
    assertThrows(RuntimeException.class, () -> ElementFactory.builder().build());
  }

  @Test
  public void testToEdgeWithoutFact() {
    assertNull(elementFactory.createEdge(null, null));
    assertNull(elementFactory.getEdge(null));
  }

  @Test
  public void testCreateEdge() {
    FactTypeStruct factTypeMock = mockFactType();
    ObjectTypeStruct objectTypeMock = mockObjectType();
    ObjectRecord source = mockObject(objectTypeMock);
    ObjectRecord destination = mockObject(objectTypeMock);

    Edge edge = elementFactory.createEdge(new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(factTypeMock.getId())
            .setBidirectionalBinding(false)
            .setSourceObject(source)
            .setDestinationObject(destination),
            source.getId());

    assertEquals(source.getId(), edge.inVertex().id());
    assertEquals(destination.getId(), edge.outVertex().id());
  }

  @Test
  public void testCreateBidirectionalEdge() {
    FactTypeStruct factTypeMock = mockFactType();
    ObjectTypeStruct objectTypeMock = mockObjectType();
    ObjectRecord source = mockObject(objectTypeMock);
    ObjectRecord destination = mockObject(objectTypeMock);

    // The fact record is bidirectional, and you are standing at source
    Edge edge = elementFactory.createEdge(new FactRecord()
                    .setId(UUID.randomUUID())
                    .setTypeID(factTypeMock.getId())
                    .setBidirectionalBinding(true)
                    .setDestinationObject(source)
                    .setSourceObject(destination),
            source.getId());

    assertEquals(source.getId(), edge.inVertex().id());
    assertEquals(destination.getId(), edge.outVertex().id());
  }

  @Test
  public void testCreateBidirectionalEdgeFlipping() {
    FactTypeStruct factTypeMock = mockFactType();
    ObjectTypeStruct objectTypeMock = mockObjectType();
    ObjectRecord source = mockObject(objectTypeMock);
    ObjectRecord destination = mockObject(objectTypeMock);

    // The fact record is bidirectional, and you are standing at destination. Then the edge is flipped
    Edge edge = elementFactory.createEdge(new FactRecord()
                    .setId(UUID.randomUUID())
                    .setTypeID(factTypeMock.getId())
                    .setBidirectionalBinding(true)
                    .setDestinationObject(source)
                    .setSourceObject(destination),
            destination.getId());

    assertEquals(source.getId(), edge.outVertex().id());
    assertEquals(destination.getId(), edge.inVertex().id());
  }

  @Test
  public void testCreateEdgesExecutedTwice() {
    UUID edgeID = UUID.randomUUID();
    FactTypeStruct factTypeMock = mockFactType();
    ObjectTypeStruct objectTypeMock = mockObjectType();
    ObjectRecord objectSource = mockObject(objectTypeMock);
    ObjectRecord objectDestination = mockObject(objectTypeMock);

    Edge first = elementFactory.createEdge(new FactRecord()
            .setId(edgeID)
            .setTypeID(factTypeMock.getId())
            .setSourceObject(objectSource)
            .setDestinationObject(objectDestination),
            objectSource.getId());

    Edge second = elementFactory.createEdge(new FactRecord()
            .setId(edgeID)
            .setTypeID(factTypeMock.getId())
            .setSourceObject(objectSource)
            .setDestinationObject(objectDestination),
            objectSource.getId());

    assertSame(first, second);
  }

  @Test
  public void testGetEdgeWithNullId() {
    assertNull(elementFactory.getEdge(null));
  }

  @Test
  public void testGetEdgeNotCached() {
    assertNull(elementFactory.getEdge(UUID.randomUUID()));
  }

  @Test
  public void testGetEdgeCached() {
    FactTypeStruct factTypeMock = mockFactType();
    ObjectTypeStruct objectTypeMock = mockObjectType();
    ObjectRecord objectSource = mockObject(objectTypeMock);
    ObjectRecord objectDestination = mockObject(objectTypeMock);

    Edge first = elementFactory.createEdge(new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(factTypeMock.getId())
            .setSourceObject(objectSource)
            .setDestinationObject(objectDestination),
            objectSource.getId());

    Edge second = elementFactory.getEdge((UUID) first.id());

    assertSame(first, second);
  }

  @Test
  public void testGetVertexWithNullId() {
    assertNull(elementFactory.getVertex(null));
  }

  @Test
  public void testGetVertexNotExists() {
    assertNull(elementFactory.getVertex(UUID.randomUUID()));
  }

  @Test
  public void testGetVertexNotCached() {
    ObjectTypeStruct objectTypeMock = mockObjectType();
    ObjectRecord objectSource = mockObject(objectTypeMock);

    UUID objectID = objectSource.getId();

    assertEquals(objectID, elementFactory.getVertex(objectID).id());
  }

  @Test
  public void testGetVertexCached() {
    ObjectTypeStruct objectTypeMock = mockObjectType();
    ObjectRecord objectSource = mockObject(objectTypeMock);

    UUID objectID = objectSource.getId();
    Vertex first = elementFactory.getVertex(objectID);
    Vertex second = elementFactory.getVertex(objectID);

    assertSame(first, second);
  }

  private ObjectTypeStruct mockObjectType() {
    UUID objectTypeId = UUID.randomUUID();
    ObjectTypeStruct objectType = ObjectTypeStruct.builder()
            .setId(objectTypeId)
            .setName("someObjectType")
            .build();
    when(objectFactTypeResolver.toObjectTypeStruct(objectTypeId)).thenReturn(objectType);
    return objectType;
  }

  private FactTypeStruct mockFactType() {
    UUID factTypeId = UUID.randomUUID();
    FactTypeStruct factType = FactTypeStruct.builder()
            .setId(factTypeId)
            .setName("someFactType")
            .build();
    when(objectFactTypeResolver.toFactTypeStruct(factTypeId))
            .thenReturn(factType);
    return factType;
  }

  private ObjectRecord mockObject(ObjectTypeStruct objectTypeStruct) {
    ObjectRecord object = new ObjectRecord().setTypeID(objectTypeStruct.getId()).setId(UUID.randomUUID()).setValue("test");
    when(objectFactDao.getObject(object.getId())).thenReturn(object);
    return object;
  }
}
