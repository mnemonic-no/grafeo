package no.mnemonic.act.platform.cli.tools.converters;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Converter between {@link FactEntity} returned from Cassandra and {@link FactDocument} required by ElasticSearch.
 */
public class FactEntityToDocumentConverter implements Function<FactEntity, FactDocument> {

  private final FactManager factManager;
  private final ObjectManager objectManager;

  @Inject
  public FactEntityToDocumentConverter(FactManager factManager, ObjectManager objectManager) {
    this.factManager = factManager;
    this.objectManager = objectManager;
  }

  @Override
  public FactDocument apply(FactEntity entity) {
    if (entity == null) return null;

    return new FactDocument()
            .setId(entity.getId())
            .setTypeID(entity.getTypeID())
            .setValue(entity.getValue())
            .setInReferenceTo(entity.getInReferenceToID())
            .setOrganizationID(entity.getOrganizationID())
            .setOriginID(entity.getOriginID())
            .setAddedByID(entity.getAddedByID())
            .setLastSeenByID(entity.getLastSeenByID())
            .setAccessMode(ObjectUtils.ifNotNull(entity.getAccessMode(), m -> FactDocument.AccessMode.valueOf(m.name())))
            .setConfidence(entity.getConfidence())
            .setTrust(entity.getTrust())
            .setTimestamp(entity.getTimestamp())
            .setLastSeenTimestamp(entity.getLastSeenTimestamp())
            .setAcl(resolveAcl(entity))
            .setObjects(resolveObjects(entity));
  }

  private Set<UUID> resolveAcl(FactEntity entity) {
    return SetUtils.set(factManager.fetchFactAcl(entity.getId()), FactAclEntity::getSubjectID);
  }

  private Set<ObjectDocument> resolveObjects(FactEntity entity) {
    if (CollectionUtils.isEmpty(entity.getBindings())) return null;

    Set<ObjectDocument> objects = SetUtils.set();
    for (FactEntity.FactObjectBinding binding : entity.getBindings()) {
      ObjectEntity objectEntity = objectManager.getObject(binding.getObjectID());
      if (objectEntity == null) continue;

      objects.add(new ObjectDocument()
              .setId(objectEntity.getId())
              .setTypeID(objectEntity.getTypeID())
              .setValue(objectEntity.getValue())
              .setDirection(ObjectUtils.ifNotNull(binding.getDirection(), d -> ObjectDocument.Direction.valueOf(d.name()))));
    }

    return objects;
  }
}
