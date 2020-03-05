package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectTypeByIdConverterTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private ObjectTypeConverter objectTypeConverter;

  private ObjectTypeByIdConverter converter;

  @Before
  public void setup() {
    initMocks(this);
    converter = new ObjectTypeByIdConverter(objectManager, objectTypeConverter);
  }

  @Test
  public void testConvertObjectType() {
    UUID id = UUID.randomUUID();
    ObjectTypeEntity entity = new ObjectTypeEntity();
    ObjectType model = ObjectType.builder().build();

    when(objectManager.getObjectType(id)).thenReturn(entity);
    when(objectTypeConverter.apply(entity)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(objectManager).getObjectType(id);
    verify(objectTypeConverter).apply(entity);
  }

  @Test
  public void testConvertObjectTypeNotAvailable() {
    UUID id = UUID.randomUUID();
    ObjectType model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(objectManager).getObjectType(id);
    verifyZeroInteractions(objectTypeConverter);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
