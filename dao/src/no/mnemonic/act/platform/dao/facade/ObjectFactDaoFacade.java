package no.mnemonic.act.platform.dao.facade;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.result.ScrollingSearchResult;
import no.mnemonic.act.platform.dao.elastic.result.SearchResult;
import no.mnemonic.act.platform.dao.facade.converters.FactAclEntryRecordConverter;
import no.mnemonic.act.platform.dao.facade.converters.FactCommentRecordConverter;
import no.mnemonic.act.platform.dao.facade.converters.FactRecordConverter;
import no.mnemonic.act.platform.dao.facade.converters.ObjectRecordConverter;
import no.mnemonic.act.platform.dao.facade.utilities.BatchingIterator;
import no.mnemonic.act.platform.dao.facade.utilities.MappingIterator;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ObjectFactDaoFacade implements ObjectFactDao {

  private final ObjectManager objectManager;
  private final FactManager factManager;
  private final FactSearchManager factSearchManager;
  private final ObjectRecordConverter objectRecordConverter;
  private final FactRecordConverter factRecordConverter;
  private final FactAclEntryRecordConverter factAclEntryRecordConverter;
  private final FactCommentRecordConverter factCommentRecordConverter;

  @Inject
  public ObjectFactDaoFacade(ObjectManager objectManager,
                             FactManager factManager,
                             FactSearchManager factSearchManager,
                             ObjectRecordConverter objectRecordConverter,
                             FactRecordConverter factRecordConverter,
                             FactAclEntryRecordConverter factAclEntryRecordConverter,
                             FactCommentRecordConverter factCommentRecordConverter) {
    this.objectManager = objectManager;
    this.factManager = factManager;
    this.factSearchManager = factSearchManager;
    this.objectRecordConverter = objectRecordConverter;
    this.factRecordConverter = factRecordConverter;
    this.factAclEntryRecordConverter = factAclEntryRecordConverter;
    this.factCommentRecordConverter = factCommentRecordConverter;
  }

  @Override
  public ObjectRecord getObject(UUID id) {
    // Just delegate to ObjectManager and convert result.
    return objectRecordConverter.fromEntity(objectManager.getObject(id));
  }

  @Override
  public ObjectRecord getObject(String type, String value) {
    // Just delegate to ObjectManager and convert result.
    return objectRecordConverter.fromEntity(objectManager.getObject(type, value));
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
    SearchResult<ObjectDocument> searchResult = factSearchManager.searchObjects(criteria);
    if (searchResult.getCount() <= 0) {
      // Return immediately if the search didn't yield any results.
      return ResultContainer.<ObjectRecord>builder().build();
    }

    // Iterator which maps ObjectDocument to UUID.
    Iterator<UUID> idIterator = new MappingIterator<>(searchResult.getValues().iterator(), ObjectDocument::getId);
    // Iterator which fetches ObjectEntity from Cassandra in batches.
    Iterator<ObjectEntity> batchingIterator = new BatchingIterator<>(idIterator, objectManager::getObjects);
    // Iterator which maps ObjectEntity to ObjectRecord.
    Iterator<ObjectRecord> recordIterator = new MappingIterator<>(batchingIterator, objectRecordConverter::fromEntity);

    return ResultContainer.<ObjectRecord>builder()
            .setCount(searchResult.getCount())
            .setValues(recordIterator)
            .build();
  }

  @Override
  public FactRecord getFact(UUID id) {
    // Just delegate to FactManager and convert result.
    return factRecordConverter.fromEntity(factManager.getFact(id));
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
    saveFactObjectBindings(entity);
    saveMetaFactBindings(entity);

    // Save all ACL entries and comments in Cassandra.
    saveAclEntries(record);
    saveComments(record);

    // Index new Fact in ElasticSearch.
    factSearchManager.indexFact(factRecordConverter.toDocument(record));

    return record;
  }

  @Override
  public FactRecord refreshFact(FactRecord record) {
    if (record == null) return null;

    factManager.refreshFact(record.getId());

    // Save new ACL entries and comments in Cassandra.
    saveAclEntries(record);
    saveComments(record);

    // After everything is saved reindex Fact in ElasticSearch.
    return reindexFact(record.getId());
  }

  @Override
  public FactRecord retractFact(FactRecord record) {
    if (record == null) return null;

    factManager.retractFact(record.getId());

    // Save new ACL entries and comments in Cassandra.
    saveAclEntries(record);
    saveComments(record);

    // After everything is saved reindex Fact in ElasticSearch.
    return reindexFact(record.getId());
  }

  @Override
  public ResultContainer<FactRecord> retrieveExistingFacts(FactRecord record) {
    // Search for existing Facts in ElasticSearch.
    SearchResult<FactDocument> searchResult = factSearchManager.retrieveExistingFacts(factRecordConverter.toCriteria(record));
    if (searchResult.getCount() <= 0) {
      // Return immediately if the search didn't yield any results.
      return ResultContainer.<FactRecord>builder().build();
    }

    return createResultContainer(searchResult.getValues().iterator(), searchResult.getCount());
  }

  @Override
  public ResultContainer<FactRecord> searchFacts(FactSearchCriteria criteria) {
    // Search for Facts in ElasticSearch.
    ScrollingSearchResult<FactDocument> searchResult = factSearchManager.searchFacts(criteria);
    if (searchResult.getCount() <= 0) {
      // Return immediately if the search didn't yield any results.
      return ResultContainer.<FactRecord>builder().build();
    }

    return createResultContainer(searchResult, searchResult.getCount());
  }

  @Override
  public FactAclEntryRecord storeFactAclEntry(FactRecord fact, FactAclEntryRecord aclEntry) {
    if (fact == null || aclEntry == null) return null;

    // Save new ACL entry and reindex Fact.
    saveAclEntry(fact, aclEntry);
    reindexFact(fact.getId());

    return aclEntry;
  }

  @Override
  public FactCommentRecord storeFactComment(FactRecord fact, FactCommentRecord comment) {
    if (fact == null || comment == null) return null;

    // Only save new comment. It's not required to reindex Fact.
    saveComment(fact, comment);

    return comment;
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

  private FactRecord reindexFact(UUID factID) {
    // getFact() will fetch all required information from Cassandra (the authoritative data store).
    // Because of that, the returned record will contain up-to-date information.
    FactRecord record = getFact(factID);
    // Simply reindex everything based on the fetched record.
    factSearchManager.indexFact(factRecordConverter.toDocument(record));
    // Return up-to-date record.
    return record;
  }

  private ResultContainer<FactRecord> createResultContainer(Iterator<FactDocument> results, int count) {
    // Iterator which maps FactDocument to UUID.
    Iterator<UUID> idIterator = new MappingIterator<>(results, FactDocument::getId);
    // Iterator which fetches FactEntity from Cassandra in batches.
    Iterator<FactEntity> batchingIterator = new BatchingIterator<>(idIterator, factManager::getFacts);
    // Iterator which maps FactEntity to FactRecord.
    Iterator<FactRecord> recordIterator = new MappingIterator<>(batchingIterator, factRecordConverter::fromEntity);

    return ResultContainer.<FactRecord>builder()
            .setCount(count)
            .setValues(recordIterator)
            .build();
  }
}
