package no.mnemonic.act.platform.dao.facade;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.FactSearchCriteria;
import no.mnemonic.act.platform.dao.api.criteria.ObjectStatisticsCriteria;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.result.SearchResult;
import no.mnemonic.act.platform.dao.facade.converters.ObjectRecordConverter;
import no.mnemonic.act.platform.dao.facade.utilities.BatchingIterator;
import no.mnemonic.act.platform.dao.facade.utilities.MappingIterator;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.UUID;

public class ObjectFactDaoFacade implements ObjectFactDao {

  private final ObjectManager objectManager;
  private final FactSearchManager factSearchManager;
  private final ObjectRecordConverter objectRecordConverter;

  @Inject
  public ObjectFactDaoFacade(ObjectManager objectManager,
                             FactSearchManager factSearchManager,
                             ObjectRecordConverter objectRecordConverter) {
    this.objectManager = objectManager;
    this.factSearchManager = factSearchManager;
    this.objectRecordConverter = objectRecordConverter;
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

    // Convert record to entity and ensure that id is set.
    ObjectEntity entity = objectRecordConverter.toEntity(record);
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID());
    }

    // Delegate to ObjectManager and convert result.
    return objectRecordConverter.fromEntity(objectManager.saveObject(entity));
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
}
