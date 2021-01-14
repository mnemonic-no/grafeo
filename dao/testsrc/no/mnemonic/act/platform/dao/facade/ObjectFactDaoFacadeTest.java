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
import no.mnemonic.act.platform.dao.elastic.criteria.FactExistenceSearchCriteria;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.dao.elastic.document.ObjectDocument;
import no.mnemonic.act.platform.dao.elastic.result.ScrollingSearchResult;
import no.mnemonic.act.platform.dao.elastic.result.SearchResult;
import no.mnemonic.act.platform.dao.facade.converters.FactAclEntryRecordConverter;
import no.mnemonic.act.platform.dao.facade.converters.FactCommentRecordConverter;
import no.mnemonic.act.platform.dao.facade.converters.FactRecordConverter;
import no.mnemonic.act.platform.dao.facade.converters.ObjectRecordConverter;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectFactDaoFacadeTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private FactManager factManager;
  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private ObjectRecordConverter objectRecordConverter;
  @Mock
  private FactRecordConverter factRecordConverter;
  @Mock
  private FactAclEntryRecordConverter factAclEntryRecordConverter;
  @Mock
  private FactCommentRecordConverter factCommentRecordConverter;
  @Mock
  private Consumer<FactRecord> dcReplicationConsumer;

  private ObjectFactDao dao;

  @Before
  public void setUp() {
    initMocks(this);
    dao = new ObjectFactDaoFacade(
            objectManager,
            factManager,
            factSearchManager,
            objectRecordConverter,
            factRecordConverter,
            factAclEntryRecordConverter,
            factCommentRecordConverter,
            dcReplicationConsumer
    );
  }

  @Test
  public void testGetObjectById() {
    UUID id = UUID.randomUUID();
    ObjectEntity entity = new ObjectEntity();
    ObjectRecord record = new ObjectRecord();
    when(objectManager.getObject(id)).thenReturn(entity);
    when(objectRecordConverter.fromEntity(entity)).thenReturn(record);

    assertSame(record, dao.getObject(id));
    verify(objectManager).getObject(id);
    verify(objectRecordConverter).fromEntity(entity);
  }

  @Test
  public void testGetObjectByTypeValue() {
    String type = "type";
    String value = "value";
    ObjectEntity entity = new ObjectEntity();
    ObjectRecord record = new ObjectRecord();
    when(objectManager.getObject(type, value)).thenReturn(entity);
    when(objectRecordConverter.fromEntity(entity)).thenReturn(record);

    assertSame(record, dao.getObject(type, value));
    verify(objectManager).getObject(type, value);
    verify(objectRecordConverter).fromEntity(entity);
  }

  @Test
  public void testStoreObjectWithNull() {
    assertNull(dao.storeObject(null));
  }

  @Test
  public void testStoreObjectSavesEntity() {
    ObjectRecord record = new ObjectRecord().setId(UUID.randomUUID());
    ObjectEntity entity = new ObjectEntity().setId(record.getId());
    when(objectRecordConverter.toEntity(record)).thenReturn(entity);

    assertSame(record, dao.storeObject(record));
    verify(objectRecordConverter).toEntity(argThat(r -> r.getId() == record.getId()));
    verify(objectManager).saveObject(entity);
  }

  @Test
  public void testStoreObjectSetsId() {
    ObjectRecord record = new ObjectRecord();
    ObjectEntity entity = new ObjectEntity();
    when(objectRecordConverter.toEntity(record)).thenReturn(entity);

    assertSame(record, dao.storeObject(record));
    verify(objectRecordConverter).toEntity(argThat(r -> r.getId() != null));
    verify(objectManager).saveObject(entity);
  }

  @Test
  public void testCalculateObjectStatistics() {
    ObjectStatisticsCriteria criteria = ObjectStatisticsCriteria.builder()
            .addObjectID(UUID.randomUUID())
            .addAvailableOrganizationID(UUID.randomUUID())
            .setCurrentUserID(UUID.randomUUID())
            .build();
    ObjectStatisticsContainer container = ObjectStatisticsContainer.builder().build();
    when(factSearchManager.calculateObjectStatistics(criteria)).thenReturn(container);

    assertSame(container, dao.calculateObjectStatistics(criteria));
    verify(factSearchManager).calculateObjectStatistics(criteria);
  }

  @Test
  public void testSearchObjectsWithoutSearchResult() {
    FactSearchCriteria criteria = createFactSearchCriteria();
    when(factSearchManager.searchObjects(criteria)).thenReturn(SearchResult.<ObjectDocument>builder().build());

    ResultContainer<ObjectRecord> container = dao.searchObjects(criteria);
    assertEquals(0, container.getCount());
    assertFalse(container.hasNext());
    verify(factSearchManager).searchObjects(criteria);
    verifyNoInteractions(objectManager);
  }

  @Test
  public void testSearchObjectsWithSearchResult() {
    UUID id = UUID.randomUUID();
    ObjectDocument document = new ObjectDocument().setId(id);
    ObjectEntity entity = new ObjectEntity().setId(id);
    ObjectRecord record = new ObjectRecord().setId(id);

    FactSearchCriteria criteria = createFactSearchCriteria();
    when(factSearchManager.searchObjects(criteria))
            .thenReturn(SearchResult.<ObjectDocument>builder().setCount(1).addValue(document).build());
    when(objectManager.getObjects(anyList())).thenReturn(ListUtils.list(entity).iterator());
    when(objectRecordConverter.fromEntity(entity)).thenReturn(record);

    ResultContainer<ObjectRecord> container = dao.searchObjects(criteria);
    assertEquals(1, container.getCount());
    assertEquals(ListUtils.list(record), ListUtils.list(container));
    verify(factSearchManager).searchObjects(criteria);
    verify(objectManager).getObjects(argThat(list -> list.contains(id)));
    verify(objectRecordConverter).fromEntity(entity);
  }

  @Test
  public void testGetFactById() {
    UUID id = UUID.randomUUID();
    FactEntity entity = new FactEntity();
    FactRecord record = new FactRecord();
    when(factManager.getFact(id)).thenReturn(entity);
    when(factRecordConverter.fromEntity(entity)).thenReturn(record);

    assertSame(record, dao.getFact(id));
    verify(factManager).getFact(id);
    verify(factRecordConverter).fromEntity(entity);
  }

  @Test
  public void testStoreFactWithNull() {
    assertNull(dao.storeFact(null));
  }

  @Test
  public void testStoreFactSavesEntity() {
    FactRecord record = new FactRecord().setId(UUID.randomUUID());
    FactEntity entity = new FactEntity().setId(record.getId());
    FactDocument document = new FactDocument().setId(record.getId());
    when(factRecordConverter.toEntity(record)).thenReturn(entity);
    when(factRecordConverter.toDocument(record)).thenReturn(document);

    assertSame(record, dao.storeFact(record));
    verify(factManager).saveFact(entity);
    verify(factSearchManager).indexFact(document);
    verify(factRecordConverter).toEntity(argThat(r -> r.getId() == record.getId()));
    verify(factRecordConverter).toDocument(argThat(r -> r.getId() == record.getId()));
  }

  @Test
  public void testStoreFactSetsId() {
    FactRecord record = new FactRecord();
    FactEntity entity = new FactEntity();
    FactDocument document = new FactDocument();
    when(factRecordConverter.toEntity(record)).thenReturn(entity);
    when(factRecordConverter.toDocument(record)).thenReturn(document);

    assertSame(record, dao.storeFact(record));
    verify(factManager).saveFact(entity);
    verify(factSearchManager).indexFact(document);
    verify(factRecordConverter).toEntity(argThat(r -> r.getId() != null));
    verify(factRecordConverter).toDocument(argThat(r -> r.getId() != null));
  }

  @Test
  public void testStoreFactSavesFactObjectBindings() {
    FactEntity.FactObjectBinding binding = new FactEntity.FactObjectBinding()
            .setObjectID(UUID.randomUUID())
            .setDirection(Direction.BiDirectional);
    FactEntity entity = new FactEntity()
            .setId(UUID.randomUUID())
            .addBinding(binding);
    when(factRecordConverter.toEntity(notNull())).thenReturn(entity);

    dao.storeFact(new FactRecord());
    verify(objectManager).saveObjectFactBinding(argThat(b -> {
      assertEquals(binding.getObjectID(), b.getObjectID());
      assertEquals(binding.getDirection(), b.getDirection());
      assertEquals(entity.getId(), b.getFactID());
      return true;
    }));
  }

  @Test
  public void testStoreFactSavesMetaFactBindings() {
    FactEntity entity = new FactEntity()
            .setId(UUID.randomUUID())
            .setInReferenceToID(UUID.randomUUID());
    when(factRecordConverter.toEntity(notNull())).thenReturn(entity);

    dao.storeFact(new FactRecord());
    verify(factManager).saveMetaFactBinding(argThat(b -> {
      assertEquals(entity.getInReferenceToID(), b.getFactID());
      assertEquals(entity.getId(), b.getMetaFactID());
      return true;
    }));
  }

  @Test
  public void testStoreFactSavesFactByTimestamp() {
    FactEntity entity = new FactEntity()
            .setId(UUID.randomUUID())
            .setTimestamp(1609504200000L);
    when(factRecordConverter.toEntity(notNull())).thenReturn(entity);

    dao.storeFact(new FactRecord());
    verify(factManager).saveFactByTimestamp(argThat(factByTimestamp -> {
      assertEquals(1609502400000L, factByTimestamp.getHourOfDay());
      assertEquals(entity.getTimestamp(), factByTimestamp.getTimestamp());
      assertEquals(entity.getId(), factByTimestamp.getFactID());
      return true;
    }));
  }

  @Test
  public void testStoreFactSavesAclEntry() {
    FactAclEntryRecord entry = new FactAclEntryRecord();
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addAclEntry(entry);
    when(factRecordConverter.toEntity(fact)).thenReturn(new FactEntity());
    when(factAclEntryRecordConverter.toEntity(entry, fact.getId())).thenReturn(new FactAclEntity());

    dao.storeFact(fact);
    verify(factManager).saveFactAclEntry(notNull());
    verify(factAclEntryRecordConverter).toEntity(argThat(r -> r.getId() != null), eq(fact.getId()));
  }

  @Test
  public void testStoreFactSavesComment() {
    FactCommentRecord comment = new FactCommentRecord();
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addComment(comment);
    when(factRecordConverter.toEntity(fact)).thenReturn(new FactEntity());
    when(factCommentRecordConverter.toEntity(comment, fact.getId())).thenReturn(new FactCommentEntity());

    dao.storeFact(fact);
    verify(factManager).saveFactComment(notNull());
    verify(factCommentRecordConverter).toEntity(argThat(r -> r.getId() != null), eq(fact.getId()));
  }

  @Test
  public void testStoreFactInitiatesReplication() {
    FactRecord fact = new FactRecord();
    when(factRecordConverter.toEntity(fact)).thenReturn(new FactEntity());

    dao.storeFact(fact);
    verify(dcReplicationConsumer).accept(fact);
  }

  @Test
  public void testRefreshFactWithNull() {
    assertNull(dao.refreshFact(null));
  }

  @Test
  public void testRefreshFactUpdatesEntity() {
    FactRecord record = new FactRecord().setId(UUID.randomUUID());
    mockReindexingOfFact(record);

    assertNotNull(dao.refreshFact(record));
    verify(factManager).refreshFact(record.getId());
    verifyReindexingOfFact(record);
  }

  @Test
  public void testRefreshFactSavesAclEntry() {
    FactAclEntryRecord entry = new FactAclEntryRecord();
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addAclEntry(entry);
    when(factAclEntryRecordConverter.toEntity(entry, fact.getId())).thenReturn(new FactAclEntity());

    dao.refreshFact(fact);
    verify(factManager).saveFactAclEntry(notNull());
    verify(factAclEntryRecordConverter).toEntity(entry, fact.getId());
  }

  @Test
  public void testRefreshFactFiltersDuplicateAclEntry() {
    FactAclEntryRecord entry = new FactAclEntryRecord()
            .setId(UUID.randomUUID());
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addAclEntry(entry);
    when(factManager.fetchFactAcl(fact.getId()))
            .thenReturn(ListUtils.list(new FactAclEntity().setId(entry.getId())));

    dao.refreshFact(fact);
    verify(factManager, never()).saveFactAclEntry(any());
    verify(factManager).fetchFactAcl(fact.getId());
  }

  @Test
  public void testRefreshFactSavesComment() {
    FactCommentRecord comment = new FactCommentRecord();
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addComment(comment);
    when(factCommentRecordConverter.toEntity(comment, fact.getId())).thenReturn(new FactCommentEntity());

    dao.refreshFact(fact);
    verify(factManager).saveFactComment(notNull());
    verify(factCommentRecordConverter).toEntity(comment, fact.getId());
  }

  @Test
  public void testRefreshFactFiltersDuplicateComment() {
    FactCommentRecord comment = new FactCommentRecord()
            .setId(UUID.randomUUID());
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addComment(comment);
    when(factManager.fetchFactComments(fact.getId()))
            .thenReturn(ListUtils.list(new FactCommentEntity().setId(comment.getId())));

    dao.refreshFact(fact);
    verify(factManager, never()).saveFactComment(any());
    verify(factManager).fetchFactComments(fact.getId());
  }

  @Test
  public void testRetractFactWithNull() {
    assertNull(dao.retractFact(null));
  }

  @Test
  public void testRetractFactUpdatesEntity() {
    FactRecord record = new FactRecord().setId(UUID.randomUUID());
    mockReindexingOfFact(record);

    assertNotNull(dao.retractFact(record));
    verify(factManager).retractFact(record.getId());
    verifyReindexingOfFact(record);
  }

  @Test
  public void testRetractFactSavesAclEntry() {
    FactAclEntryRecord entry = new FactAclEntryRecord();
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addAclEntry(entry);
    when(factAclEntryRecordConverter.toEntity(entry, fact.getId())).thenReturn(new FactAclEntity());

    dao.retractFact(fact);
    verify(factManager).saveFactAclEntry(notNull());
    verify(factAclEntryRecordConverter).toEntity(entry, fact.getId());
  }

  @Test
  public void testRetractFactFiltersDuplicateAclEntry() {
    FactAclEntryRecord entry = new FactAclEntryRecord()
            .setId(UUID.randomUUID());
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addAclEntry(entry);
    when(factManager.fetchFactAcl(fact.getId()))
            .thenReturn(ListUtils.list(new FactAclEntity().setId(entry.getId())));

    dao.retractFact(fact);
    verify(factManager, never()).saveFactAclEntry(any());
    verify(factManager).fetchFactAcl(fact.getId());
  }

  @Test
  public void testRetractFactSavesComment() {
    FactCommentRecord comment = new FactCommentRecord();
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addComment(comment);
    when(factCommentRecordConverter.toEntity(comment, fact.getId())).thenReturn(new FactCommentEntity());

    dao.retractFact(fact);
    verify(factManager).saveFactComment(notNull());
    verify(factCommentRecordConverter).toEntity(comment, fact.getId());
  }

  @Test
  public void testRetractFactFiltersDuplicateComment() {
    FactCommentRecord comment = new FactCommentRecord()
            .setId(UUID.randomUUID());
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addComment(comment);
    when(factManager.fetchFactComments(fact.getId()))
            .thenReturn(ListUtils.list(new FactCommentEntity().setId(comment.getId())));

    dao.retractFact(fact);
    verify(factManager, never()).saveFactComment(any());
    verify(factManager).fetchFactComments(fact.getId());
  }

  @Test
  public void testRetrieveExistingFactsWithoutSearchResult() {
    FactRecord record = new FactRecord();
    FactExistenceSearchCriteria criteria = createFactExistenceSearchCriteria();
    when(factRecordConverter.toCriteria(record)).thenReturn(criteria);
    when(factSearchManager.retrieveExistingFacts(criteria)).thenReturn(SearchResult.<FactDocument>builder().build());

    ResultContainer<FactRecord> container = dao.retrieveExistingFacts(record);
    assertEquals(0, container.getCount());
    assertFalse(container.hasNext());
    verify(factRecordConverter).toCriteria(record);
    verify(factSearchManager).retrieveExistingFacts(criteria);
    verifyNoInteractions(factManager);
  }

  @Test
  public void testRetrieveExistingFactsWithSearchResult() {
    UUID id = UUID.randomUUID();
    FactDocument document = new FactDocument().setId(id);
    FactEntity entity = new FactEntity().setId(id);
    FactRecord record = new FactRecord().setId(id);
    FactExistenceSearchCriteria criteria = createFactExistenceSearchCriteria();

    when(factRecordConverter.toCriteria(record)).thenReturn(criteria);
    when(factSearchManager.retrieveExistingFacts(criteria))
            .thenReturn(SearchResult.<FactDocument>builder().setCount(1).addValue(document).build());
    when(factManager.getFacts(anyList())).thenReturn(ListUtils.list(entity).iterator());
    when(factRecordConverter.fromEntity(entity)).thenReturn(record);

    ResultContainer<FactRecord> container = dao.retrieveExistingFacts(record);
    assertEquals(1, container.getCount());
    assertEquals(ListUtils.list(record), ListUtils.list(container));
    verify(factRecordConverter).toCriteria(record);
    verify(factSearchManager).retrieveExistingFacts(criteria);
    verify(factManager).getFacts(argThat(list -> list.contains(id)));
    verify(factRecordConverter).fromEntity(entity);
  }

  @Test
  public void testSearchFactsWithoutSearchResult() {
    FactSearchCriteria criteria = createFactSearchCriteria();
    when(factSearchManager.searchFacts(criteria)).thenReturn(ScrollingSearchResult.<FactDocument>builder().build());

    ResultContainer<FactRecord> container = dao.searchFacts(criteria);
    assertEquals(0, container.getCount());
    assertFalse(container.hasNext());
    verify(factSearchManager).searchFacts(criteria);
    verifyNoInteractions(factManager);
  }

  @Test
  public void testSearchFactsWithSearchResult() {
    UUID id = UUID.randomUUID();
    FactDocument document = new FactDocument().setId(id);
    FactEntity entity = new FactEntity().setId(id);
    FactRecord record = new FactRecord().setId(id);
    FactSearchCriteria criteria = createFactSearchCriteria();

    when(factSearchManager.searchFacts(criteria)).thenReturn(ScrollingSearchResult.<FactDocument>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID",
                    ListUtils.list(document).iterator(), true))
            .setCount(1)
            .build());
    when(factManager.getFacts(anyList())).thenReturn(ListUtils.list(entity).iterator());
    when(factRecordConverter.fromEntity(entity)).thenReturn(record);

    ResultContainer<FactRecord> container = dao.searchFacts(criteria);
    assertEquals(1, container.getCount());
    assertEquals(ListUtils.list(record), ListUtils.list(container));
    verify(factSearchManager).searchFacts(criteria);
    verify(factManager).getFacts(argThat(list -> list.contains(id)));
    verify(factRecordConverter).fromEntity(entity);
  }

  @Test
  public void testStoreFactAclEntryWithNull() {
    assertNull(dao.storeFactAclEntry(new FactRecord(), null));
    assertNull(dao.storeFactAclEntry(null, new FactAclEntryRecord()));
  }

  @Test
  public void testStoreFactAclEntrySavesEntity() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    FactAclEntryRecord entry = new FactAclEntryRecord().setId(UUID.randomUUID());
    when(factAclEntryRecordConverter.toEntity(entry, fact.getId())).thenReturn(new FactAclEntity());

    assertSame(entry, dao.storeFactAclEntry(fact, entry));
    verify(factManager).saveFactAclEntry(notNull());
    verify(factAclEntryRecordConverter).toEntity(argThat(r -> r.getId() == entry.getId()), eq(fact.getId()));
  }

  @Test
  public void testStoreFactAclEntrySetsId() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    FactAclEntryRecord entry = new FactAclEntryRecord();
    when(factAclEntryRecordConverter.toEntity(entry, fact.getId())).thenReturn(new FactAclEntity());

    assertSame(entry, dao.storeFactAclEntry(fact, entry));
    verify(factManager).saveFactAclEntry(notNull());
    verify(factAclEntryRecordConverter).toEntity(argThat(r -> r.getId() != null), eq(fact.getId()));
  }

  @Test
  public void testStoreFactAclEntryReindexFact() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    mockReindexingOfFact(fact);

    dao.storeFactAclEntry(fact, new FactAclEntryRecord());
    verifyReindexingOfFact(fact);
  }

  @Test
  public void testStoreFactCommentWithNull() {
    assertNull(dao.storeFactComment(new FactRecord(), null));
    assertNull(dao.storeFactComment(null, new FactCommentRecord()));
  }

  @Test
  public void testStoreFactCommentSavesEntity() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    FactCommentRecord comment = new FactCommentRecord().setId(UUID.randomUUID());
    when(factCommentRecordConverter.toEntity(comment, fact.getId())).thenReturn(new FactCommentEntity());

    assertSame(comment, dao.storeFactComment(fact, comment));
    verify(factManager).saveFactComment(notNull());
    verify(factCommentRecordConverter).toEntity(argThat(r -> r.getId() == comment.getId()), eq(fact.getId()));
  }

  @Test
  public void testStoreFactCommentSetsId() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    FactCommentRecord comment = new FactCommentRecord();
    when(factCommentRecordConverter.toEntity(comment, fact.getId())).thenReturn(new FactCommentEntity());

    assertSame(comment, dao.storeFactComment(fact, comment));
    verify(factManager).saveFactComment(notNull());
    verify(factCommentRecordConverter).toEntity(argThat(r -> r.getId() != null), eq(fact.getId()));
  }

  private void mockReindexingOfFact(FactRecord fact) {
    // Mock methods required for reindexing.
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
    when(factRecordConverter.fromEntity(notNull())).thenReturn(new FactRecord());
    when(factRecordConverter.toDocument(notNull())).thenReturn(new FactDocument());
  }

  private void verifyReindexingOfFact(FactRecord fact) {
    // Verify reindexing.
    verify(factManager).getFact(fact.getId());
    verify(factRecordConverter).fromEntity(notNull());
    verify(factRecordConverter).toDocument(notNull());
    verify(factSearchManager, atLeastOnce()).indexFact(notNull());
    verify(dcReplicationConsumer).accept(notNull());
  }

  private FactSearchCriteria createFactSearchCriteria() {
    return FactSearchCriteria.builder()
            .addAvailableOrganizationID(UUID.randomUUID())
            .setCurrentUserID(UUID.randomUUID())
            .build();
  }

  private FactExistenceSearchCriteria createFactExistenceSearchCriteria() {
    return FactExistenceSearchCriteria.builder()
            .setFactTypeID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode("Public")
            .setConfidence(0.1f)
            .setInReferenceTo(UUID.randomUUID())
            .build();
  }
}
