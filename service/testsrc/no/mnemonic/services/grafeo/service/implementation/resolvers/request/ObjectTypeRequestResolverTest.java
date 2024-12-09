package no.mnemonic.services.grafeo.service.implementation.resolvers.request;

import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObjectTypeRequestResolverTest {

  @Mock
  private ObjectManager objectManager;
  @InjectMocks
  private ObjectTypeRequestResolver resolver;

  @Test
  public void testFetchExistingObjectType() throws ObjectNotFoundException {
    UUID id = UUID.randomUUID();
    ObjectTypeEntity entity = new ObjectTypeEntity();
    when(objectManager.getObjectType(id)).thenReturn(entity);
    assertSame(entity, resolver.fetchExistingObjectType(id));
  }

  @Test
  public void testFetchExistingObjectTypeFailsWhenMissing(){
    ObjectNotFoundException ex = assertThrows(ObjectNotFoundException.class, () -> resolver.fetchExistingObjectType(UUID.randomUUID()));
    assertEquals("object.type.not.exist", ex.getMessageTemplate());
  }
}
