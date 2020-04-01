package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.converters.response.FactTypeResponseConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactTypeByIdResponseResolverTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactTypeResponseConverter factTypeResponseConverter;

  private FactTypeByIdResponseResolver converter;

  @Before
  public void setup() {
    initMocks(this);
    converter = new FactTypeByIdResponseResolver(factManager, factTypeResponseConverter);
  }

  @Test
  public void testConvertFactType() {
    UUID id = UUID.randomUUID();
    FactTypeEntity entity = new FactTypeEntity();
    FactType model = FactType.builder().build();

    when(factManager.getFactType(id)).thenReturn(entity);
    when(factTypeResponseConverter.apply(entity)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(factManager).getFactType(id);
    verify(factTypeResponseConverter).apply(entity);
  }

  @Test
  public void testConvertFactTypeNotAvailable() {
    UUID id = UUID.randomUUID();
    FactType model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(factManager).getFactType(id);
    verifyZeroInteractions(factTypeResponseConverter);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
