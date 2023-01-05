package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.IndexSelectCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.act.platform.service.ti.resolvers.response.OrganizationByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.resolvers.response.OriginByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.resolvers.response.SubjectByIdResponseResolver;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PropertyHelperTest {

  private final AccessControlCriteria accessControlCriteria = AccessControlCriteria.builder()
          .addCurrentUserIdentity(UUID.randomUUID())
          .addAvailableOrganizationID(UUID.randomUUID())
          .build();
  private final IndexSelectCriteria indexSelectCriteria = IndexSelectCriteria.builder().build();
  private final FactSearchCriteria factSearchCriteria = FactSearchCriteria.builder()
          .setAccessControlCriteria(accessControlCriteria)
          .setIndexSelectCriteria(indexSelectCriteria)
          .build();
  private final TraverseParams emptyTraverseParams = TraverseParams.builder()
          .setBaseSearchCriteria(factSearchCriteria)
          .build();

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private ObjectFactTypeResolver objectFactTypeResolver;
  @Mock
  private FactRetractionHandler factRetractionHandler;
  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private SubjectByIdResponseResolver subjectResolver;
  @Mock
  private OrganizationByIdResponseResolver organizationResolver;
  @Mock
  private OriginByIdResponseResolver originResolver;

  private PropertyHelper helper;

  @Before
  public void setUp() {
    initMocks(this);
    helper = new PropertyHelper(factRetractionHandler, objectFactDao, objectFactTypeResolver, securityContext,
            subjectResolver, organizationResolver, originResolver);
    when(factRetractionHandler.isRetracted(any())).thenReturn(false);
  }

  @Test
  public void testOneLeggedFactsAsPropsNoOneLeggedFacts() {
    when(objectFactDao.searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
    assertEquals(0, helper.getOneLeggedFactsAsProperties(new ObjectRecord(), emptyTraverseParams).size());
  }

  @Test
  public void testOneLeggedFactsAsPropsCanGetProperties() {
    UUID objectId = UUID.randomUUID();
    ObjectRecord objectRecord = new ObjectRecord().setId(objectId);

    FactTypeStruct factType1 = mockFactType("factType1");
    FactTypeStruct factType2 = mockFactType("factType2");

    FactRecord fact1 = new FactRecord().setTypeID(factType1.getId()).setValue("factValue1");
    FactRecord fact2 = new FactRecord().setTypeID(factType2.getId()).setValue("factValue2");

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getObjectID(), set(objectId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(fact1, fact2).iterator()).build()
    );

    List<PropertyEntry<?>> props = helper.getOneLeggedFactsAsProperties(
            objectRecord,
            traverseParamsBuilder().setIncludeRetracted(true).build());

    assertEquals(2, props.size());
    assertEquals(set("factType1->factValue1", "factType2->factValue2"), asKeyValueStrings(props));
  }

  @Test
  public void testOneLeggedFactsAsPropsWithoutReadPermission() {
    UUID objectId = UUID.randomUUID();
    ObjectRecord objectRecord = new ObjectRecord().setId(objectId);

    FactTypeStruct factType1 = mockFactType("factType1");
    FactTypeStruct factType2 = mockFactType("factType2");

    FactRecord fact1 = new FactRecord().setTypeID(factType1.getId()).setValue("factValue1");
    FactRecord fact2 = new FactRecord().setTypeID(factType2.getId()).setValue("factValue2");

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(false);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getObjectID(), set(objectId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(fact1, fact2).iterator()).build()
    );

    assertEquals(0, helper.getOneLeggedFactsAsProperties(objectRecord, emptyTraverseParams).size());
    verify(securityContext).hasReadPermission(fact1);
    verify(securityContext).hasReadPermission(fact2);
  }

  @Test
  public void testOneLeggedFactsAsPropsFilterRetracted() {
    UUID objectId = UUID.randomUUID();
    ObjectRecord objectRecord = new ObjectRecord().setId(objectId);
    FactTypeStruct factType = mockFactType("factType");

    FactRecord retractedFact = new FactRecord().setTypeID(factType.getId()).setValue("factValue1");
    FactRecord fact = new FactRecord().setTypeID(factType.getId()).setValue("factValue2");

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getObjectID(), set(objectId))))).thenAnswer(x ->
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(retractedFact, fact).iterator()).build()
    );
    when(factRetractionHandler.isRetracted(retractedFact)).thenReturn(true);
    when(factRetractionHandler.isRetracted(fact)).thenReturn(false);

    assertEquals(2, helper.getOneLeggedFactsAsProperties(objectRecord, traverseParamsBuilder().setIncludeRetracted(true).build()).size());
    assertEquals(1, helper.getOneLeggedFactsAsProperties(objectRecord, traverseParamsBuilder().setIncludeRetracted(false).build()).size());
  }

  @Test
  public void testOneLeggedFactsPassesSearchCriteria() {
    UUID objectId = UUID.randomUUID();
    when(objectFactDao.searchFacts(any())).thenAnswer(x -> ResultContainer.<FactRecord>builder().build());

    helper.getOneLeggedFactsAsProperties(new ObjectRecord().setId(objectId), emptyTraverseParams);

    verify(objectFactDao).searchFacts(argThat(criteria -> {
      assertEquals(set(objectId), criteria.getObjectID());
      assertEquals(FactSearchCriteria.FactBinding.oneLegged, criteria.getFactBinding());
      assertSame(accessControlCriteria, criteria.getAccessControlCriteria());
      assertSame(indexSelectCriteria, criteria.getIndexSelectCriteria());
      return true;
    }));
  }

  @Test
  public void testGetObjectPropsInputValidation() {
    assertThrows(IllegalArgumentException.class,
            () -> helper.getObjectProperties(null, emptyTraverseParams));

    assertThrows(IllegalArgumentException.class,
            () -> helper.getObjectProperties(new ObjectRecord(), null));
  }

  @Test
  public void testGetObjectProps() {
    ObjectRecord objectRecord = new ObjectRecord().setId(UUID.randomUUID()).setValue("someValue");
    when(objectFactDao.searchFacts(any())).thenAnswer(x -> ResultContainer.<FactRecord>builder().build());

    List<PropertyEntry<?>> props = helper.getObjectProperties(objectRecord, emptyTraverseParams);

    assertEquals(set("value->someValue"), asKeyValueStrings(props));
    verify(objectFactDao).searchFacts(argThat(c -> {
      assertEquals(set(objectRecord.getId()), c.getObjectID());
      return true;
    }));
  }

  // Note: There should never be a one-legged fact type with name "value". This is just making sure it will work
  @Test
  public void testGetObjectPropsWithNameValue() {
    UUID objectId = UUID.randomUUID();
    ObjectRecord objectRecord = new ObjectRecord().setId(objectId).setValue("objectValue");

    FactTypeStruct factType1 = mockFactType("value");
    FactRecord fact = new FactRecord().setTypeID(factType1.getId()).setValue("someFactValue");

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getObjectID(), set(objectId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(fact).iterator()).build()
    );

    List<PropertyEntry<?>> props = helper.getObjectProperties(
            objectRecord,
            traverseParamsBuilder().setIncludeRetracted(true).build());

    assertEquals(2, props.size());
    assertEquals(set("value->objectValue", "value->someFactValue"), asKeyValueStrings(props));
  }

  @Test
  public void testGetObjectPropsForKeyInputValidation() {
    assertThrows(IllegalArgumentException.class,
            () -> helper.getObjectProperties(null, emptyTraverseParams, "key"));

    assertThrows(IllegalArgumentException.class,
            () -> helper.getObjectProperties(new ObjectRecord(), null, "key"));

    assertThrows(IllegalArgumentException.class,
            () -> helper.getObjectProperties(new ObjectRecord(), emptyTraverseParams, ""));
  }

  @Test
  public void testGetObjectPropsForValueKey() {
    ObjectRecord objectRecord = new ObjectRecord().setId(UUID.randomUUID()).setValue("someValue");

    List<PropertyEntry<?>> props = helper.getObjectProperties(objectRecord, emptyTraverseParams, "value");

    assertEquals(set("value->someValue"), asKeyValueStrings(props));
    verify(objectFactDao, never()).searchFacts(any());
  }

  @Test
  public void testGetObjectPropsForOneLeggedFact() {
    UUID objectId = UUID.randomUUID();
    ObjectRecord objectRecord = new ObjectRecord().setId(objectId);

    FactTypeStruct factType1 = mockFactType("factType1");
    FactTypeStruct factType2 = mockFactType("factType2");

    FactRecord fact1 = new FactRecord().setTypeID(factType1.getId()).setValue("factValue1");
    FactRecord fact2 = new FactRecord().setTypeID(factType2.getId()).setValue("factValue2");

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getObjectID(), set(objectId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(fact1, fact2).iterator()).build()
    );

    List<PropertyEntry<?>> props = helper.getObjectProperties(objectRecord, emptyTraverseParams, "factType1");

    assertEquals(set("factType1->factValue1"), asKeyValueStrings(props));
    verify(objectFactDao).searchFacts(notNull());
  }

  @Test
  public void testMetaFactsAsPropsEmptyResult() {
    when(objectFactDao.searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());

    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    List<PropertyEntry<?>> props = helper.getMetaFactsAsProperties(fact, emptyTraverseParams);

    assertEquals(0, props.size());

    verify(objectFactDao).searchFacts(argThat(arg -> {
      assertEquals(set(fact.getId()), arg.getInReferenceTo());
      return true;
    }));
  }

  @Test
  public void testMetaFactsAsPropsCanGetProperties() {
    FactTypeStruct factType = mockFactType("someFactType");
    FactTypeStruct metaFactType = mockFactType("metaFactType");

    UUID factId = UUID.randomUUID();
    FactRecord fact = new FactRecord().setTypeID(factType.getId()).setValue("someFact").setId(factId);
    FactRecord metaFact = new FactRecord().setTypeID(metaFactType.getId()).setValue("metaFactValue").setId(UUID.randomUUID()).setInReferenceToID(fact.getId());

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getInReferenceTo(), set(factId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(metaFact).iterator()).build()
    );

    List<PropertyEntry<?>> props = helper.getMetaFactsAsProperties(fact, emptyTraverseParams);

    assertEquals(set("meta/metaFactType->metaFactValue"), asKeyValueStrings(props));
  }

  @Test
  public void testMetaFactsAsPropsWithoutReadPermission() {
    FactTypeStruct factType = mockFactType("someFactType");
    FactTypeStruct metaFactType = mockFactType("metaFactType");

    UUID factId = UUID.randomUUID();
    FactRecord fact = new FactRecord().setTypeID(factType.getId()).setValue("someFact").setId(factId);
    FactRecord metaFact = new FactRecord().setTypeID(metaFactType.getId()).setValue("metaFactValue").setId(UUID.randomUUID()).setInReferenceToID(fact.getId());

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(false);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getInReferenceTo(), set(factId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(metaFact).iterator()).build()
    );

    List<PropertyEntry<?>> props = helper.getMetaFactsAsProperties(fact, emptyTraverseParams);
    assertEquals(0, props.size());
    verify(securityContext).hasReadPermission(metaFact);
  }

  @Test
  public void testMetaFactsAsPropsFilterRetracted() {
    FactTypeStruct factType = mockFactType("someFactType");
    FactTypeStruct metaFactType = mockFactType("metaFactType");

    UUID factId = UUID.randomUUID();
    FactRecord fact = new FactRecord().setTypeID(factType.getId()).setValue("someFact").setId(factId);
    FactRecord retractedMetaFact = new FactRecord().setTypeID(metaFactType.getId()).setValue("metaFactValue").setId(UUID.randomUUID()).setInReferenceToID(fact.getId());

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getInReferenceTo(), set(factId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(retractedMetaFact).iterator()).build()
    );

    when(factRetractionHandler.isRetracted(retractedMetaFact)).thenReturn(true);

    List<PropertyEntry<?>> props = helper.getMetaFactsAsProperties(fact, emptyTraverseParams);
    assertEquals(0, props.size());
    verify(factRetractionHandler).isRetracted(retractedMetaFact);
  }

  @Test
  public void testMetaFactsPassesSearchCriteria() {
    UUID inReferenceToID = UUID.randomUUID();
    when(objectFactDao.searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());

    helper.getMetaFactsAsProperties(new FactRecord().setId(inReferenceToID), emptyTraverseParams);

    verify(objectFactDao).searchFacts(argThat(criteria -> {
      assertEquals(set(inReferenceToID), criteria.getInReferenceTo());
      assertEquals(FactSearchCriteria.FactBinding.meta, criteria.getFactBinding());
      assertSame(accessControlCriteria, criteria.getAccessControlCriteria());
      assertSame(indexSelectCriteria, criteria.getIndexSelectCriteria());
      return true;
    }));
  }

  @Test
  public void testGetStaticFactProperties() {
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setValue("someValue")
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setTimestamp(1L)
            .setLastSeenTimestamp(2L)
            .setAddedByID(UUID.randomUUID());

    when(subjectResolver.apply(fact.getAddedByID())).thenReturn(Subject.builder().setName("someSubjectName").build());
    when(organizationResolver.apply(fact.getOrganizationID())).thenReturn(Organization.builder().setName("someOrgName").build());
    when(originResolver.apply(fact.getOriginID())).thenReturn(Origin.builder().setName("someOriginName").build());

    Map<String, ?> props = MapUtils.map(helper.getStaticFactProperties(fact), p -> T(p.getName(), p.getValue()));

    Map<String, ?> expected = MapUtils.map(
            T("accessMode", fact.getAccessMode().toString()),
            T("addedByID", fact.getAddedByID().toString()),
            T("addedByName", "someSubjectName"),
            T("certainty", 0.0f),
            T("confidence", 0.0f),
            T("trust", 0.0f),
            T("originID", fact.getOriginID().toString()),
            T("originName", "someOriginName"),
            T("organizationID", fact.getOrganizationID().toString()),
            T("organizationName", "someOrgName"),
            T("lastSeenTimestamp", 2L),
            T("timestamp", 1L),
            T("value", "someValue"),
            T("isRetracted", false));

    assertEqualMaps(expected, props);
  }

  @Test
  public void testGetStaticFactPropertiesMissingData() {
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setValue("someValue")
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setTimestamp(1L)
            .setLastSeenTimestamp(2L)
            .setAddedByID(UUID.randomUUID());

    Map<String, ?> props = MapUtils.map(helper.getStaticFactProperties(fact), p -> T(p.getName(), p.getValue()));

    assertEquals(fact.getOrganizationID().toString(), props.get("organizationID"));
    assertEquals(fact.getOriginID().toString(), props.get("originID"));
    assertEquals(fact.getAddedByID().toString(), props.get("addedByID"));
    assertNull(props.get("organizationName"));
    assertNull(props.get("originName"));
    assertNull(props.get("addedByName"));
  }

  @Test
  public void testGetStaticFactPropertiesWithEmptyFact() {
    Map<String, ?> props = MapUtils.map(helper.getStaticFactProperties(new FactRecord()), p -> T(p.getName(), p.getValue()));

    assertEquals(0.0f, props.get("trust"));
    assertEquals(0.0f, props.get("confidence"));
    assertEquals(0.0f, props.get("certainty"));
    assertEquals(0L, props.get("timestamp"));
    assertEquals(0L, props.get("lastSeenTimestamp"));
    assertNull(props.get("value"));

  }


  @Test
  public void testGetFactPropsInputValidation() {
    assertThrows(IllegalArgumentException.class,
            () -> helper.getFactProperties(null, emptyTraverseParams));

    assertThrows(IllegalArgumentException.class,
            () -> helper.getFactProperties(new FactRecord(), null));
  }

  @Test
  public void testGetFactProperties() {
    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setValue("value");

    when(objectFactDao.searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
    List<PropertyEntry<?>> props = helper.getFactProperties(factRecord, emptyTraverseParams);
    assertEquals(10, props.size());

    verify(objectFactDao).searchFacts(any());
    verify(subjectResolver).apply(factRecord.getAddedByID());
    verify(organizationResolver).apply(factRecord.getOrganizationID());
    verify(originResolver).apply(factRecord.getOriginID());
  }

  @Test
  public void testGetFactPropsForKeyInputValidation() {
    assertThrows(IllegalArgumentException.class,
            () -> helper.getFactProperties(null, emptyTraverseParams, "key"));

    assertThrows(IllegalArgumentException.class,
            () -> helper.getFactProperties(new FactRecord(), null, "key"));

    assertThrows(IllegalArgumentException.class,
            () -> helper.getFactProperties(new FactRecord(), emptyTraverseParams, ""));
  }

  @Test
  public void testGetFactPropsForKeyNotFound() {
    assertTrue(helper.getFactProperties(new FactRecord(), emptyTraverseParams, "somethingWeird").isEmpty());
    verify(objectFactDao, never()).searchFacts(any());
  }

  @Test
  public void testGetFactPropsForKeyMetaProperties() {
    FactTypeStruct factType = mockFactType("someFactType");
    FactTypeStruct metaFactType1 = mockFactType("metaFactType1");
    FactTypeStruct metaFactType2 = mockFactType("metaFactType2");

    UUID factId = UUID.randomUUID();
    FactRecord fact = new FactRecord().setTypeID(factType.getId()).setValue("someFact").setId(factId);
    FactRecord metaFact1 = new FactRecord().setTypeID(metaFactType1.getId()).setValue("metaFactValue1").setId(UUID.randomUUID()).setInReferenceToID(fact.getId());
    FactRecord metaFact2 = new FactRecord().setTypeID(metaFactType2.getId()).setValue("metaFactValue2").setId(UUID.randomUUID()).setInReferenceToID(fact.getId());

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getInReferenceTo(), set(factId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(metaFact1, metaFact2).iterator()).build()
    );

    List<PropertyEntry<?>> props = helper.getFactProperties(fact, emptyTraverseParams, "meta/metaFactType1");

    assertEquals(set("meta/metaFactType1->metaFactValue1"), asKeyValueStrings(props));
    verify(objectFactDao).searchFacts(notNull());
  }

  @Test
  public void testGetFactPropsForKeyStaticProperties() {
    FactRecord fact = new FactRecord()
            .setValue("someValue")
            .setAddedByID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Public);

    when(subjectResolver.apply(fact.getAddedByID())).thenReturn(Subject.builder().setName("someSubjectName").build());
    when(organizationResolver.apply(fact.getOrganizationID())).thenReturn(Organization.builder().setName("someOrgName").build());
    when(originResolver.apply(fact.getOriginID())).thenReturn(Origin.builder().setName("someOriginName").build());

    for (PropertyHelper.StaticFactProperty property : PropertyHelper.StaticFactProperty.values()) {
      assertFalse(helper.getFactProperties(fact, emptyTraverseParams, property.name()).isEmpty());
    }
  }

  private TraverseParams.Builder traverseParamsBuilder() {
    return TraverseParams.builder()
            .setBaseSearchCriteria(factSearchCriteria);
  }

  private FactTypeStruct mockFactType(String name) {
    UUID typeId = UUID.randomUUID();
    FactTypeStruct factType = FactTypeStruct.builder().setId(typeId).setName(name).build();
    when(objectFactTypeResolver.toFactTypeStruct(typeId)).thenReturn(factType);
    return factType;
  }

  private Set<String> asKeyValueStrings(List<PropertyEntry<?>> props) {
    return props.stream()
            .map(x -> x.getName() + "->" + x.getValue())
            .collect(Collectors.toSet());
  }

  private void assertEqualMaps(Map<String, ?> expected, Map<String, ?> actual) {
    assertEquals(expected.keySet(), actual.keySet());
    for (String s : expected.keySet()) {
      assertEquals(expected.get(s), actual.get(s));
    }
  }
}
