package no.mnemonic.services.grafeo.service.implementation.tinkerpop;

import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.handlers.FactRetractionHandler;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ObjectFactTypeResolver;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.PropertyHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
  private GrafeoSecurityContext securityContext;

  private ObjectFactGraph graph;

  @BeforeEach
  public void setup() {
    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);

    when(propertyHelper.getObjectProperties(any(), any())).thenReturn(list());
    when(propertyHelper.getFactProperties(any(), any())).thenReturn(list());

    graph = createGraph(TraverseParams.builder()
            .setBaseSearchCriteria(factSearchCriteria)
            .build());
  }

  ObjectFactDao getObjectFactDao() {
    return objectFactDao;
  }

  ObjectFactTypeResolver getObjectFactTypeResolver() {
    return objectFactTypeResolver;
  }

  FactRetractionHandler getFactRetractionHandler() {
    return factRetractionHandler;
  }

  PropertyHelper getPropertyHelper() {
    return propertyHelper;
  }

  GrafeoSecurityContext getSecurityContext() {
    return securityContext;
  }

  ObjectFactGraph getGraph() {
    return graph;
  }

  ObjectFactGraph createGraph(TraverseParams traverseParams) {
    return ObjectFactGraph.builder()
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
