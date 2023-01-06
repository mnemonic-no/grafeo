package no.mnemonic.act.platform.dao.facade.converters;

import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.facade.resolvers.CachedObjectResolver;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.util.UUID;

import static no.mnemonic.act.platform.dao.cassandra.entity.Direction.FactIsDestination;
import static no.mnemonic.act.platform.dao.cassandra.entity.Direction.FactIsSource;

/**
 * Class for converting {@link FactRecord}s.
 */
public class FactRecordConverter {

  private static final Logger LOGGER = Logging.getLogger(FactRecordConverter.class);

  private final FactManager factManager;
  private final CachedObjectResolver objectResolver;
  private final FactAclEntryRecordConverter factAclEntryRecordConverter;
  private final FactCommentRecordConverter factCommentRecordConverter;

  @Inject
  public FactRecordConverter(FactManager factManager,
                             CachedObjectResolver objectResolver,
                             FactAclEntryRecordConverter factAclEntryRecordConverter,
                             FactCommentRecordConverter factCommentRecordConverter) {
    this.factManager = factManager;
    this.objectResolver = objectResolver;
    this.factAclEntryRecordConverter = factAclEntryRecordConverter;
    this.factCommentRecordConverter = factCommentRecordConverter;
  }

  /**
   * Convert {@link FactEntity} to {@link FactRecord}.
   *
   * @param entity Fact to convert
   * @return Converted Fact
   */
  public FactRecord fromEntity(FactEntity entity) {
    if (entity == null) return null;

    // Set all fields directly available on entity.
    FactRecord record = new FactRecord()
            .setId(entity.getId())
            .setTypeID(entity.getTypeID())
            .setValue(entity.getValue())
            .setInReferenceToID(entity.getInReferenceToID())
            .setOrganizationID(entity.getOrganizationID())
            .setOriginID(entity.getOriginID())
            .setAddedByID(entity.getAddedByID())
            .setLastSeenByID(entity.getLastSeenByID())
            .setAccessMode(ObjectUtils.ifNotNull(entity.getAccessMode(), m -> FactRecord.AccessMode.valueOf(m.name())))
            .setConfidence(entity.getConfidence())
            .setTrust(entity.getTrust())
            .setTimestamp(entity.getTimestamp())
            .setLastSeenTimestamp(entity.getLastSeenTimestamp());

    for (FactEntity.Flag flag : SetUtils.set(entity.getFlags())) {
      if (flag.isCassandraOnly()) continue;
      record.addFlag(FactRecord.Flag.valueOf(flag.name()));
    }

    // Populate with records from related entities.
    populateObjects(record, entity);

    if (entity.isSet(FactEntity.Flag.HasAcl)) {
      populateFactAcl(record);
    }

    if (entity.isSet(FactEntity.Flag.HasComments)) {
      populateFactComments(record);
    }

    return record;
  }

