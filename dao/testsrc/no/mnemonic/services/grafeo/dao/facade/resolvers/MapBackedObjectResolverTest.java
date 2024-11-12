package no.mnemonic.services.grafeo.dao.facade.resolvers;

import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.services.grafeo.dao.facade.converters.ObjectRecordConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MapBackedObjectResolverTest {

  @Mock
  private ObjectManager objectManager;
  @Mock
  private ObjectRecordConverter objectRecordConverter;

  private CachedObjectResolver objectResolver;

  @BeforeEach
  public void setUp() {
    objectResolver = new MapBackedObjectResolver(objectManager, objectRecordConverter, new HashMap<>(), new HashMap<>());
  }

  @Test
  public void testGetObjectByIdInvalidInput() {
    assertNull(objectResolver.getObject(null));
    verifyNoInteractions(objectManager);
  }

  @Test
  public void testGetObjectByIdNotFound() {
    UUID id = UUID.randomUUID();
    assertNull(objectResolver.getObject(id));
    verify(objectManager).getObject(id);
  }

  @Test
  public void testGetObjectByIdFound() {
    UUID id = UUID.randomUUID();
    when(objectManager.getObject(any())).thenReturn(new ObjectEntity());
    when(objectRecordConverter.fromEntity(any())).thenReturn(new ObjectRecord());

    assertNotNull(objectResolver.getObject(id));
    verify(objectManager).getObject(id);
    verify(objectRecordConverter).fromEntity(notNull());
  }

  @Test
  public void testGetObjectByIdFoundCached() {
    UUID id = UUID.randomUUID();
    when(objectManager.getObject(any())).thenReturn(new ObjectEntity());
    when(objectRecordConverter.fromEntity(any())).then(i -> new ObjectRecord());

    assertSame(objectResolver.getObject(id), objectResolver.getObject(id));
    verify(objectManager).getObject(id);
    verify(objectRecordConverter).fromEntity(notNull());
  }

  @Test
  public void testGetObjectByTypeValueInvalidInput() {
    assertNull(objectResolver.getObject(null, null));
    assertNull(objectResolver.getObject("type", null));
    assertNull(objectResolver.getObject(null, "value"));
    verifyNoInteractions(objectManager);
  }

  @Test
  public void testGetObjectByTypeValueNotFound() {
    String type = "type";
    String value = "value";
    assertNull(objectResolver.getObject(type, value));
    verify(objectManager).getObject(type, value);
  }

  @Test
  public void testGetObjectByTypeValueFound() {
    String type = "type";
    String value = "value";
    when(objectManager.getObject(any(), any())).thenReturn(new ObjectEntity());
    when(objectRecordConverter.fromEntity(any())).thenReturn(new ObjectRecord());

    assertNotNull(objectResolver.getObject(type, value));
    verify(objectManager).getObject(type, value);
    verify(objectRecordConverter).fromEntity(notNull());
  }

  @Test
  public void testGetObjectByTypeValueFoundCached() {
    String type = "type";
    String value = "value";
    when(objectManager.getObject(any(), any())).thenReturn(new ObjectEntity());
    when(objectRecordConverter.fromEntity(any())).then(i -> new ObjectRecord());

    assertSame(objectResolver.getObject(type, value), objectResolver.getObject(type, value));
    verify(objectManager).getObject(type, value);
    verify(objectRecordConverter).fromEntity(notNull());
  }
}
