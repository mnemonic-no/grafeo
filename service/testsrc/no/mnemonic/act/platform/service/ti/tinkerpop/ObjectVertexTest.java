package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.commons.utilities.collections.MapUtils;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.apache.tinkerpop.gremlin.structure.Direction.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

public class ObjectVertexTest extends AbstractGraphTest {

  @Test(expected = RuntimeException.class)
  public void testCreateVertexWithoutGraph() {
    new ObjectVertex(null, new ObjectRecord(), ObjectTypeStruct.builder().build());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateVertexWithoutObject() {
    new ObjectVertex(getActGraph(), null, ObjectTypeStruct.builder().build());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateVertexWithoutObjectType() {
    new ObjectVertex(getActGraph(), new ObjectRecord(), null);
  }

  @Test
  public void testCreateVertexFromObject() {
    ObjectRecord object = new ObjectRecord().setTypeID(UUID.randomUUID());
    ObjectTypeStruct type = ObjectTypeStruct.builder().setName("someObjectType").setId(UUID.randomUUID()).build();
    Vertex vertex = new ObjectVertex(getActGraph(), object, type);

    assertEquals(object.getId(), vertex.id());
    assertEquals("someObjectType", vertex.label());
    assertSame(getActGraph(), vertex.graph());
  }

  @Test
  public void testEdgesWithDirectionBiDirectional() {
    ObjectTypeStruct objectType = mockObjectType();
    ObjectRecord source = mockObjectRecord(objectType, "someValue");
    ObjectRecord destination = mockObjectRecord(objectType, "someOthervalue");

    FactRecord bidirectionalFact = new FactRecord()
            .setBidirectionalBinding(true)
            .setId(UUID.randomUUID())
            .setTypeID(mockFactType("someType").getId())
            .setValue("someValue")
            .setSourceObject(source)
            .setDestinationObject(destination);

    when(getActGraph().getObjectFactDao().searchFacts(notNull())).thenAnswer(
            x -> ResultContainer.<FactRecord>builder().setValues(list(bidirectionalFact).iterator()).build()
    );

    Vertex vertex = new ObjectVertex(getActGraph(), source, objectType);

    assertTrue(vertex.edges(BOTH).hasNext());
    assertTrue(vertex.edges(OUT).hasNext());
    assertTrue(vertex.edges(IN).hasNext());
  }

  @Test
  public void testEdgesWithDirectionOutFromSource() {
    ObjectTypeStruct objectType = mockObjectType();
    ObjectRecord source = mockObjectRecord(objectType, "someValue");
    ObjectRecord destination = mockObjectRecord(objectType, "someOtherValue");

    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(mockFactType("someType").getId())
            .setValue("someValue")
            .setSourceObject(source)
            .setDestinationObject(destination);

    when(getActGraph().getObjectFactDao().searchFacts(notNull())).thenAnswer(
            x -> ResultContainer.<FactRecord>builder().setValues(list(factRecord).iterator()).build()
    );

    Vertex vertex = new ObjectVertex(getActGraph(), source, objectType);

    assertTrue(vertex.edges(BOTH).hasNext());
    assertFalse(vertex.edges(IN).hasNext());
    assertTrue(vertex.edges(OUT).hasNext());
  }

  @Test
  public void testEdgesWithDirectionInToDestination() {
    ObjectTypeStruct objectType = mockObjectType();
    ObjectRecord source = mockObjectRecord(objectType, "someValue");
    ObjectRecord destination = mockObjectRecord(objectType, "someOtherValue");

    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(mockFactType("someType").getId())
            .setValue("someValue")
            .setSourceObject(source)
            .setDestinationObject(destination);

    when(getActGraph().getObjectFactDao().searchFacts(notNull())).thenAnswer(
            x -> ResultContainer.<FactRecord>builder().setValues(list(factRecord).iterator()).build()
    );

    Vertex vertex = new ObjectVertex(getActGraph(), destination, objectType);

    assertTrue(vertex.edges(BOTH).hasNext());
    assertTrue(vertex.edges(IN).hasNext());
    assertFalse(vertex.edges(OUT).hasNext());
  }

  @Test
  public void testEdgesFilterByLabel() {
    ObjectTypeStruct objectType = mockObjectType();
    ObjectRecord source = mockObjectRecord(objectType, "someValue");
    ObjectRecord destination = mockObjectRecord(objectType, "someOthervalue");

    FactTypeStruct bidirectionalFact = mockFactType("someFactType");
    FactRecord factRecord = new FactRecord()
            .setBidirectionalBinding(true)
            .setId(UUID.randomUUID())
            .setTypeID(bidirectionalFact.getId())
            .setValue("someValue")
            .setSourceObject(source)
            .setDestinationObject(destination);

    when(getActGraph().getObjectFactTypeResolver().factTypeNamesToIds(set(bidirectionalFact.getName())))
            .thenReturn(set(bidirectionalFact.getId()));

    when(getActGraph().getObjectFactDao().searchFacts(any()))
            .thenAnswer(invocation -> {
              FactSearchCriteria argument = invocation.getArgument(0);
              if (argument.getFactTypeID().equals(set(bidirectionalFact.getId()))) {
                return ResultContainer.<FactRecord>builder().setValues(list(factRecord).iterator()).build();
              }
              return ResultContainer.<FactRecord>builder().build();
            });

    Vertex vertex = new ObjectVertex(getActGraph(), source, objectType);

    assertTrue(vertex.edges(BOTH, "someFactType").hasNext());
    assertFalse(vertex.edges(BOTH, "nonExistingType").hasNext());
  }

  @Test
  public void testVerticesWithDirectionBiDirectional() {
    ObjectTypeStruct objectType = mockObjectType();
    ObjectRecord source = mockObjectRecord(objectType, "someValue");
    ObjectRecord destination = mockObjectRecord(objectType, "someOthervalue");

    FactRecord bidirectionalFact = new FactRecord()
            .setBidirectionalBinding(true)
            .setId(UUID.randomUUID())
            .setTypeID(mockFactType("someType").getId())
            .setValue("someValue")
            .setSourceObject(source)
            .setDestinationObject(destination);

    when(getActGraph().getObjectFactDao().searchFacts(notNull())).thenAnswer(
            x -> ResultContainer.<FactRecord>builder().setValues(list(bidirectionalFact).iterator()).build()
    );

    Vertex vertex = new ObjectVertex(getActGraph(), source, objectType);

    assertTrue(vertex.vertices(BOTH).hasNext());
    assertTrue(vertex.vertices(OUT).hasNext());
    assertTrue(vertex.vertices(IN).hasNext());
  }

  @Test
  public void testVerticesWithDirectionOutFromSource() {
    ObjectTypeStruct objectType = mockObjectType();
    ObjectRecord source = mockObjectRecord(objectType, "someValue");
    ObjectRecord destination = mockObjectRecord(objectType, "someOtherValue");

    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(mockFactType("someType").getId())
            .setValue("someValue")
            .setSourceObject(source)
            .setDestinationObject(destination);

    when(getActGraph().getObjectFactDao().searchFacts(notNull())).thenAnswer(
            x -> ResultContainer.<FactRecord>builder().setValues(list(factRecord).iterator()).build()
    );

    Vertex vertex = new ObjectVertex(getActGraph(), source, objectType);

    assertTrue(vertex.vertices(BOTH).hasNext());
    assertFalse(vertex.vertices(IN).hasNext());
    assertTrue(vertex.vertices(OUT).hasNext());
  }

  @Test
  public void testVerticesWithDirectionInToDestination() {
    ObjectTypeStruct objectType = mockObjectType();
    ObjectRecord source = mockObjectRecord(objectType, "someValue");
    ObjectRecord destination = mockObjectRecord(objectType, "someOtherValue");

    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(mockFactType("someType").getId())
            .setValue("someValue")
            .setSourceObject(source)
            .setDestinationObject(destination);

    when(getActGraph().getObjectFactDao().searchFacts(notNull())).thenAnswer(
            x -> ResultContainer.<FactRecord>builder().setValues(list(factRecord).iterator()).build()
    );

    Vertex vertex = new ObjectVertex(getActGraph(), destination, objectType);

    assertTrue(vertex.vertices(BOTH).hasNext());
    assertTrue(vertex.vertices(IN).hasNext());
    assertFalse(vertex.vertices(OUT).hasNext());
  }


  @Test
  public void testVerticesFilterByLabel() {

    ObjectTypeStruct objectType = mockObjectType();
    ObjectRecord source = mockObjectRecord(objectType, "someValue");
    ObjectRecord destination = mockObjectRecord(objectType, "someOthervalue");

    FactTypeStruct factTypeStruct = mockFactType("someFactType");
    FactRecord bidirectionalFact = new FactRecord()
            .setBidirectionalBinding(true)
            .setId(UUID.randomUUID())
            .setTypeID(factTypeStruct.getId())
            .setValue("someValue")
            .setSourceObject(source)
            .setDestinationObject(destination);

    when(getActGraph().getObjectFactTypeResolver().factTypeNamesToIds(set(factTypeStruct.getName())))
            .thenReturn(set(factTypeStruct.getId()));

    when(getActGraph().getObjectFactDao().searchFacts(any()))
            .thenAnswer(invocation -> {
              FactSearchCriteria argument = invocation.getArgument(0);
              if (argument.getFactTypeID().equals(set(factTypeStruct.getId()))) {
                return ResultContainer.<FactRecord>builder().setValues(list(bidirectionalFact).iterator()).build();
              }
              return ResultContainer.<FactRecord>builder().build();
            });

    Vertex vertex = new ObjectVertex(getActGraph(), source, objectType);

    assertTrue(vertex.vertices(BOTH, "someFactType").hasNext());
    assertFalse(vertex.vertices(BOTH, "nonExistingType").hasNext());
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
    assertEquals("someObjectValue", value);
  }

  @Test
  public void testGetPropertyKeysOnVertex() {
    Vertex vertex = createVertex();
    // Test that the following properties exists on the vertex.
    Map<String, String> expected = MapUtils.map(
            T("value", "someObjectValue")
    );

    Set<String> keys = vertex.keys();
    Set<VertexProperty<Object>> properties = set(vertex.properties());

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

  @Test
  public void testMatchesDirectionBidirectional() {

    ObjectRecord objectA = new ObjectRecord().setId(UUID.randomUUID());
    ObjectRecord objectB = new ObjectRecord().setId(UUID.randomUUID());

    // objectA <-- fact --> objectB
    FactRecord bidirectionalFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setBidirectionalBinding(true)
            .setSourceObject(objectA)
            .setDestinationObject(objectB);

    assertTrue(ObjectVertex.matchesDirection(bidirectionalFact, objectA, OUT));
    assertTrue(ObjectVertex.matchesDirection(bidirectionalFact, objectA, IN));
    assertTrue(ObjectVertex.matchesDirection(bidirectionalFact, objectA, BOTH));
  }

  @Test
  public void testMatchesDirectionIgnoresLoops() {
    ObjectRecord objectA = new ObjectRecord().setId(UUID.randomUUID());

    // objectA --- fact ---> objectA
    FactRecord loopFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setSourceObject(objectA)
            .setDestinationObject(objectA);

    assertFalse(ObjectVertex.matchesDirection(loopFact, objectA, OUT));
    assertFalse(ObjectVertex.matchesDirection(loopFact, objectA, IN));
    assertFalse(ObjectVertex.matchesDirection(loopFact, objectA, BOTH));
  }

  @Test
  public void testMatchesDirectionIgnoresOneLeggedFacts() {
    ObjectRecord objectA = new ObjectRecord().setId(UUID.randomUUID());

    // fact ---> objectA
    FactRecord loopFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setDestinationObject(objectA);

    assertFalse(ObjectVertex.matchesDirection(loopFact, objectA, OUT));
    assertFalse(ObjectVertex.matchesDirection(loopFact, objectA, IN));
    assertFalse(ObjectVertex.matchesDirection(loopFact, objectA, BOTH));
  }


  @Test
  public void testMatchesDirectionUniDir() {
    ObjectRecord objectA = new ObjectRecord().setId(UUID.randomUUID());
    ObjectRecord objectB = new ObjectRecord().setId(UUID.randomUUID());

    // objectA -- fact --> objectB
    FactRecord aToBFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setSourceObject(objectA)
            .setDestinationObject(objectB);

    assertTrue(ObjectVertex.matchesDirection(aToBFact, objectA, OUT));
    assertFalse(ObjectVertex.matchesDirection(aToBFact, objectA, IN));
    assertTrue(ObjectVertex.matchesDirection(aToBFact, objectA, BOTH));

    // objectA <-- fact -- objectB
    FactRecord bToAFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setSourceObject(objectB)
            .setDestinationObject(objectA);

    assertFalse(ObjectVertex.matchesDirection(bToAFact, objectA, OUT));
    assertTrue(ObjectVertex.matchesDirection(bToAFact, objectA, IN));
    assertTrue(ObjectVertex.matchesDirection(bToAFact, objectA, BOTH));
  }

  @Test
  public void testEdgesWithoutAccess() {
    ActGraph actGraph = ActGraph.builder()
            .setObjectFactDao(getObjectFactDao())
            .setObjectTypeFactResolver(getObjectFactTypeResolver())
            .setSecurityContext(getSecurityContext())
            .setTraverseParams(TraverseParams.builder().build())
            .build();

    ObjectTypeStruct objectType = mockObjectType();
    ObjectRecord source = mockObjectRecord(objectType, "someValue");
    ObjectRecord destination = mockObjectRecord(objectType, "someOthervalue");

    FactRecord bidirectionalFact = new FactRecord()
            .setBidirectionalBinding(true)
            .setId(UUID.randomUUID())
            .setTypeID(mockFactType("someType").getId())
            .setValue("someValue")
            .setSourceObject(source)
            .setDestinationObject(destination);

    when(getSecurityContext().hasReadPermission(any(FactRecord.class))).thenReturn(false);
    when(getObjectFactDao().searchFacts(notNull())).thenAnswer(
            x -> ResultContainer.<FactRecord>builder().setValues(list(bidirectionalFact).iterator()).build()
    );

    Vertex vertex = new ObjectVertex(actGraph, source, objectType);

    assertFalse(vertex.edges(BOTH).hasNext());
    assertFalse(vertex.edges(OUT).hasNext());
    assertFalse(vertex.edges(IN).hasNext());
  }

  private Vertex createVertex() {
    ObjectTypeStruct objectTypeStruct = ObjectTypeStruct.builder()
            .setId(UUID.randomUUID())
            .setName("someObjectType")
            .build();

    ObjectRecord object = new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(objectTypeStruct.getId())
            .setValue("someObjectValue");

    return new ObjectVertex(getActGraph(), object, objectTypeStruct);
  }
}
