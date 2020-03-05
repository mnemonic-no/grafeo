package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactTypeByIdConverterTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactTypeConverter factTypeConverter;

  private FactTypeByIdConverter converter;

  @Before
  public void setup() {
    initMocks(this);
    converter = new FactTypeByIdConverter(factManager, factTypeConverter);
  }

  @Test
  public void testConvertFactType() {
    UUID id = UUID.randomUUID();
    FactTypeEntity entity = new FactTypeEntity();
    FactType model = FactType.builder().build();

    when(factManager.getFactType(id)).thenReturn(entity);
    when(factTypeConverter.apply(entity)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(factManager).getFactType(id);
    verify(factTypeConverter).apply(entity);
  }

  @Test
  public void testConvertFactTypeNotAvailable() {
    UUID id = UUID.randomUUID();
    FactType model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(factManager).getFactType(id);
    verifyZeroInteractions(factTypeConverter);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