  /**
   * Convert {@link FactRecord} to {@link FactEntity}.
   *
   * @param record Fact to convert
   * @return Converted Fact
   */
  public FactEntity toEntity(FactRecord record) {
    if (record == null) return null;

    FactEntity entity = new FactEntity()
            .setId(record.getId())
            .setTypeID(record.getTypeID())
            .setValue(record.getValue())
            .setInReferenceToID(record.getInReferenceToID())
            .setOrganizationID(record.getOrganizationID())
            .setOriginID(record.getOriginID())
            .setAddedByID(record.getAddedByID())
            .setLastSeenByID(record.getLastSeenByID())
            .setAccessMode(ObjectUtils.ifNotNull(record.getAccessMode(), m -> AccessMode.valueOf(m.name())))
            .setConfidence(record.getConfidence())
            .setTrust(record.getTrust())
            .setTimestamp(record.getTimestamp())
            .setLastSeenTimestamp(record.getLastSeenTimestamp())
            .setFlags(SetUtils.set(record.getFlags(), flag -> FactEntity.Flag.valueOf(flag.name())));

    if (!CollectionUtils.isEmpty(record.getAcl())) {
      entity.addFlag(FactEntity.Flag.HasAcl);
    }

    if (!CollectionUtils.isEmpty(record.getComments())) {
      entity.addFlag(FactEntity.Flag.HasComments);
    }

    if (record.isBidirectionalBinding()) {
      entity.addFlag(FactEntity.Flag.BidirectionalBinding);
    }

    if (record.getSourceObject() != null) {
      entity.setSourceObjectID(record.getSourceObject().getId());
      entity.addBinding(new FactEntity.FactObjectBinding()
              .setObjectID(record.getSourceObject().getId())
              .setDirection(record.isBidirectionalBinding() ? Direction.BiDirectional : Direction.FactIsDestination));
    }

    if (record.getDestinationObject() != null) {
      entity.setDestinationObjectID(record.getDestinationObject().getId());
      entity.addBinding(new FactEntity.FactObjectBinding()
              .setObjectID(record.getDestinationObject().getId())
              .setDirection(record.isBidirectionalBinding() ? Direction.BiDirectional : Direction.FactIsSource));
    }

    // Always set this flag to distinguish between Facts which use the 'source_object_id'
    // plus 'destination_object_id' fields and the deprecated 'bindings' field.
    entity.addFlag(FactEntity.Flag.UsesSeparatedObjectFields);

    return entity;
  }

  /**
   * Convert {@link FactRecord} to {@link FactDocument}.
   *
   * @param record Fact to convert
   * @return Converted Fact
   */
  public FactDocument toDocument(FactRecord record) {
    if (record == null) return null;

    FactDocument document = new FactDocument()
            .setId(record.getId())
            .setTypeID(record.getTypeID())
            .setValue(record.getValue())
            .setInReferenceTo(record.getInReferenceToID())
            .setOrganizationID(record.getOrganizationID())
            .setOriginID(record.getOriginID())
            .setAddedByID(record.getAddedByID())
            .setLastSeenByID(record.getLastSeenByID())
            .setAccessMode(ObjectUtils.ifNotNull(record.getAccessMode(), m -> FactDocument.AccessMode.valueOf(m.name())))
            .setConfidence(record.getConfidence())
            .setTrust(record.getTrust())
            .setTimestamp(record.getTimestamp())
            .setLastSeenTimestamp(record.getLastSeenTimestamp())
            .setAcl(SetUtils.set(record.getAcl(), FactAclEntryRecord::getSubjectID))
            .setFlags(SetUtils.set(record.getFlags(), flag -> FactDocument.Flag.valueOf(flag.name())));

    if (record.getSourceObject() != null) {
      ObjectDocument.Direction direction = record.isBidirectionalBinding() ? ObjectDocument.Direction.BiDirectional : ObjectDocument.Direction.FactIsDestination;
      document.addObject(toDocument(record.getSourceObject(), direction));
    }

    if (record.getDestinationObject() != null) {
      ObjectDocument.Direction direction = record.isBidirectionalBinding() ? ObjectDocument.Direction.BiDirectional : ObjectDocument.Direction.FactIsSource;
      document.addObject(toDocument(record.getDestinationObject(), direction));
    }

    return document;
  }

  private void populateObjects(FactRecord record, FactEntity entity) {
    // If 'UsesSeparatedObjectFields' is set populate Objects from the new 'source_object_id' and 'destination_object_id' fields.
    // Otherwise, this is an 'old' Fact and the Objects need to be populated from the 'bindings' field.
    if (entity.isSet(FactEntity.Flag.UsesSeparatedObjectFields)) {
      populateObjectsFromSourceDestinationFields(record, entity);
    } else {
      populateObjectsFromBindingsField(record, entity);
    }
  }

