package no.mnemonic.services.grafeo.service.implementation.resolvers.response;

import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.converters.response.ObjectTypeResponseConverter;
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
public class ObjectTypeByIdResponseResolverTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private ObjectTypeResponseConverter objectTypeResponseConverter;

  private Map<UUID, ObjectType> responseCache;
  private ObjectTypeByIdResponseResolver converter;

  @BeforeEach
  public void setup() {
    responseCache = new HashMap<>();
    converter = new ObjectTypeByIdResponseResolver(objectManager, objectTypeResponseConverter, responseCache);
  }

  @Test
  public void testConvertCachedObjectType() {
    UUID id = UUID.randomUUID();
    ObjectType model = ObjectType.builder().build();
    responseCache.put(id, model);

    assertSame(model, converter.apply(id));
    verifyNoInteractions(objectManager);
    verifyNoInteractions(objectTypeResponseConverter);
  }

  @Test
  public void testConvertUncachedObjectType() {
    UUID id = UUID.randomUUID();
    ObjectTypeEntity entity = new ObjectTypeEntity();
    ObjectType model = ObjectType.builder().build();

    when(objectManager.getObjectType(id)).thenReturn(entity);
    when(objectTypeResponseConverter.apply(entity)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(objectManager).getObjectType(id);
    verify(objectTypeResponseConverter).apply(entity);
  }

  @Test
  public void testConvertUncachedObjectTypeNotAvailable() {
    UUID id = UUID.randomUUID();
    ObjectType model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(objectManager).getObjectType(id);
    verifyNoInteractions(objectTypeResponseConverter);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
