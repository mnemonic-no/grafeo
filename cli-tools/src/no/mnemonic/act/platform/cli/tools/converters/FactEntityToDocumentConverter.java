package no.mnemonic.act.platform.cli.tools.converters;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactRefreshLogEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Converter between {@link FactEntity} returned from Cassandra and {@link FactDocument} required by ElasticSearch.
 */
public class FactEntityToDocumentConverter implements BiFunction<FactEntity, FactRefreshLogEntity, FactDocument> {

  private final FactManager factManager;
  private final ObjectManager objectManager;

  @Inject
  public FactEntityToDocumentConverter(FactManager factManager, ObjectManager objectManager) {
    this.factManager = factManager;
    this.objectManager = objectManager;
  }

  @Override
  public FactDocument apply(FactEntity fact, FactRefreshLogEntity logEntry) {
    if (fact == null) return null;

    return new FactDocument()
            .setId(fact.getId())
            .setTypeID(fact.getTypeID())
            .setValue(fact.getValue())
            .setInReferenceTo(fact.getInReferenceToID())
            .setOrganizationID(fact.getOrganizationID())
            .setOriginID(fact.getOriginID())
            .setAddedByID(fact.getAddedByID())
            .setTimestamp(fact.getTimestamp())
            // To reconstruct the correct history set lastSeenByID and lastSeenTimestamp to the values from the refresh log.
            // lastSeenTimestamp is especially important because it decides into which daily index the Fact will be indexed.
            .setLastSeenByID(logEntry == null ? fact.getLastSeenByID() : logEntry.getRefreshedByID())
            .setLastSeenTimestamp(logEntry == null ? fact.getLastSeenTimestamp() : logEntry.getRefreshTimestamp())
            .setAccessMode(ObjectUtils.ifNotNull(fact.getAccessMode(), m -> FactDocument.AccessMode.valueOf(m.name())))
            .setConfidence(fact.getConfidence())
            .setTrust(fact.getTrust())
            .setAcl(resolveAcl(fact, logEntry))
            .setObjects(resolveObjects(fact));
  }

  private Set<UUID> resolveAcl(FactEntity fact, FactRefreshLogEntity logEntry) {
    if (!fact.isSet(FactEntity.Flag.HasAcl)) return null;

    if (logEntry == null) {
      return SetUtils.set(factManager.fetchFactAcl(fact.getId()), FactAclEntity::getSubjectID);
    }

    // refreshTimestamp decides into which index the Fact will be indexed, however,
    // all ACL entries from the entire day should be included in that document.
    long indexEndTimestamp = Instant.ofEpochMilli(logEntry.getRefreshTimestamp())
            .truncatedTo(ChronoUnit.DAYS)
            .plus(1, ChronoUnit.DAYS)
            .toEpochMilli();

    // Only include ACL entries created before or during the index day.
    return factManager.fetchFactAcl(fact.getId())
            .stream()
            .filter(entry -> entry.getTimestamp() < indexEndTimestamp)
            .map(FactAclEntity::getSubjectID)
            .collect(Collectors.toSet());
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
