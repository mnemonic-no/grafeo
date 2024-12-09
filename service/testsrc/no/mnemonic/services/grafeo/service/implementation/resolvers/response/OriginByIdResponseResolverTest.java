package no.mnemonic.services.grafeo.service.implementation.resolvers.response;

import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.converters.response.OriginResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.OriginResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OriginByIdResponseResolverTest {

  @Mock
  private OriginResolver originResolver;
  @Mock
  private OriginResponseConverter originResponseConverter;

  private Map<UUID, Origin> responseCache;
  private OriginByIdResponseResolver converter;

  @BeforeEach
  public void setup() {
    responseCache = new HashMap<>();
    converter = new OriginByIdResponseResolver(originResolver, originResponseConverter, responseCache);
  }

  @Test
  public void testConvertCachedOrigin() {
    UUID id = UUID.randomUUID();
    Origin model = Origin.builder().build();
    responseCache.put(id, model);

    assertSame(model, converter.apply(id));
    verifyNoInteractions(originResolver);
    verifyNoInteractions(originResponseConverter);
  }

  @Test
  public void testConvertUncachedOrigin() {
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
  public void testConvertUncachedOriginNotAvailable() {
    UUID id = UUID.randomUUID();
    Origin model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(originResolver).apply(id);
    verifyNoInteractions(originResponseConverter);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
