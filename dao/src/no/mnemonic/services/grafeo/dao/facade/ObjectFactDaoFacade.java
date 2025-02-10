package no.mnemonic.services.grafeo.dao.facade;

import com.google.common.collect.Streams;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.services.grafeo.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactCommentRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.services.grafeo.dao.api.result.ResultContainer;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.*;
import no.mnemonic.services.grafeo.dao.elastic.FactSearchManager;
import no.mnemonic.services.grafeo.dao.elastic.document.FactDocument;
import no.mnemonic.services.grafeo.dao.elastic.result.ScrollingSearchResult;
import no.mnemonic.services.grafeo.dao.elastic.result.SearchResult;
import no.mnemonic.services.grafeo.dao.facade.converters.FactAclEntryRecordConverter;
import no.mnemonic.services.grafeo.dao.facade.converters.FactCommentRecordConverter;
import no.mnemonic.services.grafeo.dao.facade.converters.FactRecordConverter;
import no.mnemonic.services.grafeo.dao.facade.converters.ObjectRecordConverter;
import no.mnemonic.services.grafeo.dao.facade.helpers.FactRecordHasher;
import no.mnemonic.services.grafeo.dao.facade.resolvers.CachedFactResolver;
import no.mnemonic.services.grafeo.dao.facade.resolvers.CachedObjectResolver;

import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.Daily;
import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.TimeGlobal;

public class ObjectFactDaoFacade implements ObjectFactDao {

  private final ObjectManager objectManager;
  private final FactManager factManager;
  private final FactSearchManager factSearchManager;
  private final ObjectRecordConverter objectRecordConverter;
  private final FactRecordConverter factRecordConverter;
  private final FactAclEntryRecordConverter factAclEntryRecordConverter;
  private final FactCommentRecordConverter factCommentRecordConverter;
  private final CachedObjectResolver objectResolver;
  private final CachedFactResolver factResolver;
  private final Consumer<FactRecord> dcReplicationConsumer;

  @Inject
  public ObjectFactDaoFacade(ObjectManager objectManager,
                             FactManager factManager,
                             FactSearchManager factSearchManager,
                             ObjectRecordConverter objectRecordConverter,
                             FactRecordConverter factRecordConverter,
                             FactAclEntryRecordConverter factAclEntryRecordConverter,
                             FactCommentRecordConverter factCommentRecordConverter,
                             CachedObjectResolver objectResolver,
                             CachedFactResolver factResolver,
                             Consumer<FactRecord> dcReplicationConsumer) {
    this.objectManager = objectManager;
    this.factManager = factManager;
    this.factSearchManager = factSearchManager;
    this.objectRecordConverter = objectRecordConverter;
    this.factRecordConverter = factRecordConverter;
    this.factAclEntryRecordConverter = factAclEntryRecordConverter;
    this.factCommentRecordConverter = factCommentRecordConverter;
    this.objectResolver = objectResolver;
    this.factResolver = factResolver;
    this.dcReplicationConsumer = dcReplicationConsumer;
  }

  @Override
  public ObjectRecord getObject(UUID id) {
    return objectResolver.getObject(id);
  }

  @Override
  public ObjectRecord getObject(String type, String value) {
    return objectResolver.getObject(type, value);
  }

  @Override
  public ObjectRecord storeObject(ObjectRecord record) {
    if (record == null) return null;

    // Ensure that id is set before converting and saving entity.
    if (record.getId() == null) {
      record.setId(UUID.randomUUID());
    }

    objectManager.saveObject(objectRecordConverter.toEntity(record));
    return record;
  }

  @Override
  public ObjectStatisticsContainer calculateObjectStatistics(ObjectStatisticsCriteria criteria) {
    // Just delegate it to FactSearchManager, nothing else needs to be done.
    return factSearchManager.calculateObjectStatistics(criteria);
  }

  @Override
  public ResultContainer<ObjectRecord> searchObjects(FactSearchCriteria criteria) {
    // Search for Objects in ElasticSearch.
    SearchResult<UUID> searchResult = factSearchManager.searchObjects(criteria);
    if (searchResult.getCount() <= 0) {
      // Return immediately if the search didn't yield any results.
      return ResultContainer.<ObjectRecord>builder().build();
    }

    // Fetch Objects from Cassandra (or cache).
    Iterator<ObjectRecord> resultsIterator = searchResult.getValues()
            .stream()
            .map(objectResolver::getObject)
            .filter(Objects::nonNull)
            .iterator();

    return ResultContainer.<ObjectRecord>builder()
            .setCount(searchResult.getCount())
            .setValues(resultsIterator)
            .build();
  }

  @Override
  public FactRecord getFact(UUID id) {
    return factResolver.getFact(id);
  }

