package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ObjectStatisticsContainer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectRecordConverterTest {

  @Mock
  private ObjectTypeByIdConverter objectTypeConverter;
  @Mock
  private FactTypeByIdConverter factTypeConverter;

  private ObjectRecordConverter converter;

  @Before
  public void setUp() {
    initMocks(this);

    when(objectTypeConverter.apply(notNull())).thenAnswer(i -> ObjectType.builder().setId(i.getArgument(0)).build());
    when(factTypeConverter.apply(notNull())).thenAnswer(i -> FactType.builder().setId(i.getArgument(0)).build());

    converter = new ObjectRecordConverter(objectTypeConverter, factTypeConverter, id -> null);
  }

  @Test
  public void testConvertNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmpty() {
    assertNotNull(converter.apply(new ObjectRecord()));
  }

  @Test
  public void testConvertWithoutStatistics() {
    ObjectRecord record = createRecord();
    assertModel(record, converter.apply(record));
  }

  @Test
  public void testConvertWithStatistics() {
    ObjectRecordConverter converter = new ObjectRecordConverter(objectTypeConverter, factTypeConverter,
            id -> Collections.singleton(new ObjectStatisticsContainer.FactStatistic(UUID.randomUUID(), 42, 123456789, 987654321)));

    ObjectRecord record = createRecord();
    Object model = converter.apply(record);
    assertModel(record, model);
    assertEquals(1, model.getStatistics().size());
    assertNotNull(model.getStatistics().get(0).getType());
    assertEquals(42, model.getStatistics().get(0).getCount());
    assertEquals(123456789, (long) model.getStatistics().get(0).getLastAddedTimestamp());
    assertEquals(987654321, (long) model.getStatistics().get(0).getLastSeenTimestamp());
  }

  private ObjectRecord createRecord() {
    return new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(UUID.randomUUID())
            .setValue("value");
  }

  private void assertModel(ObjectRecord record, Object model) {
    assertEquals(record.getId(), model.getId());
    assertEquals(record.getTypeID(), model.getType().getId());
    assertEquals(record.getValue(), model.getValue());
  }
}
