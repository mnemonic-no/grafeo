package no.mnemonic.act.platform.service.ti.resolvers;

import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectTypeResolverTest {

  @Mock
  private ObjectManager objectManager;
  private ObjectTypeResolver resolver;

  @Before
  public void init() {
    initMocks(this);
    resolver = new ObjectTypeResolver(objectManager);
  }

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
