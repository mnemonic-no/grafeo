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
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectFactDaoFacadeTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private ObjectRecordConverter objectRecordConverter;

  private ObjectFactDao dao;

  @Before
  public void setUp() {
    initMocks(this);
    dao = new ObjectFactDaoFacade(objectManager, factSearchManager, objectRecordConverter);
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
    when(objectManager.saveObject(entity)).thenReturn(entity);
    when(objectRecordConverter.fromEntity(entity)).thenReturn(record);

    assertSame(record, dao.storeObject(record));
    verify(objectRecordConverter).toEntity(record);
    verify(objectManager).saveObject(argThat(e -> e.getId() == entity.getId()));
    verify(objectRecordConverter).fromEntity(entity);
  }

  @Test
  public void testStoreObjectSetsId() {
    ObjectRecord record = new ObjectRecord();
    ObjectEntity entity = new ObjectEntity();
    when(objectRecordConverter.toEntity(record)).thenReturn(entity);
    when(objectManager.saveObject(entity)).thenReturn(entity);
    when(objectRecordConverter.fromEntity(entity)).thenReturn(record);

    assertSame(record, dao.storeObject(record));
    verify(objectRecordConverter).toEntity(record);
    verify(objectManager).saveObject(argThat(e -> e.getId() != null));
    verify(objectRecordConverter).fromEntity(entity);
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
    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .addAvailableOrganizationID(UUID.randomUUID())
            .setCurrentUserID(UUID.randomUUID())
            .build();
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

    FactSearchCriteria criteria = FactSearchCriteria.builder()
            .addAvailableOrganizationID(UUID.randomUUID())
            .setCurrentUserID(UUID.randomUUID())
            .build();
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
}
