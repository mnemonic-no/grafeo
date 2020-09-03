package no.mnemonic.act.platform.seb.producer.v1.converters;

import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.seb.model.v1.*;
import no.mnemonic.act.platform.seb.producer.v1.resolvers.*;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactConverterTest {

  @Mock
  private FactTypeInfoResolver typeResolver;
  @Mock
  private FactInfoResolver inReferenceToResolver;
  @Mock
  private OrganizationInfoResolver organizationResolver;
  @Mock
  private OriginInfoResolver originResolver;
  @Mock
  private SubjectInfoResolver addedByResolver;
  @Mock
  private ObjectInfoConverter objectConverter;
  @Mock
  private AclEntryConverter aclEntryConverter;

  private FactConverter converter;

  @Before
  public void setUp() {
    initMocks(this);

    when(typeResolver.apply(any())).thenReturn(FactTypeInfoSEB.builder().build());
    when(inReferenceToResolver.apply(any())).thenReturn(FactInfoSEB.builder().build());
    when(organizationResolver.apply(any())).thenReturn(OrganizationInfoSEB.builder().build());
    when(originResolver.apply(any())).thenReturn(OriginInfoSEB.builder().build());
    when(addedByResolver.apply(any())).thenReturn(SubjectInfoSEB.builder().build());
    when(objectConverter.apply(any())).thenReturn(ObjectInfoSEB.builder().build());
    when(aclEntryConverter.apply(any())).thenReturn(AclEntrySEB.builder().build());

    converter = new FactConverter(
            typeResolver,
            inReferenceToResolver,
            organizationResolver,
            originResolver,
            addedByResolver,
            objectConverter,
            aclEntryConverter
    );
  }

  @Test
  public void testConvertNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmpty() {
    assertNotNull(converter.apply(new FactRecord()));
  }

  @Test
  public void testConvertFull() {
    ObjectRecord source = new ObjectRecord();
    ObjectRecord destination = new ObjectRecord();
    FactAclEntryRecord aclEntry = new FactAclEntryRecord();
    FactRecord record = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Explicit)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setSourceObject(source)
            .setDestinationObject(destination)
            .setBidirectionalBinding(true)
            .addFlag(FactRecord.Flag.RetractedHint)
            .addAclEntry(aclEntry);

    FactSEB seb = converter.apply(record);
    assertNotNull(seb);
    assertEquals(record.getId(), seb.getId());
    assertNotNull(seb.getType());
    assertEquals(record.getValue(), seb.getValue());
    assertNotNull(seb.getInReferenceTo());
    assertNotNull(seb.getOrganization());
    assertNotNull(seb.getOrigin());
    assertNotNull(seb.getAddedBy());
    assertEquals(record.getAccessMode().name(), seb.getAccessMode().name());
    assertEquals(record.getConfidence(), seb.getConfidence(), 0.0);
    assertEquals(record.getTrust(), seb.getTrust(), 0.0);
    assertEquals(record.getTimestamp(), seb.getTimestamp());
    assertEquals(record.getLastSeenTimestamp(), seb.getLastSeenTimestamp());
    assertNotNull(seb.getSourceObject());
    assertNotNull(seb.getDestinationObject());
    assertTrue(seb.isBidirectionalBinding());
    assertEquals(SetUtils.set(record.getFlags(), Enum::name), SetUtils.set(seb.getFlags(), Enum::name));
    assertFalse(seb.getAcl().isEmpty());

    verify(typeResolver).apply(record.getTypeID());
    verify(inReferenceToResolver).apply(record.getInReferenceToID());
    verify(organizationResolver).apply(record.getOrganizationID());
    verify(originResolver).apply(record.getOriginID());
    verify(addedByResolver).apply(record.getAddedByID());
    verify(objectConverter).apply(source);
    verify(objectConverter).apply(destination);
    verify(aclEntryConverter).apply(aclEntry);
  }
}
