package no.mnemonic.act.platform.dao.facade;

import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.criteria.AccessControlCriteria;
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
import no.mnemonic.act.platform.dao.facade.helpers.FactRecordHasher;
import no.mnemonic.act.platform.dao.facade.resolvers.CachedFactResolver;
import no.mnemonic.act.platform.dao.facade.resolvers.CachedObjectResolver;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectFactDaoFacadeTest {

  private final AccessControlCriteria accessControlCriteria = AccessControlCriteria.builder()
          .addCurrentUserIdentity(UUID.randomUUID())
          .addAvailableOrganizationID(UUID.randomUUID())
          .build();

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
  private CachedObjectResolver objectResolver;
  @Mock
  private CachedFactResolver factResolver;
  @Mock
  private Consumer<FactRecord> dcReplicationConsumer;
  @Mock
  private Clock clock;

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
            objectResolver,
            factResolver,
            dcReplicationConsumer
    ).withClock(clock);
  }

  @Test
  public void testGetObjectById() {
    UUID id = UUID.randomUUID();
    ObjectRecord record = new ObjectRecord();
    when(objectResolver.getObject(id)).thenReturn(record);

    assertSame(record, dao.getObject(id));
    verify(objectResolver).getObject(id);
  }

  @Test
  public void testGetObjectByTypeValue() {
    String type = "type";
    String value = "value";
    ObjectRecord record = new ObjectRecord();
    when(objectResolver.getObject(type, value)).thenReturn(record);

    assertSame(record, dao.getObject(type, value));
    verify(objectResolver).getObject(type, value);
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
            .setAccessControlCriteria(accessControlCriteria)
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
    ObjectRecord record = new ObjectRecord().setId(id);

    FactSearchCriteria criteria = createFactSearchCriteria();
    when(factSearchManager.searchObjects(criteria))
            .thenReturn(SearchResult.<ObjectDocument>builder().setCount(1).addValue(document).build());
    when(objectResolver.getObject(id)).thenReturn(record);

    ResultContainer<ObjectRecord> container = dao.searchObjects(criteria);
    assertEquals(1, container.getCount());
    assertEquals(ListUtils.list(record), ListUtils.list(container));
    verify(factSearchManager).searchObjects(criteria);
    verify(objectResolver).getObject(id);
  }

  @Test
  public void testGetFactById() {
    UUID id = UUID.randomUUID();
    FactRecord record = new FactRecord();
    when(factResolver.getFact(id)).thenReturn(record);

    assertSame(record, dao.getFact(id));
    verify(factResolver).getFact(id);
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
  public void testStoreFactSavesFactExistence() {
    FactRecord record = new FactRecord().setId(UUID.randomUUID());
    when(factRecordConverter.toEntity(notNull())).thenReturn(new FactEntity());

    dao.storeFact(record);
    verify(factManager).saveFactExistence(argThat(factExistence -> {
      assertEquals(FactRecordHasher.toHash(record), factExistence.getFactHash());
      assertEquals(record.getId(), factExistence.getFactID());
      return true;
    }));
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
    long timestamp = 123456789;
    FactRecord record = new FactRecord().setId(UUID.randomUUID());
    when(clock.instant()).thenReturn(Instant.ofEpochMilli(timestamp));
    when(factManager.getFact(record.getId())).thenReturn(new FactEntity());
    mockReindexingOfFact(record);

    assertNotNull(dao.refreshFact(record));
    verify(factManager).saveFact(argThat(entity -> entity.getLastSeenTimestamp() == timestamp));
    verifyReindexingOfFact(record);
  }

  @Test
  public void testRefreshFactSavesAclEntry() {
    FactAclEntryRecord entry = new FactAclEntryRecord();
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addAclEntry(entry);
    when(clock.instant()).thenReturn(Instant.ofEpochMilli(123456789));
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
    when(factAclEntryRecordConverter.toEntity(entry, fact.getId())).thenReturn(new FactAclEntity());

    dao.refreshFact(fact);
    verify(factManager).saveFact(argThat(entity -> entity.isSet(FactEntity.Flag.HasAcl)));
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
    when(clock.instant()).thenReturn(Instant.ofEpochMilli(123456789));
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
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
    when(clock.instant()).thenReturn(Instant.ofEpochMilli(123456789));
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
    when(factCommentRecordConverter.toEntity(comment, fact.getId())).thenReturn(new FactCommentEntity());

    dao.refreshFact(fact);
    verify(factManager).saveFact(argThat(entity -> entity.isSet(FactEntity.Flag.HasComments)));
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
    when(clock.instant()).thenReturn(Instant.ofEpochMilli(123456789));
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
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
    when(factManager.getFact(record.getId())).thenReturn(new FactEntity());
    mockReindexingOfFact(record);

    assertNotNull(dao.retractFact(record));
    verify(factManager).saveFact(argThat(entity -> entity.isSet(FactEntity.Flag.RetractedHint)));
    verifyReindexingOfFact(record);
  }

  @Test
  public void testRetractFactSavesAclEntry() {
    FactAclEntryRecord entry = new FactAclEntryRecord();
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .addAclEntry(entry);
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
    when(factAclEntryRecordConverter.toEntity(entry, fact.getId())).thenReturn(new FactAclEntity());

    dao.retractFact(fact);
    verify(factManager).saveFact(argThat(entity -> entity.isSet(FactEntity.Flag.HasAcl)));
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
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
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
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
    when(factCommentRecordConverter.toEntity(comment, fact.getId())).thenReturn(new FactCommentEntity());

    dao.retractFact(fact);
    verify(factManager).saveFact(argThat(entity -> entity.isSet(FactEntity.Flag.HasComments)));
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
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
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
    FactRecord record = new FactRecord().setId(id);
    FactExistenceSearchCriteria criteria = createFactExistenceSearchCriteria();

    when(factRecordConverter.toCriteria(record)).thenReturn(criteria);
    when(factSearchManager.retrieveExistingFacts(criteria))
            .thenReturn(SearchResult.<FactDocument>builder().setCount(1).addValue(document).build());
    when(factResolver.getFact(id)).thenReturn(record);

    ResultContainer<FactRecord> container = dao.retrieveExistingFacts(record);
    assertEquals(1, container.getCount());
    assertEquals(ListUtils.list(record), ListUtils.list(container));
    verify(factRecordConverter).toCriteria(record);
    verify(factSearchManager).retrieveExistingFacts(criteria);
    verify(factResolver).getFact(id);
  }

  @Test
  public void testRetrieveExistingFactWithNull() {
    assertFalse(dao.retrieveExistingFact(null).isPresent());
    verifyNoInteractions(factResolver);
  }

  @Test
  public void testRetrieveExistingFactNotFound() {
    FactRecord record = new FactRecord();
    String hash = FactRecordHasher.toHash(record);

    assertFalse(dao.retrieveExistingFact(record).isPresent());
    verify(factResolver).getFact(hash);
  }

  @Test
  public void testRetrieveExistingFactFound() {
    FactRecord record = new FactRecord();
    String hash = FactRecordHasher.toHash(record);
    when(factResolver.getFact(hash)).thenReturn(record);

    Optional<FactRecord> result = dao.retrieveExistingFact(record);
    assertTrue(result.isPresent());
    assertSame(record, result.get());
    verify(factResolver).getFact(hash);
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
    FactRecord record = new FactRecord().setId(id);
    FactSearchCriteria criteria = createFactSearchCriteria();

    when(factSearchManager.searchFacts(criteria)).thenReturn(ScrollingSearchResult.<FactDocument>builder()
            .setInitialBatch(new ScrollingSearchResult.ScrollingBatch<>("TEST_SCROLL_ID",
                    ListUtils.list(document).iterator(), true))
            .setCount(1)
            .build());
    when(factResolver.getFact(id)).thenReturn(record);

    ResultContainer<FactRecord> container = dao.searchFacts(criteria);
    assertEquals(1, container.getCount());
    assertEquals(ListUtils.list(record), ListUtils.list(container));
    verify(factSearchManager).searchFacts(criteria);
    verify(factResolver).getFact(id);
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
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
    when(factAclEntryRecordConverter.toEntity(entry, fact.getId())).thenReturn(new FactAclEntity());

    assertSame(entry, dao.storeFactAclEntry(fact, entry));
    verify(factManager).saveFact(argThat(entity -> entity.isSet(FactEntity.Flag.HasAcl)));
    verify(factManager).saveFactAclEntry(notNull());
    verify(factAclEntryRecordConverter).toEntity(argThat(r -> r.getId() == entry.getId()), eq(fact.getId()));
  }

  @Test
  public void testStoreFactAclEntrySetsId() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    FactAclEntryRecord entry = new FactAclEntryRecord();
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
    when(factAclEntryRecordConverter.toEntity(entry, fact.getId())).thenReturn(new FactAclEntity());

    assertSame(entry, dao.storeFactAclEntry(fact, entry));
    verify(factManager).saveFactAclEntry(notNull());
    verify(factAclEntryRecordConverter).toEntity(argThat(r -> r.getId() != null), eq(fact.getId()));
  }

  @Test
  public void testStoreFactAclEntryReindexFact() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
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
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
    when(factCommentRecordConverter.toEntity(comment, fact.getId())).thenReturn(new FactCommentEntity());

    assertSame(comment, dao.storeFactComment(fact, comment));
    verify(factManager).saveFact(argThat(entity -> entity.isSet(FactEntity.Flag.HasComments)));
    verify(factManager).saveFactComment(notNull());
    verify(factCommentRecordConverter).toEntity(argThat(r -> r.getId() == comment.getId()), eq(fact.getId()));
  }

  @Test
  public void testStoreFactCommentSetsId() {
    FactRecord fact = new FactRecord().setId(UUID.randomUUID());
    FactCommentRecord comment = new FactCommentRecord();
    when(factManager.getFact(fact.getId())).thenReturn(new FactEntity());
    when(factCommentRecordConverter.toEntity(comment, fact.getId())).thenReturn(new FactCommentEntity());

    assertSame(comment, dao.storeFactComment(fact, comment));
    verify(factManager).saveFactComment(notNull());
    verify(factCommentRecordConverter).toEntity(argThat(r -> r.getId() != null), eq(fact.getId()));
  }

  private void mockReindexingOfFact(FactRecord fact) {
    // Mock methods required for reindexing.
    when(factResolver.getFact(fact.getId())).thenReturn(new FactRecord());
    when(factRecordConverter.toDocument(notNull())).thenReturn(new FactDocument());
  }

  private void verifyReindexingOfFact(FactRecord fact) {
    // Verify reindexing.
    verify(factResolver).evict(fact);
    verify(factResolver).getFact(fact.getId());
    verify(factRecordConverter).toDocument(notNull());
    verify(factSearchManager, atLeastOnce()).indexFact(notNull());
    verify(dcReplicationConsumer).accept(notNull());
  }

  private FactSearchCriteria createFactSearchCriteria() {
    return FactSearchCriteria.builder()
            .setAccessControlCriteria(accessControlCriteria)
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
