package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.OperationTimeoutException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.FactResponseConverter;
import no.mnemonic.act.platform.service.ti.converters.response.ObjectResponseConverter;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.PropertyHelper;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.PropertyEntry;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TraverseGraphHandlerTest {

  @Mock
  private FactResponseConverter factResponseConverter;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private ObjectResponseConverter objectResponseConverter;
  @Mock
  private ObjectFactTypeResolver objectFactTypeResolver;
  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private FactRetractionHandler factRetractionHandler;
  @Mock
  private PropertyHelper propertyHelper;

  private TraverseGraphHandler handler;

  @Before
  public void setup() {
    initMocks(this);

    when(securityContext.hasReadPermission(isA(FactRecord.class))).thenReturn(true);
    when(securityContext.getCurrentUserID()).thenReturn(new UUID(0, 1));
    when(securityContext.getAvailableOrganizationID()).thenReturn(set(new UUID(0, 1)));
    when(propertyHelper.getOneLeggedFactsAsProperties(any(), any())).thenReturn(list());

    handler = new TraverseGraphHandler(
            securityContext,
            objectFactDao,
            objectFactTypeResolver,
            objectResponseConverter,
            factResponseConverter,
            factRetractionHandler,
            propertyHelper).setScriptExecutionTimeout(5000);
  }

  @Test
  public void testTraverseGraphReturnEdges() throws Exception {
    ObjectRecord source = mockObjectRecord(mockObjectType(), "someValue");
    ObjectRecord destination = mockObjectRecord(mockObjectType(), "someOther");
    FactRecord factRecord = mockFact(source, destination);

    ResultSet<?> resultSet = handler.traverse(set(source.getId()), "g.outE()", TraverseParams.builder().build());

    List<?> result = ListUtils.list(resultSet.iterator());
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof Fact);
    assertEquals(factRecord.getId(), ((Fact) result.get(0)).getId());
  }

  @Test
  public void testTraverseGraphReturnVertices() throws Exception {
    ObjectRecord source = mockObjectRecord(mockObjectType(), "someValue");
    ObjectRecord destination = mockObjectRecord(mockObjectType(), "someOther");
    mockFact(source, destination);

    ResultSet<?> resultSet = handler.traverse(set(source.getId()), "g.out()", TraverseParams.builder().build());

    List<?> result = ListUtils.list(resultSet.iterator());
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof Object);
    assertEquals(destination.getId(), ((Object) result.get(0)).getId());
  }

  @Test
  public void testTraverseGraphReturnValue() throws Exception {
    ObjectRecord source = mockObjectRecord(mockObjectType(), "someValue");
    ObjectRecord destination = mockObjectRecord(mockObjectType(), "someOther");
    mockFact(source, destination);

    ResultSet<?> resultSet = handler.traverse(set(source.getId()), "g.values('value')", TraverseParams.builder().build());

    List<?> result = ListUtils.list(resultSet.iterator());
    assertEquals(1, result.size());
    assertEquals("someValue", result.get(0));
  }

  @Test
  public void testTraverseGraphReturnProperties() throws Exception {
    ObjectRecord source = mockObjectRecord(mockObjectType(), "someValue");
    ObjectRecord destination = mockObjectRecord(mockObjectType(), "someOther");
    mockFact(source, destination);
    when(propertyHelper.getOneLeggedFactsAsProperties(eq(source.getId()), any()))
            .thenReturn(ListUtils.list(
                    new PropertyEntry<>("name", "test"),
                    new PropertyEntry<>("otherProp", "something")));

    ResultSet<?> resultSet = handler.traverse(set(source.getId()), "g.properties()", TraverseParams.builder().build());

    Set<?> result = set(resultSet.iterator());
    assertEquals(3, result.size());
    assertEquals(set("vp[value->someValue]", "vp[name->test]", "vp[otherProp->something]"), result);
  }

  @Test
  public void testTraverseGraphReturnError() {
    ObjectRecord source = mockObjectRecord(mockObjectType(), "someValue");
    ObjectRecord destination = mockObjectRecord(mockObjectType(), "someOther");
    mockFact(source, destination);

    assertThrows(InvalidArgumentException.class, () -> {
      handler.traverse(set(source.getId()), "g.addE('notAllowed')", TraverseParams.builder().build());
    });
  }

  @Test
  public void testTraverseGraphSandboxed() {
    ObjectRecord source = mockObjectRecord(mockObjectType(), "someValue");
    ObjectRecord destination = mockObjectRecord(mockObjectType(), "someOther");
    mockFact(source, destination);

    assertThrows(InvalidArgumentException.class,
            () -> handler.traverse(set(source.getId()), "System.exit(0)", TraverseParams.builder().build()));
  }

  @Test
  public void testTraverseGraphTimeout() {
    ObjectRecord source = mockObjectRecord(mockObjectType(), "someValue");
    ObjectRecord destination = mockObjectRecord(mockObjectType(), "someOther");
    mockFact(source, destination);

    assertThrows(OperationTimeoutException.class,
            () -> handler.traverse(set(source.getId()), "while (true) {}", TraverseParams.builder().build()));
  }

  @Test
  public void testTraverseGraphWithoutStartingObjects() throws Exception {
    ResultSet<?> resultSet = handler.traverse(set(), "g.values('value')", TraverseParams.builder().build());

    List<?> result = ListUtils.list(resultSet.iterator());
    assertEquals(0, result.size());
  }

  @Test
  public void testTraverseGraphWithLimit() throws Exception {
    // Create three vertices and two facts
    // source -> destination1
    // source -> destination2
    ObjectRecord source = mockObjectRecord(mockObjectType(), "sourceValue");
    ObjectRecord destination1 = mockObjectRecord(mockObjectType(), "destination1Value");
    ObjectRecord destination2 = mockObjectRecord(mockObjectType(), "destination2Value");

    UUID fact1ID = UUID.randomUUID();
    FactTypeStruct factTypeStruct = mockFactType("someFactType");
    FactRecord fact1 = new FactRecord()
            .setId(fact1ID)
            .setTypeID(factTypeStruct.getId())
            .setSourceObject(source)
            .setDestinationObject(destination1);
    when(objectFactDao.getFact(fact1ID)).thenReturn(fact1);
    when(factResponseConverter.apply(fact1)).thenReturn(Fact.builder().setId(fact1.getId()).build());

    UUID fact2ID = UUID.randomUUID();
    FactRecord fact2 = new FactRecord()
            .setId(fact2ID)
            .setTypeID(factTypeStruct.getId())
            .setSourceObject(source)
            .setDestinationObject(destination2);
    when(objectFactDao.getFact(fact2ID)).thenReturn(fact2);
    when(factResponseConverter.apply(fact2)).thenReturn(Fact.builder().setId(fact2.getId()).build());

    // Return both facts when searching
    when(objectFactDao.searchFacts(
            argThat(criteria -> criteria.getObjectID().contains(source.getId()))))
            .thenAnswer(x -> ResultContainer.<FactRecord>builder()
                    .setValues(list(fact1, fact2).iterator())
                   .build());

    // Limit the search to 1 edge
    ResultSet<?> resultSet = handler.traverse(
            set(source.getId()),
            "g.outE()",
            TraverseParams.builder().setLimit(1).build());

    // Expect 1 edge due to the limit
    List<?> result = ListUtils.list(resultSet.iterator());
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof Fact);
    // One of the facts must be in the result
    assertTrue(set(fact1ID, fact2ID).contains(((Fact) result.get(0)).getId()));
  }

  private ObjectTypeStruct mockObjectType() {
    UUID objectTypeID = UUID.randomUUID();
    ObjectTypeStruct objectTypeStruct = ObjectTypeStruct.builder()
            .setId(objectTypeID)
            .setName("ip")
            .build();
    when(objectFactTypeResolver.toObjectTypeStruct(objectTypeID)).thenReturn(objectTypeStruct);
    return objectTypeStruct;
  }

  private ObjectRecord mockObjectRecord(ObjectTypeStruct objectType, String value) {
    UUID objectID = UUID.randomUUID();
    ObjectRecord objectRecord = new ObjectRecord()
            .setId(objectID)
            .setTypeID(objectType.getId())
            .setValue(value);
    when(objectFactDao.getObject(objectID)).thenReturn(objectRecord);
    when(objectResponseConverter.apply(objectRecord))
            .thenReturn(
                    Object.builder()
                            .setId(objectRecord.getId())
                            .setValue(objectRecord.getValue())
                            .build());
    return objectRecord;
  }

  private FactRecord mockFact(ObjectRecord source, ObjectRecord destination) {
    UUID factID = UUID.randomUUID();
    FactTypeStruct factTypeStruct = mockFactType("someFactType");

    FactRecord factRecord = new FactRecord()
            .setId(factID)
            .setTypeID(factTypeStruct.getId())
            .setValue("value")
            .setInReferenceToID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000002"))
            .setOriginID(UUID.fromString("00000000-0000-0000-0000-000000000003"))
            .setTrust(0.3f)
            .setConfidence(0.5f)
            .setAccessMode(FactRecord.AccessMode.Public)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setSourceObject(source)
            .setDestinationObject(destination);
    when(objectFactDao.getFact(factID)).thenReturn(factRecord);
    when(factResponseConverter.apply(factRecord)).thenReturn(Fact.builder().setId(factRecord.getId()).build());

    // Always just return this fact
    when(objectFactDao.searchFacts(
            argThat(criteria -> criteria.getObjectID().contains(source.getId()) ||
                    criteria.getObjectID().contains(destination.getId()))))
            .thenAnswer(x -> ResultContainer.<FactRecord>builder()
                    .setValues(list(factRecord).iterator())
                    .build());

    return factRecord;
  }

  private FactTypeStruct mockFactType(String name) {
    UUID factTypeId = UUID.randomUUID();
    FactTypeStruct factTypeStruct = FactTypeStruct.builder().setId(factTypeId).setName(name).build();
    when(objectFactTypeResolver.toFactTypeStruct(factTypeId)).thenReturn(factTypeStruct);
    return factTypeStruct;
  }
}
