package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.seb.model.v1.ObjectTypeInfoSEB;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObjectTypeInfoDaoResolverTest {

  @Mock
  private ObjectManager objectManager;
  @InjectMocks
  private ObjectTypeInfoDaoResolver resolver;

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveNoTypeFound() {
    UUID id = UUID.randomUUID();

    ObjectTypeInfoSEB seb = resolver.apply(id);
    assertNotNull(seb);
    assertEquals(id, seb.getId());
    assertEquals("N/A", seb.getName());

    verify(objectManager).getObjectType(id);
  }

  @Test
  public void testResolveTypeFound() {
    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setName("name");
    when(objectManager.getObjectType(isA(UUID.class))).thenReturn(entity);

    ObjectTypeInfoSEB seb = resolver.apply(entity.getId());
    assertNotNull(seb);
    assertEquals(entity.getId(), seb.getId());
    assertEquals(entity.getName(), seb.getName());

    verify(objectManager).getObjectType(entity.getId());
  }
}
