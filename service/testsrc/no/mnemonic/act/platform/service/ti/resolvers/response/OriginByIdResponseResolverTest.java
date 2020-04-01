package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.converters.response.OriginResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.OriginResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginByIdResponseResolverTest {

  @Mock
  private OriginResolver originResolver;
  @Mock
  private OriginResponseConverter originResponseConverter;

  private OriginByIdResponseResolver converter;

  @Before
  public void setup() {
    initMocks(this);
    converter = new OriginByIdResponseResolver(originResolver, originResponseConverter);
  }

  @Test
  public void testConvertOrigin() {
    UUID id = UUID.randomUUID();
    OriginEntity entity = new OriginEntity();
    Origin model = Origin.builder().build();

    when(originResolver.apply(id)).thenReturn(entity);
    when(originResponseConverter.apply(entity)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(originResolver).apply(id);
    verify(originResponseConverter).apply(entity);
  }

  @Test
  public void testConvertOriginNotAvailable() {
    UUID id = UUID.randomUUID();
    Origin model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(originResolver).apply(id);
    verifyZeroInteractions(originResponseConverter);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