  @Override
  public FactRecord storeFact(FactRecord record) {
    if (record == null) return null;

    // Ensure that id is set.
    if (record.getId() == null) {
      record.setId(UUID.randomUUID());
    }

    // Save new Fact and lookup tables in Cassandra.
    FactEntity entity = factRecordConverter.toEntity(record);
    factManager.saveFact(entity);
    saveFactExistence(record);
    saveFactObjectBindings(entity);
    saveMetaFactBindings(entity);
    saveFactByTimestamp(entity);
    saveFactRefreshLog(record);

    // Save all ACL entries and comments in Cassandra.
    saveAclEntries(record);
    saveComments(record);

    // Index new Fact in ElasticSearch.
    indexFact(record);
    // Initiate data center replication.
    dcReplicationConsumer.accept(record);

    return record;
  }

  @Override
  public FactRecord refreshFact(FactRecord record) {
    if (record == null) return null;

    updateAndSaveFact(record, entity -> entity
            .setLastSeenTimestamp(record.getLastSeenTimestamp())
            .setLastSeenByID(record.getLastSeenByID())
    );

    // Save a new refresh log entry everytime a Fact is refreshed.
    saveFactRefreshLog(record);

    // Save new ACL entries and comments in Cassandra.
    saveAclEntries(record);
    saveComments(record);

    // After everything is saved reindex Fact in ElasticSearch.
    return reindexFact(record);
  }

  @Override
  public FactRecord retractFact(FactRecord record) {
    if (record == null) return null;

    updateAndSaveFact(record, entity -> entity.addFlag(FactEntity.Flag.RetractedHint));

    // Save new ACL entries and comments in Cassandra.
    saveAclEntries(record);
    saveComments(record);

    // After everything is saved reindex Fact in ElasticSearch.
    return reindexFact(record);
  }

  @Override
  public Optional<FactRecord> retrieveExistingFact(FactRecord record) {
    if (record == null) return Optional.empty();

    // Delegate resolution of Fact by its hash value to CachedFactResolver.
    // This will return null if the Fact doesn't exist yet.
    return Optional.ofNullable(factResolver.getFact(FactRecordHasher.toHash(record)));
  }

  @Override
  public ResultContainer<FactRecord> searchFacts(FactSearchCriteria criteria) {
    // Search for Facts in ElasticSearch.
    ScrollingSearchResult<UUID> searchResult = factSearchManager.searchFacts(criteria);
    if (searchResult.getCount() <= 0) {
      // Return immediately if the search didn't yield any results.
      return ResultContainer.<FactRecord>builder().build();
    }

    // Fetch Facts from Cassandra (or cache).
    Iterator<FactRecord> resultsIterator = Streams.stream(searchResult)
            .map(factResolver::getFact)
            .filter(Objects::nonNull)
            .iterator();

    return ResultContainer.<FactRecord>builder()
            .setCount(searchResult.getCount())
            .setValues(resultsIterator)
            .build();
  }

  @Override
  public FactAclEntryRecord storeFactAclEntry(FactRecord fact, FactAclEntryRecord aclEntry) {
    if (fact == null || aclEntry == null) return null;

    // Save new ACL entry and reindex Fact.
    saveAclEntry(fact, aclEntry);
    updateAndSaveFact(fact, entity -> entity.addFlag(FactEntity.Flag.HasAcl));
    reindexFact(fact);

    return aclEntry;
  }

  @Override
  public FactCommentRecord storeFactComment(FactRecord fact, FactCommentRecord comment) {
    if (fact == null || comment == null) return null;

    // Only save new comment. It's not required to reindex Fact.
    saveComment(fact, comment);
    updateAndSaveFact(fact, entity -> entity.addFlag(FactEntity.Flag.HasComments));
    factResolver.evict(fact);

    return comment;
  }

  @Override
  public Iterator<FactRecord> retrieveObjectFacts(UUID id) {
    // Use Cassandra lookup table to resolve all Facts bound to the given Object ID.
    return Streams.stream(objectManager.fetchObjectFactBindings(id))
            .map(ObjectFactBindingEntity::getFactID)
            .map(factResolver::getFact)
            .filter(Objects::nonNull)
            .iterator();
  }

  @Override
  public Iterator<FactRecord> retrieveMetaFacts(UUID id) {
    // Use Cassandra lookup table to resolve all meta Facts bound to the given Fact ID.
    return Streams.stream(factManager.fetchMetaFactBindings(id))
            .map(MetaFactBindingEntity::getMetaFactID)
            .map(factResolver::getFact)
            .filter(Objects::nonNull)
            .iterator();
  }

  private void saveFactExistence(FactRecord fact) {
    // Calculate hash value for given Fact.
    String hash = FactRecordHasher.toHash(fact);

    // Save FactExistence lookup table entry.
    factManager.saveFactExistence(new FactExistenceEntity()
            .setFactHash(hash)
            .setFactID(fact.getId())
    );
  }

  private void saveFactObjectBindings(FactEntity fact) {
    if (CollectionUtils.isEmpty(fact.getBindings())) return;

    // Save all bindings between Objects and the new Fact.
    for (FactEntity.FactObjectBinding binding : fact.getBindings()) {
      objectManager.saveObjectFactBinding(new ObjectFactBindingEntity()
              .setObjectID(binding.getObjectID())
              .setFactID(fact.getId())
              .setDirection(binding.getDirection())
      );
    }
  }