  private void populateObjectsFromSourceDestinationFields(FactRecord record, FactEntity entity) {
    ObjectUtils.ifNotNullDo(entity.getSourceObjectID(), id -> record.setSourceObject(convertObject(id)));
    ObjectUtils.ifNotNullDo(entity.getDestinationObjectID(), id -> record.setDestinationObject(convertObject(id)));
    record.setBidirectionalBinding(entity.isSet(FactEntity.Flag.BidirectionalBinding));
  }

  private void populateObjectsFromBindingsField(FactRecord record, FactEntity entity) {
    if (CollectionUtils.isEmpty(entity.getBindings())) return;

    if (CollectionUtils.size(entity.getBindings()) == 1) {
      populateObjectsWithCardinalityOne(record, entity.getBindings().get(0));
    } else if (CollectionUtils.size(entity.getBindings()) == 2) {
      populateObjectsWithCardinalityTwo(record, entity.getBindings().get(0), entity.getBindings().get(1));
    } else {
      // This should never happen as long as create Fact API only allows bindings with cardinality 1 or 2. Log it, just in case.
      LOGGER.warning("Fact is bound to more than two Objects (id = %s). Ignoring Objects in result.", record.getId());
    }
  }

  private void populateObjectsWithCardinalityOne(FactRecord record, FactEntity.FactObjectBinding binding) {
    if (binding.getDirection() == FactIsDestination) {
      record.setSourceObject(convertObject(binding.getObjectID()));
    } else if (binding.getDirection() == FactIsSource) {
      record.setDestinationObject(convertObject(binding.getObjectID()));
    } else {
      // In case of bidirectional binding with cardinality 1 populate source and destination with same Object.
      ObjectRecord object = convertObject(binding.getObjectID());
      record.setSourceObject(object)
              .setDestinationObject(object)
              .setBidirectionalBinding(true);
    }
  }

  private void populateObjectsWithCardinalityTwo(FactRecord record, FactEntity.FactObjectBinding first, FactEntity.FactObjectBinding second) {
    if ((first.getDirection() == FactIsDestination && second.getDirection() == FactIsDestination) ||
            (first.getDirection() == FactIsSource && second.getDirection() == FactIsSource)) {
      // This should never happen as long as create Fact API only allows bindings with cardinality 1 or 2. Log it, just in case.
      LOGGER.warning("Fact is bound to two Objects with the same direction (id = %s). Ignoring Objects in result.", record.getId());
      return;
    }

    if (first.getDirection() == FactIsDestination) {
      // If 'first' has direction 'FactIsDestination' it's the source Object and 'second' the destination Object ...
      record.setSourceObject(convertObject(first.getObjectID()))
              .setDestinationObject(convertObject(second.getObjectID()));
    } else if (second.getDirection() == FactIsDestination) {
      // ... and vice versa. They can't have the same direction!
      record.setSourceObject(convertObject(second.getObjectID()))
              .setDestinationObject(convertObject(first.getObjectID()));
    } else {
      // With bidirectional binding it doesn't matter which Object is source/destination.
      // In order to be consistent always set first as source and second as destination.
      record.setSourceObject(convertObject(first.getObjectID()))
              .setDestinationObject(convertObject(second.getObjectID()))
              .setBidirectionalBinding(true);
    }
  }

  private void populateFactAcl(FactRecord record) {
    for (FactAclEntity entity : factManager.fetchFactAcl(record.getId())) {
      record.addAclEntry(factAclEntryRecordConverter.fromEntity(entity));
    }
  }

  private void populateFactComments(FactRecord record) {
    for (FactCommentEntity entity : factManager.fetchFactComments(record.getId())) {
      record.addComment(factCommentRecordConverter.fromEntity(entity));
    }
  }

  private ObjectRecord convertObject(UUID objectID) {
    return objectResolver.getObject(objectID);
  }

  private ObjectDocument toDocument(ObjectRecord record, ObjectDocument.Direction direction) {
    return new ObjectDocument()
            .setId(record.getId())
            .setTypeID(record.getTypeID())
            .setValue(record.getValue())
            .setDirection(direction);
  }
}
