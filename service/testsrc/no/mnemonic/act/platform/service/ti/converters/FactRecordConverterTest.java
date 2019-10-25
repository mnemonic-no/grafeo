package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.*;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.handlers.FactRetractionHandler;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactRecordConverterTest {

  @Mock
  private FactTypeByIdConverter factTypeConverter;
  @Mock
  private OriginByIdConverter originConverter;
  @Mock
  private ObjectRecordConverter objectConverter;
  @Mock
  private Function<UUID, Organization> organizationConverter;
  @Mock
  private Function<UUID, Subject> subjectConverter;
  @Mock
  private FactRetractionHandler factRetractionHandler;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private TiSecurityContext securityContext;

  private FactRecordConverter converter;

  @Before
  public void setUp() {
    initMocks(this);

    when(factTypeConverter.apply(notNull())).thenAnswer(i -> FactType.builder().setId(i.getArgument(0)).build());
    when(originConverter.apply(notNull())).thenAnswer(i -> Origin.builder().setId(i.getArgument(0)).build());
    when(organizationConverter.apply(notNull())).thenAnswer(i -> Organization.builder().setId(i.getArgument(0)).build());
    when(subjectConverter.apply(notNull())).thenAnswer(i -> Subject.builder().setId(i.getArgument(0)).build());

    converter = new FactRecordConverter(
            factTypeConverter,
            originConverter,
            objectConverter,
            organizationConverter,
            subjectConverter,
            factRetractionHandler,
            objectFactDao,
            securityContext
    );
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertFactEmpty() {
    assertNotNull(converter.apply(new FactRecord()));
  }

  @Test
  public void testConvertFactBothSourceAndDestination() {
    ObjectRecord source = new ObjectRecord().setId(UUID.randomUUID());
    ObjectRecord destination = new ObjectRecord().setId(UUID.randomUUID());
    FactRecord record = createRecord()
            .setSourceObject(source)
            .setDestinationObject(destination)
            .setBidirectionalBinding(true);
    when(objectConverter.apply(source)).thenReturn(Object.builder().setId(source.getId()).build());
    when(objectConverter.apply(destination)).thenReturn(Object.builder().setId(destination.getId()).build());

    Fact model = converter.apply(record);

    assertModelCommon(record, model);
    assertTrue(model.isBidirectionalBinding());
    assertNotNull(model.getSourceObject());
    assertEquals(source.getId(), model.getSourceObject().getId());
    assertNotNull(model.getDestinationObject());
    assertEquals(destination.getId(), model.getDestinationObject().getId());
  }

  @Test
  public void testConvertFactOnlySource() {
    ObjectRecord source = new ObjectRecord().setId(UUID.randomUUID());
    FactRecord record = createRecord().setSourceObject(source);
    when(objectConverter.apply(source)).thenReturn(Object.builder().setId(source.getId()).build());

    Fact model = converter.apply(record);

    assertModelCommon(record, model);
    assertNotNull(model.getSourceObject());
    assertEquals(source.getId(), model.getSourceObject().getId());
    assertNull(model.getDestinationObject());
  }

  @Test
  public void testConvertFactOnlyDestination() {
    ObjectRecord destination = new ObjectRecord().setId(UUID.randomUUID());
    FactRecord record = createRecord().setDestinationObject(destination);
    when(objectConverter.apply(destination)).thenReturn(Object.builder().setId(destination.getId()).build());

    Fact model = converter.apply(record);

    assertModelCommon(record, model);
    assertNull(model.getSourceObject());
    assertNotNull(model.getDestinationObject());
    assertEquals(destination.getId(), model.getDestinationObject().getId());
  }

  @Test
  public void testConvertFactWithoutBinding() {
    FactRecord record = createRecord();
    Fact model = converter.apply(record);

    assertModelCommon(record, model);
    assertFalse(model.isBidirectionalBinding());
    assertNull(model.getSourceObject());
    assertNull(model.getDestinationObject());
  }

  @Test
  public void testConvertFactCannotResolveInReferenceToFact() {
    FactRecord record = createRecord().setInReferenceToID(UUID.randomUUID());
    Fact model = converter.apply(record);

    assertNull(model.getInReferenceTo());
    verify(objectFactDao).getFact(record.getInReferenceToID());
  }

  @Test
  public void testConvertFactNoAccessToInReferenceToFact() {
    FactRecord record = createRecord().setInReferenceToID(UUID.randomUUID());
    FactRecord inReferenceTo = new FactRecord().setId(record.getInReferenceToID());
    when(objectFactDao.getFact(record.getInReferenceToID())).thenReturn(inReferenceTo);
    when(securityContext.hasReadPermission(inReferenceTo)).thenReturn(false);

    Fact model = converter.apply(record);

    assertNull(model.getInReferenceTo());
    verify(securityContext).hasReadPermission(inReferenceTo);
  }

  @Test
  public void testConvertFactWithInReferenceToFact() {
    FactRecord record = createRecord().setInReferenceToID(UUID.randomUUID());
    FactRecord inReferenceTo = new FactRecord().setId(record.getInReferenceToID());
    when(objectFactDao.getFact(record.getInReferenceToID())).thenReturn(inReferenceTo);
    when(securityContext.hasReadPermission(inReferenceTo)).thenReturn(true);

    Fact model = converter.apply(record);

    assertNotNull(model.getInReferenceTo());
    assertEquals(record.getInReferenceToID(), model.getInReferenceTo().getId());
    verify(securityContext).hasReadPermission(inReferenceTo);
  }

  @Test
  public void testConvertFactNotRetracted() {
    FactRecord record = createRecord();
    when(factRetractionHandler.isRetracted(record.getId(), false)).thenReturn(false);

    Fact model = converter.apply(record);

    assertEquals(SetUtils.set(), model.getFlags());
    verify(factRetractionHandler).isRetracted(record.getId(), false);
  }

  @Test
  public void testConvertFactIsRetracted() {
    FactRecord record = createRecord().addFlag(FactRecord.Flag.RetractedHint);
    when(factRetractionHandler.isRetracted(record.getId(), true)).thenReturn(true);

    Fact model = converter.apply(record);

    assertEquals(SetUtils.set(Fact.Flag.Retracted), model.getFlags());
    verify(factRetractionHandler).isRetracted(record.getId(), true);
  }

  private FactRecord createRecord() {
    return new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setOrganizationID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTrust(0.1f)
            .setConfidence(0.2f)
            .setAccessMode(FactRecord.AccessMode.Explicit)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321);
  }

  private void assertModelCommon(FactRecord record, Fact model) {
    assertEquals(record.getId(), model.getId());
    assertEquals(record.getTypeID(), model.getType().getId());
    assertEquals(record.getValue(), model.getValue());
    assertEquals(record.getOrganizationID(), model.getOrganization().getId());
    assertEquals(record.getAddedByID(), model.getAddedBy().getId());
    assertEquals(record.getOriginID(), model.getOrigin().getId());
    assertEquals(record.getTrust(), model.getTrust(), 0.0);
    assertEquals(record.getConfidence(), model.getConfidence(), 0.0);
    assertEquals(record.getAccessMode().name(), model.getAccessMode().name());
    assertEquals(record.getTimestamp(), (long) model.getTimestamp());
    assertEquals(record.getLastSeenTimestamp(), (long) model.getLastSeenTimestamp());
  }
}
