package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PropertyHelperTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private ObjectFactTypeResolver objectFactTypeResolver;
  @Mock
  private FactRetractionHandler factRetractionHandler;
  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private SubjectResolver subjectResolver;
  @Mock
  private OrganizationResolver organizationResolver;
  @Mock
  private OriginResolver originResolver;

  private PropertyHelper helper;

  @Before
  public void setUp() {
    initMocks(this);
    helper = new PropertyHelper(factRetractionHandler, objectFactDao, objectFactTypeResolver, securityContext,
            subjectResolver, organizationResolver, originResolver);
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(securityContext.getAvailableOrganizationID()).thenReturn(set(UUID.randomUUID()));
    when(factRetractionHandler.isRetracted(any())).thenReturn(false);
  }

  @Test
  public void testOneLeggedFactsAsPropsNoOneLeggedFacts() {
    when(objectFactDao.searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
    assertEquals(0, helper.getOneLeggedFactsAsProperties(new ObjectRecord(), TraverseParams.builder().build()).size());
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
            TraverseParams.builder().setIncludeRetracted(true).build());

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

    assertEquals(0, helper.getOneLeggedFactsAsProperties(objectRecord, TraverseParams.builder().build()).size());
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

    assertEquals(2, helper.getOneLeggedFactsAsProperties(objectRecord, TraverseParams.builder().setIncludeRetracted(true).build()).size());
    assertEquals(1, helper.getOneLeggedFactsAsProperties(objectRecord, TraverseParams.builder().setIncludeRetracted(false).build()).size());
  }

  @Test
  public void testOneLeggedFactsAsPropsTimeFilter() {
    Long start = 1L;
    Long end = 2L;
    UUID objectId = UUID.randomUUID();
    ObjectRecord objectRecord = new ObjectRecord().setId(objectId);

    when(objectFactDao.searchFacts(any())).thenAnswer(x -> ResultContainer.<FactRecord>builder().build());

    helper.getOneLeggedFactsAsProperties(objectRecord, TraverseParams.builder()
            .setBeforeTimestamp(end)
            .setAfterTimestamp(start)
            .setIncludeRetracted(true)
            .build());

    verify(objectFactDao).searchFacts(argThat(criteria -> {
      assertEquals(set(objectId), criteria.getObjectID());
      assertEquals(FactSearchCriteria.FactBinding.oneLegged, criteria.getFactBinding());
      assertEquals(start, criteria.getStartTimestamp());
      assertEquals(end, criteria.getEndTimestamp());
      return true;
    }));
  }

  @Test
  public void testGetObjectPropsInputValidation() {
    assertThrows(IllegalArgumentException.class,
            () -> helper.getObjectProperties(null, TraverseParams.builder().build()));

    assertThrows(IllegalArgumentException.class,
            () -> helper.getObjectProperties(new ObjectRecord(), null));
  }

  @Test
  public void testGetObjectProps() {
    ObjectRecord objectRecord = new ObjectRecord().setId(UUID.randomUUID()).setValue("someValue");
    when(objectFactDao.searchFacts(any())).thenAnswer(x -> ResultContainer.<FactRecord>builder().build());

    List<PropertyEntry<?>> props = helper.getObjectProperties(objectRecord, TraverseParams.builder().build());

    assertEquals(1, props.size());
    assertEquals("value", props.get(0).getName());
    assertEquals("someValue", props.get(0).getValue());
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
            TraverseParams.builder().setIncludeRetracted(true).build());

    assertEquals(2, props.size());
    assertEquals(set("value->objectValue", "value->someFactValue"), asKeyValueStrings(props));
  }

  @Test
  public void testMetaFactsAsPropsEmptyResult() {
    when(objectFactDao.searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());

    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    List<PropertyEntry<?>> props = helper.getMetaFactsAsProperties(fact, TraverseParams.builder().build());

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

    List<PropertyEntry<?>> props = helper.getMetaFactsAsProperties(fact, TraverseParams.builder().build());

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

    List<PropertyEntry<?>> props = helper.getMetaFactsAsProperties(fact, TraverseParams.builder().build());
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

    List<PropertyEntry<?>> props = helper.getMetaFactsAsProperties(fact, TraverseParams.builder().build());
    assertEquals(0, props.size());
    verify(factRetractionHandler).isRetracted(retractedMetaFact);
  }

  @Test
  public void testMetaFactsAsPropsTimeFilter() {
    Long start = 1L;
    Long end = 2L;

    UUID inReferenceToID = UUID.randomUUID();

    when(objectFactDao.searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());

    helper.getMetaFactsAsProperties(new FactRecord().setId(inReferenceToID), TraverseParams.builder()
            .setBeforeTimestamp(end)
            .setAfterTimestamp(start)
            .setIncludeRetracted(true)
            .build());

    verify(objectFactDao).searchFacts(argThat(criteria -> {
      assertEquals(set(inReferenceToID), criteria.getInReferenceTo());
      assertEquals(FactSearchCriteria.FactBinding.meta, criteria.getFactBinding());
      assertEquals(start, criteria.getStartTimestamp());
      assertEquals(end, criteria.getEndTimestamp());
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

    when(subjectResolver.resolveSubject(fact.getAddedByID())).thenReturn(Subject.builder().setName("someSubjectName").build());
    when(organizationResolver.resolveOrganization(fact.getOrganizationID())).thenReturn(Organization.builder().setName("someOrgName").build());
    when(originResolver.apply(fact.getOriginID())).thenReturn(new OriginEntity().setName("someOriginName"));

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
  public void testGetFactPropsInputValidation() {
    assertThrows(IllegalArgumentException.class,
            () -> helper.getFactProperties(null, TraverseParams.builder().build()));

    assertThrows(IllegalArgumentException.class,
            () -> helper.getFactProperties(new FactRecord(), null));
  }

  @Test
  public void testGetFactProperties() {
    FactRecord factRecord = new FactRecord()
            .setId(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID());

    when(objectFactDao.searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
    List<PropertyEntry<?>> props = helper.getFactProperties(factRecord, TraverseParams.builder().build());
    assertEquals(10, props.size());

    verify(objectFactDao).searchFacts(any());
    verify(subjectResolver).resolveSubject(factRecord.getAddedByID());
    verify(organizationResolver).resolveOrganization(factRecord.getOrganizationID());
    verify(originResolver).apply(factRecord.getOriginID());
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
