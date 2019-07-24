package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectByIdConverterTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private Function<ObjectEntity, Object> objectConverter;

  private ObjectByIdConverter converter;

  @Before
  public void setup() {
    initMocks(this);
    converter = new ObjectByIdConverter(objectManager, objectConverter);
  }

  @Test
  public void testConvertObject() {
    UUID id = UUID.randomUUID();
    ObjectEntity entity = new ObjectEntity();
    Object model = Object.builder().build();

    when(objectManager.getObject(id)).thenReturn(entity);
    when(objectConverter.apply(entity)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(objectManager).getObject(id);
    verify(objectConverter).apply(entity);
  }

  @Test
  public void testConvertObjectNotAvailable() {
    UUID id = UUID.randomUUID();
    Object model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getValue());

    verify(objectManager).getObject(id);
    verifyZeroInteractions(objectConverter);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
