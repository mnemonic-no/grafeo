package no.mnemonic.services.grafeo.service.implementation.resolvers.response;

import no.mnemonic.services.grafeo.api.model.v1.FactType;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactTypeResponseConverter;
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
public class FactTypeByIdResponseResolverTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactTypeResponseConverter factTypeResponseConverter;

  private Map<UUID, FactType> responseCache;
  private FactTypeByIdResponseResolver converter;

  @BeforeEach
  public void setup() {
    responseCache = new HashMap<>();
    converter = new FactTypeByIdResponseResolver(factManager, factTypeResponseConverter, responseCache);
  }

  @Test
  public void testConvertCachedFactType() {
    UUID id = UUID.randomUUID();
    FactType model = FactType.builder().build();
    responseCache.put(id, model);

    assertSame(model, converter.apply(id));
    verifyNoInteractions(factManager);
    verifyNoInteractions(factTypeResponseConverter);
  }

  @Test
  public void testConvertUncachedFactType() {
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
  public void testConvertUncachedFactTypeNotAvailable() {
    UUID id = UUID.randomUUID();
    FactType model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(factManager).getFactType(id);
    verifyNoInteractions(factTypeResponseConverter);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
