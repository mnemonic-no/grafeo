package no.mnemonic.services.grafeo.service.implementation.converters.response;

import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.api.model.v1.Object;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.api.result.ObjectStatisticsContainer;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.FactTypeByIdResponseResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.ObjectTypeByIdResponseResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObjectResponseConverterTest {

  @Mock
  private ObjectTypeByIdResponseResolver objectTypeConverter;
  @Mock
  private FactTypeByIdResponseResolver factTypeConverter;

  private ObjectResponseConverter converter;

  @BeforeEach
  public void setUp() {
    converter = new ObjectResponseConverter(objectTypeConverter, factTypeConverter, id -> null);
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
    when(objectTypeConverter.apply(notNull())).thenAnswer(i -> ObjectType.builder().setId(i.getArgument(0)).build());

    ObjectRecord record = createRecord();
    assertModel(record, converter.apply(record));
  }

  @Test
  public void testConvertWithStatistics() {
    when(objectTypeConverter.apply(notNull())).thenAnswer(i -> ObjectType.builder().setId(i.getArgument(0)).build());
    when(factTypeConverter.apply(notNull())).thenAnswer(i -> FactType.builder().setId(i.getArgument(0)).build());

    ObjectResponseConverter converter = new ObjectResponseConverter(objectTypeConverter, factTypeConverter,
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