  private void saveMetaFactBindings(FactEntity fact) {
    if (fact.getInReferenceToID() == null) return;

    // Save binding between referenced Fact and new meta Fact.
    factManager.saveMetaFactBinding(new MetaFactBindingEntity()
            .setFactID(fact.getInReferenceToID())
            .setMetaFactID(fact.getId())
    );
  }

  private void saveFactByTimestamp(FactEntity fact) {
    // Calculate the correct time bucket (truncate minutes, seconds, ...).
    long hourOfDay = Instant.ofEpochMilli(fact.getTimestamp())
            .truncatedTo(ChronoUnit.HOURS)
            .toEpochMilli();

    // Save FactByTimestamp lookup table entry.
    factManager.saveFactByTimestamp(new FactByTimestampEntity()
            .setHourOfDay(hourOfDay)
            .setTimestamp(fact.getTimestamp())
            .setFactID(fact.getId())
    );
  }

  private void saveFactRefreshLog(FactRecord fact) {
    factManager.saveFactRefreshLogEntry(new FactRefreshLogEntity()
            .setFactID(fact.getId())
            .setRefreshTimestamp(fact.getLastSeenTimestamp())
            .setRefreshedByID(fact.getLastSeenByID())
    );
  }

  private void saveAclEntries(FactRecord fact) {
    if (CollectionUtils.isEmpty(fact.getAcl())) return;

    // Make sure to not add duplicates. This list will be empty for new Facts.
    Set<UUID> existingAcl = factManager.fetchFactAcl(fact.getId())
            .stream()
            .map(FactAclEntity::getId)
            .collect(Collectors.toSet());
    // Only save new entries.
    fact.getAcl()
            .stream()
            .filter(entry -> entry.getId() == null || !existingAcl.contains(entry.getId()))
            .forEach(entry -> saveAclEntry(fact, entry));
  }

  private void saveAclEntry(FactRecord fact, FactAclEntryRecord entry) {
    // Ensure that id is set before converting and saving entity.
    if (entry.getId() == null) {
      entry.setId(UUID.randomUUID());
    }

    factManager.saveFactAclEntry(factAclEntryRecordConverter.toEntity(entry, fact.getId()));
  }

  private void saveComments(FactRecord fact) {
    if (CollectionUtils.isEmpty(fact.getComments())) return;

    // Make sure to not add duplicates. This list will be empty for new Facts.
    Set<UUID> existingComments = factManager.fetchFactComments(fact.getId())
            .stream()
            .map(FactCommentEntity::getId)
            .collect(Collectors.toSet());
    // Only save new entries.
    fact.getComments()
            .stream()
            .filter(comment -> comment.getId() == null || !existingComments.contains(comment.getId()))
            .forEach(comment -> saveComment(fact, comment));
  }

  private void saveComment(FactRecord fact, FactCommentRecord comment) {
    // Ensure that id is set before converting and saving entity.
    if (comment.getId() == null) {
      comment.setId(UUID.randomUUID());
    }

    factManager.saveFactComment(factCommentRecordConverter.toEntity(comment, fact.getId()));
  }

  private void updateAndSaveFact(FactRecord record, FactEntityUpdater updater) {
    // Fetch Fact directly from Cassandra to avoid stale cache issues.
    FactEntity entity = factManager.getFact(record.getId());
    if (entity == null) {
      // If this ever happens it's a bug in the code calling ObjectFactDaoFacade!
      throw new IllegalStateException(String.format("Could not fetch Fact with id = %s from Cassandra.", record.getId()));
    }

    // Apply changes to entity.
    updater.update(entity);

    // Ensure that the HasAcl and HasComments flags are set.
    if (!CollectionUtils.isEmpty(record.getAcl())) {
      entity.addFlag(FactEntity.Flag.HasAcl);
    }

    if (!CollectionUtils.isEmpty(record.getComments())) {
      entity.addFlag(FactEntity.Flag.HasComments);
    }

    // Write changes back to Cassandra.
    factManager.saveFact(entity);
  }

  private FactRecord reindexFact(FactRecord fact) {
    // Evict cached entry to force a reload from Cassandra (the authoritative data store).
    // Because of that, the returned record will contain up-to-date information.
    factResolver.evict(fact);
    FactRecord record = factResolver.getFact(fact.getId());
    // Simply reindex everything based on the fetched record.
    indexFact(record);
    // Initiate data center replication to propagate changes.
    dcReplicationConsumer.accept(record);
    // Return up-to-date record.
    return record;
  }

  private void indexFact(FactRecord fact) {
    FactDocument document = factRecordConverter.toDocument(fact);

    if (fact.isSet(FactRecord.Flag.TimeGlobalIndex)) {
      factSearchManager.indexFact(document, TimeGlobal);
    } else {
      factSearchManager.indexFact(document, Daily);
    }
  }

  private interface FactEntityUpdater {
    void update(FactEntity entity);
  }
}
