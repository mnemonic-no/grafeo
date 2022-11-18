package no.mnemonic.act.platform.service.ti.tinkerpop;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.PropertyHelper;
import org.junit.Before;
import org.mockito.Mock;

import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

abstract class AbstractGraphTest {

  final FactSearchCriteria factSearchCriteria = FactSearchCriteria.builder()
          .setAccessControlCriteria(AccessControlCriteria.builder()
                  .addCurrentUserIdentity(UUID.randomUUID())
                  .addAvailableOrganizationID(UUID.randomUUID())
                  .build())
          .setIndexSelectCriteria(IndexSelectCriteria.builder().build())
          .build();

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

  private ActGraph actGraph;

  @Before
  public void setup() {
    initMocks(this);

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);

    when(propertyHelper.getObjectProperties(any(), any())).thenReturn(list());
    when(propertyHelper.getFactProperties(any(), any())).thenReturn(list());

    actGraph = createActGraph(TraverseParams.builder()
            .setBaseSearchCriteria(factSearchCriteria)
            .build());
  }

  ObjectFactDao getObjectFactDao() {
    return objectFactDao;
  }

  ObjectFactTypeResolver getObjectFactTypeResolver() { return objectFactTypeResolver; }

  FactRetractionHandler getFactRetractionHandler() { return factRetractionHandler; }

  PropertyHelper getPropertyHelper() { return propertyHelper; }

  TiSecurityContext getSecurityContext() { return securityContext; }

  ActGraph getActGraph() {
    return actGraph;
  }

  ActGraph createActGraph(TraverseParams traverseParams) {
    return ActGraph.builder()
            .setObjectFactDao(getObjectFactDao())
            .setObjectTypeFactResolver(getObjectFactTypeResolver())
            .setSecurityContext(getSecurityContext())
            .setFactRetractionHandler(getFactRetractionHandler())
            .setPropertyHelper(getPropertyHelper())
            .setTraverseParams(traverseParams)
            .build();
  }

  ObjectTypeStruct mockObjectType() {
    UUID objectTypeID = UUID.randomUUID();
    ObjectTypeStruct objectTypeStruct = ObjectTypeStruct.builder()
            .setId(objectTypeID)
            .setName("ip")
            .build();
    when(objectFactTypeResolver.toObjectTypeStruct(objectTypeID)).thenReturn(objectTypeStruct);
    return objectTypeStruct;
  }

  ObjectRecord mockObjectRecord(ObjectTypeStruct objectType, String value) {
    UUID objectID = UUID.randomUUID();
    ObjectRecord objectRecord = new ObjectRecord()
            .setId(objectID)
            .setTypeID(objectType.getId())
            .setValue(value);
    when(objectFactDao.getObject(objectID)).thenReturn(objectRecord);
    return objectRecord;
  }

  FactRecord mockFact(ObjectRecord source, ObjectRecord destination) {
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

    return factRecord;
  }

  FactTypeStruct mockFactType(String name) {
    UUID factTypeId = UUID.randomUUID();
    FactTypeStruct factTypeStruct = FactTypeStruct.builder().setId(factTypeId).setName(name).build();
    when(objectFactTypeResolver.toFactTypeStruct(factTypeId)).thenReturn(factTypeStruct);
    return factTypeStruct;
  }
}
