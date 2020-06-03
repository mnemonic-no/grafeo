package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.act.platform.service.ti.tinkerpop.TraverseParams;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

  private PropertyHelper helper;

  @Before
  public void setUp() {
    initMocks(this);
    helper = new PropertyHelper(factRetractionHandler, objectFactDao, objectFactTypeResolver, securityContext);
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(securityContext.getAvailableOrganizationID()).thenReturn(set(UUID.randomUUID()));
    when(factRetractionHandler.isRetracted(any())).thenReturn(false);
  }

  @Test
  public void testNoOneLeggedFacts() {
    when(objectFactDao.searchFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
    assertEquals(0, helper.getOneLeggedFactsAsProperties(UUID.randomUUID(), TraverseParams.builder().build()).size());
  }

  @Test
  public void testCanGetProperties() {
    UUID objectId = UUID.randomUUID();

    FactTypeStruct factType1 = mockFactType("factType1");
    FactTypeStruct factType2 = mockFactType("factType2");

    FactRecord fact1 = new FactRecord().setTypeID(factType1.getId()).setValue("factValue1");
    FactRecord fact2 = new FactRecord().setTypeID(factType2.getId()).setValue("factValue2");

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getObjectID(), set(objectId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(fact1,fact2).iterator()).build()
    );

    Set<String> props = helper.getOneLeggedFactsAsProperties(objectId, TraverseParams.builder().setIncludeRetracted(true).build())
            .stream()
            .map(p -> p.getName() + "->" + p.getValue())
            .collect(Collectors.toSet());

    assertEquals(2, props.size());
    assertEquals(set("factType1->factValue1", "factType2->factValue2"), props);
  }

  @Test
  public void testWithoutReadPermission() {
    UUID objectId = UUID.randomUUID();

    FactTypeStruct factType1 = mockFactType("factType1");
    FactTypeStruct factType2 = mockFactType("factType2");

    FactRecord fact1 = new FactRecord().setTypeID(factType1.getId()).setValue("factValue1");
    FactRecord fact2 = new FactRecord().setTypeID(factType2.getId()).setValue("factValue2");

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(false);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getObjectID(), set(objectId))))).thenReturn(
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(fact1,fact2).iterator()).build()
    );

    assertEquals(0, helper.getOneLeggedFactsAsProperties(objectId, TraverseParams.builder().build()).size());
    verify(securityContext).hasReadPermission(fact1);
    verify(securityContext).hasReadPermission(fact2);
  }

  @Test
  public void testFilterRetracted() {
    UUID objectId = UUID.randomUUID();
    FactTypeStruct factType = mockFactType("factType");

    FactRecord retractedFact = new FactRecord().setTypeID(factType.getId()).setValue("factValue1");
    FactRecord fact = new FactRecord().setTypeID(factType.getId()).setValue("factValue2");

    when(securityContext.hasReadPermission(any(FactRecord.class))).thenReturn(true);
    when(objectFactDao.searchFacts(argThat(x -> Objects.equals(x.getObjectID(), set(objectId))))).thenAnswer(x ->
            ResultContainer.<FactRecord>builder().setValues(ListUtils.list(retractedFact,fact).iterator()).build()
    );
    when(factRetractionHandler.isRetracted(retractedFact)).thenReturn(true);
    when(factRetractionHandler.isRetracted(fact)).thenReturn(false);

    assertEquals(2, helper.getOneLeggedFactsAsProperties(objectId, TraverseParams.builder().setIncludeRetracted(true).build()).size());
    assertEquals(1, helper.getOneLeggedFactsAsProperties(objectId, TraverseParams.builder().setIncludeRetracted(false).build()).size());
  }

  @Test
  public void testTimeFilter() {
    Long start = 1L;
    Long end = 2L;
    UUID objectId = UUID.randomUUID();

    when(objectFactDao.searchFacts(any())).thenAnswer(x -> ResultContainer.<FactRecord>builder().build());

    helper.getOneLeggedFactsAsProperties(objectId, TraverseParams.builder()
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
  public void testInputValidation() {
    assertThrows(IllegalArgumentException.class,
            () -> helper.getOneLeggedFactsAsProperties(null, TraverseParams.builder().build()));

    assertThrows(IllegalArgumentException.class,
            () -> helper.getOneLeggedFactsAsProperties(UUID.randomUUID(), null));
  }

  private FactTypeStruct mockFactType(String name) {
    UUID typeId = UUID.randomUUID();
    FactTypeStruct factType = FactTypeStruct.builder().setId(typeId).setName(name).build();
    when(objectFactTypeResolver.toFactTypeStruct(typeId)).thenReturn(factType);
    return factType;
  }
}

