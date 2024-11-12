package no.mnemonic.services.grafeo.dao.facade.converters;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactCommentRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.*;
import no.mnemonic.services.grafeo.dao.elastic.document.FactDocument;
import no.mnemonic.services.grafeo.dao.elastic.document.ObjectDocument;
import no.mnemonic.services.grafeo.dao.facade.resolvers.CachedObjectResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactRecordConverterTest {

  @Mock
  private FactManager factManager;
  @Mock
  private CachedObjectResolver objectResolver;
  @Mock
  private FactAclEntryRecordConverter factAclEntryRecordConverter;
  @Mock
  private FactCommentRecordConverter factCommentRecordConverter;
  @InjectMocks
  private FactRecordConverter converter;

  @Test
  public void testFromEntityWithNullEntity() {
    assertNull(converter.fromEntity(null));
  }

  @Test
  public void testFromEntityWithEmptyEntity() {
    assertNotNull(converter.fromEntity(new FactEntity()));
  }

  @Test
  public void testFromEntityWithDirectFields() {
    FactEntity entity = new FactEntity()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setLastSeenByID(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(123456789L)
            .setLastSeenTimestamp(987654321L)
            .addFlag(FactEntity.Flag.RetractedHint);

    FactRecord record = converter.fromEntity(entity);
    assertEquals(entity.getId(), record.getId());
    assertEquals(entity.getTypeID(), record.getTypeID());
    assertEquals(entity.getValue(), record.getValue());
    assertEquals(entity.getInReferenceToID(), record.getInReferenceToID());
    assertEquals(entity.getOrganizationID(), record.getOrganizationID());
    assertEquals(entity.getOriginID(), record.getOriginID());
    assertEquals(entity.getAddedByID(), record.getAddedByID());
    assertEquals(entity.getLastSeenByID(), record.getLastSeenByID());
    assertEquals(entity.getAccessMode().name(), record.getAccessMode().name());
    assertEquals(entity.getConfidence(), record.getConfidence(), 0.0f);
    assertEquals(entity.getTrust(), record.getTrust(), 0.0f);
    assertEquals(entity.getTimestamp(), record.getTimestamp());
    assertEquals(entity.getLastSeenTimestamp(), record.getLastSeenTimestamp());
    assertEquals(SetUtils.set(FactRecord.Flag.RetractedHint), record.getFlags());
  }

  @Test
  public void testFromEntitySkipsCassandraOnlyFlags() {
    FactEntity entity = new FactEntity();
    Arrays.stream(FactEntity.Flag.values()).forEach(entity::addFlag);

    FactRecord record = converter.fromEntity(entity);
    assertEquals(SetUtils.set(FactRecord.Flag.values()), record.getFlags());
  }

  @Test
  public void testFromEntityUsesSeparatedObjectFields() {
    when(objectResolver.getObject(notNull())).thenReturn(new ObjectRecord());

    FactEntity entity = new FactEntity()
            .setSourceObjectID(UUID.randomUUID())
            .setDestinationObjectID(UUID.randomUUID())
            .addFlag(FactEntity.Flag.BidirectionalBinding)
            .addFlag(FactEntity.Flag.UsesSeparatedObjectFields);

    FactRecord record = converter.fromEntity(entity);
    assertNotNull(record.getSourceObject());
    assertNotNull(record.getDestinationObject());
    assertTrue(record.isBidirectionalBinding());

    verify(objectResolver).getObject(entity.getSourceObjectID());
    verify(objectResolver).getObject(entity.getDestinationObjectID());
  }

  @Test
  public void testFromEntityWithBindingOfCardinalityTwo() {
    when(objectResolver.getObject(notNull())).thenReturn(new ObjectRecord());

    FactEntity.FactObjectBinding source = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsDestination);
    FactEntity.FactObjectBinding destination = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsSource);
    FactEntity entity = new FactEntity().addBinding(source).addBinding(destination);

    FactRecord record = converter.fromEntity(entity);
    assertNotNull(record.getSourceObject());
    assertNotNull(record.getDestinationObject());
    assertFalse(record.isBidirectionalBinding());

    verify(objectResolver).getObject(source.getObjectID());
    verify(objectResolver).getObject(destination.getObjectID());
  }

  @Test
  public void testFromEntityWithBindingOfCardinalityTwoBidirectional() {
    when(objectResolver.getObject(notNull())).thenReturn(new ObjectRecord());

    FactEntity.FactObjectBinding source = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
    FactEntity.FactObjectBinding destination = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
    FactEntity entity = new FactEntity().addBinding(source).addBinding(destination);

    FactRecord record = converter.fromEntity(entity);
    assertNotNull(record.getSourceObject());
    assertNotNull(record.getDestinationObject());
    assertTrue(record.isBidirectionalBinding());

    verify(objectResolver).getObject(source.getObjectID());
    verify(objectResolver).getObject(destination.getObjectID());
  }

  @Test
  public void testFromEntityWithBindingOfCardinalityOneFactIsSource() {
    when(objectResolver.getObject(notNull())).thenReturn(new ObjectRecord());

    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsSource);
    FactEntity entity = new FactEntity().addBinding(binding);

    FactRecord record = converter.fromEntity(entity);
    assertNull(record.getSourceObject());
    assertNotNull(record.getDestinationObject());
    assertFalse(record.isBidirectionalBinding());

    verify(objectResolver).getObject(binding.getObjectID());
  }

  @Test
  public void testFromEntityWithBindingOfCardinalityOneFactIsDestination() {
    when(objectResolver.getObject(notNull())).thenReturn(new ObjectRecord());

    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsDestination);
    FactEntity entity = new FactEntity().addBinding(binding);

    FactRecord record = converter.fromEntity(entity);
    assertNotNull(record.getSourceObject());
    assertNull(record.getDestinationObject());
    assertFalse(record.isBidirectionalBinding());

    verify(objectResolver).getObject(binding.getObjectID());
  }

  @Test
  public void testFromEntityWithBindingOfCardinalityOneBiDirectional() {
    when(objectResolver.getObject(notNull())).thenReturn(new ObjectRecord());

    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
    FactEntity entity = new FactEntity().addBinding(binding);

    FactRecord record = converter.fromEntity(entity);
    assertNotNull(record.getSourceObject());
    assertNotNull(record.getDestinationObject());
    assertTrue(record.isBidirectionalBinding());

    verify(objectResolver).getObject(binding.getObjectID());
  }

  @Test
  public void testFromEntityWithoutBinding() {
    FactRecord record = converter.fromEntity(new FactEntity());
    assertNull(record.getSourceObject());
    assertNull(record.getDestinationObject());
  }

  @Test
  public void testFromEntityWithBindingOfCardinalityTwoAndSameDirectionFactIsDestination() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsDestination);
    FactEntity entity = new FactEntity().addBinding(binding).addBinding(binding);

    FactRecord record = converter.fromEntity(entity);
    assertNull(record.getSourceObject());
    assertNull(record.getDestinationObject());
  }

  @Test
  public void testFromEntityWithBindingOfCardinalityTwoAndSameDirectionFactIsSource() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.FactIsSource);
    FactEntity entity = new FactEntity().addBinding(binding).addBinding(binding);

    FactRecord record = converter.fromEntity(entity);
    assertNull(record.getSourceObject());
    assertNull(record.getDestinationObject());
  }

  @Test
  public void testFromEntityWithBindingOfCardinalityThree() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
    FactEntity entity = new FactEntity().addBinding(binding).addBinding(binding).addBinding(binding);

    FactRecord record = converter.fromEntity(entity);
    assertNull(record.getSourceObject());
    assertNull(record.getDestinationObject());
  }

  @Test
  public void testFromEntityWithAcl() {
    FactEntity entity = new FactEntity().setId(UUID.randomUUID()).addFlag(FactEntity.Flag.HasAcl);
    when(factManager.fetchFactAcl(entity.getId()))
            .thenReturn(ListUtils.list(new FactAclEntity(), new FactAclEntity(), new FactAclEntity()));
    when(factAclEntryRecordConverter.fromEntity(notNull())).thenReturn(new FactAclEntryRecord());

    FactRecord record = converter.fromEntity(entity);
    assertEquals(3, record.getAcl().size());

    verify(factManager).fetchFactAcl(entity.getId());
    verify(factAclEntryRecordConverter, times(3)).fromEntity(notNull());
  }

  @Test
  public void testFromEntitySkipsAcl() {
    FactEntity entity = new FactEntity().setId(UUID.randomUUID());

    FactRecord record = converter.fromEntity(entity);
    assertNull(record.getAcl());

    verify(factManager, never()).fetchFactAcl(entity.getId());
  }

  @Test
  public void testFromEntityWithComments() {
    FactEntity entity = new FactEntity().setId(UUID.randomUUID()).addFlag(FactEntity.Flag.HasComments);
    when(factManager.fetchFactComments(entity.getId()))
            .thenReturn(ListUtils.list(new FactCommentEntity(), new FactCommentEntity(), new FactCommentEntity()));
    when(factCommentRecordConverter.fromEntity(notNull())).thenReturn(new FactCommentRecord());

    FactRecord record = converter.fromEntity(entity);
    assertEquals(3, record.getComments().size());

    verify(factManager).fetchFactComments(entity.getId());
    verify(factCommentRecordConverter, times(3)).fromEntity(notNull());
  }

  @Test
  public void testFromEntitySkipComments() {
    FactEntity entity = new FactEntity().setId(UUID.randomUUID());

    FactRecord record = converter.fromEntity(entity);
    assertNull(record.getComments());

    verify(factManager, never()).fetchFactComments(entity.getId());
  }

  @Test
  public void testToEntityWithNullRecord() {
    assertNull(converter.toEntity(null));
  }

  @Test
  public void testToEntityWithEmptyRecord() {
    assertNotNull(converter.toEntity(new FactRecord()));
  }

  @Test
  public void testToEntityWithFullRecord() {
    FactRecord record = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setLastSeenByID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Explicit)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(123456789L)
            .setLastSeenTimestamp(987654321L)
            .addFlag(FactRecord.Flag.RetractedHint);

    FactEntity entity = converter.toEntity(record);
    assertEquals(record.getId(), entity.getId());
    assertEquals(record.getTypeID(), entity.getTypeID());
    assertEquals(record.getValue(), entity.getValue());
    assertEquals(record.getInReferenceToID(), entity.getInReferenceToID());
    assertEquals(record.getOrganizationID(), entity.getOrganizationID());
    assertEquals(record.getOriginID(), entity.getOriginID());
    assertEquals(record.getAddedByID(), entity.getAddedByID());
    assertEquals(record.getLastSeenByID(), entity.getLastSeenByID());
    assertEquals(record.getAccessMode().name(), entity.getAccessMode().name());
    assertEquals(record.getConfidence(), entity.getConfidence(), 0.0f);
    assertEquals(record.getTrust(), entity.getTrust(), 0.0f);
    assertEquals(record.getTimestamp(), entity.getTimestamp());
    assertEquals(record.getLastSeenTimestamp(), entity.getLastSeenTimestamp());
    assertEquals(SetUtils.set(FactEntity.Flag.RetractedHint, FactEntity.Flag.UsesSeparatedObjectFields), entity.getFlags());
  }

  @Test
  public void testToEntityWithAcl() {
    FactRecord record = new FactRecord().addAclEntry(new FactAclEntryRecord());

    FactEntity entity = converter.toEntity(record);
    assertTrue(entity.getFlags().contains(FactEntity.Flag.HasAcl));
  }

  @Test
  public void testToEntityWithComments() {
    FactRecord record = new FactRecord().addComment(new FactCommentRecord());

    FactEntity entity = converter.toEntity(record);
    assertTrue(entity.getFlags().contains(FactEntity.Flag.HasComments));
  }

  @Test
  public void testToEntityWithSourceObject() {
    FactRecord record = new FactRecord().setSourceObject(createObjectRecord());

    FactEntity entity = converter.toEntity(record);
    assertEquals(1, entity.getBindings().size());
    assertEquals(record.getSourceObject().getId(), entity.getSourceObjectID());
    assertEquals(record.getSourceObject().getId(), entity.getBindings().get(0).getObjectID());
    assertEquals(Direction.FactIsDestination, entity.getBindings().get(0).getDirection());
    assertFalse(entity.isSet(FactEntity.Flag.BidirectionalBinding));
  }

  @Test
  public void testToEntityWithDestinationObject() {
    FactRecord record = new FactRecord().setDestinationObject(createObjectRecord());

    FactEntity entity = converter.toEntity(record);
    assertEquals(1, entity.getBindings().size());
    assertEquals(record.getDestinationObject().getId(), entity.getDestinationObjectID());
    assertEquals(record.getDestinationObject().getId(), entity.getBindings().get(0).getObjectID());
    assertEquals(Direction.FactIsSource, entity.getBindings().get(0).getDirection());
    assertFalse(entity.isSet(FactEntity.Flag.BidirectionalBinding));
  }

  @Test
  public void testToEntityWithBidirectionalBinding() {
    FactRecord record = new FactRecord()
            .setSourceObject(createObjectRecord())
            .setDestinationObject(createObjectRecord())
            .setBidirectionalBinding(true);

    FactEntity entity = converter.toEntity(record);
    assertEquals(2, entity.getBindings().size());
    assertEquals(record.getSourceObject().getId(), entity.getSourceObjectID());
    assertEquals(record.getSourceObject().getId(), entity.getBindings().get(0).getObjectID());
    assertEquals(Direction.BiDirectional, entity.getBindings().get(0).getDirection());
    assertEquals(record.getDestinationObject().getId(), entity.getDestinationObjectID());
    assertEquals(record.getDestinationObject().getId(), entity.getBindings().get(1).getObjectID());
    assertEquals(Direction.BiDirectional, entity.getBindings().get(1).getDirection());
    assertTrue(entity.isSet(FactEntity.Flag.BidirectionalBinding));
  }

  @Test
  public void testToDocumentWithNullRecord() {
    assertNull(converter.toDocument(null));
  }

  @Test
  public void testToDocumentWithEmptyRecord() {
    assertNotNull(converter.toDocument(new FactRecord()));
  }

  @Test
  public void testToDocumentWithFullRecord() {
    FactRecord record = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value")
            .setInReferenceToID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setAddedByID(UUID.randomUUID())
            .setLastSeenByID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Explicit)
            .setConfidence(0.1f)
            .setTrust(0.2f)
            .setTimestamp(123456789L)
            .setLastSeenTimestamp(987654321L)
            .addFlag(FactRecord.Flag.TimeGlobalIndex);

    FactDocument document = converter.toDocument(record);
    assertEquals(record.getId(), document.getId());
    assertEquals(record.getTypeID(), document.getTypeID());
    assertEquals(record.getValue(), document.getValue());
    assertEquals(record.getInReferenceToID(), document.getInReferenceTo());
    assertEquals(record.getOrganizationID(), document.getOrganizationID());
    assertEquals(record.getOriginID(), document.getOriginID());
    assertEquals(record.getAddedByID(), document.getAddedByID());
    assertEquals(record.getLastSeenByID(), document.getLastSeenByID());
    assertEquals(record.getAccessMode().name(), document.getAccessMode().name());
    assertEquals(record.getConfidence(), document.getConfidence(), 0.0f);
    assertEquals(record.getTrust(), document.getTrust(), 0.0f);
    assertEquals(record.getTimestamp(), document.getTimestamp());
    assertEquals(record.getLastSeenTimestamp(), document.getLastSeenTimestamp());
    assertEquals(SetUtils.set(record.getFlags(), Enum::name), SetUtils.set(document.getFlags(), Enum::name));
  }

  @Test
  public void testToDocumentWithAcl() {
    UUID subjectID = UUID.randomUUID();
    FactRecord record = new FactRecord().addAclEntry(new FactAclEntryRecord().setSubjectID(subjectID));
    assertEquals(SetUtils.set(subjectID), converter.toDocument(record).getAcl());
  }

  @Test
  public void testToDocumentWithSourceObject() {
    ObjectRecord objectRecord = createObjectRecord();
    FactRecord factRecord = new FactRecord().setSourceObject(objectRecord);

    FactDocument factDocument = converter.toDocument(factRecord);
    assertEquals(1, factDocument.getObjects().size());
    assertObjectDocument(objectRecord, factDocument.getObjects().iterator().next(), ObjectDocument.Direction.FactIsDestination);
  }

  @Test
  public void testToDocumentWithDestinationObject() {
    ObjectRecord objectRecord = createObjectRecord();
    FactRecord factRecord = new FactRecord().setDestinationObject(objectRecord);

    FactDocument factDocument = converter.toDocument(factRecord);
    assertEquals(1, factDocument.getObjects().size());
    assertObjectDocument(objectRecord, factDocument.getObjects().iterator().next(), ObjectDocument.Direction.FactIsSource);
  }

  @Test
  public void testToDocumentWithBidirectionalBinding() {
    FactRecord record = new FactRecord()
            .setSourceObject(createObjectRecord())
            .setDestinationObject(createObjectRecord())
            .setBidirectionalBinding(true);

    FactDocument document = converter.toDocument(record);
    assertEquals(2, document.getObjects().size());
    assertTrue(document.getObjects().stream().allMatch(o -> ObjectDocument.Direction.BiDirectional == o.getDirection()));
  }

  private ObjectRecord createObjectRecord() {
    return new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");
  }

  private void assertObjectDocument(ObjectRecord expected, ObjectDocument actual, ObjectDocument.Direction direction) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getTypeID(), actual.getTypeID());
    assertEquals(expected.getValue(), actual.getValue());
    assertEquals(direction, actual.getDirection());
  }
}
