package no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.ObjectManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.services.grafeo.service.implementation.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObjectFactTypeResolverTest {

  @Mock
  private FactManager factManager;
  @Mock
  private ObjectManager objectManager;
  @InjectMocks
  private ObjectFactTypeResolver resolver;

  @Test
  public void testGetFactTypeIdBasedOnName() {
    UUID uuid = UUID.randomUUID();
    when(factManager.getFactType("test")).thenReturn(new FactTypeEntity().setId(uuid));
    assertEquals(uuid, resolver.factTypeNameToId("test"));
  }

  @Test
  public void testGetNonExistingFactType() {
    assertNull(resolver.factTypeNameToId("does not exist"));
  }

  @Test
  public void testGetFactTypeIdsBasedOnNames() {
    UUID uuid1 = UUID.randomUUID();
    UUID uuid2 = UUID.randomUUID();

    when(factManager.getFactType("test1")).thenReturn(new FactTypeEntity().setId(uuid1));
    when(factManager.getFactType("test2")).thenReturn(new FactTypeEntity().setId(uuid2));

    assertEquals(SetUtils.set(uuid1, uuid2), resolver.factTypeNamesToIds(SetUtils.set("test1", "test2")));
  }

  @Test
  public void testGetNonExistingFactTypeIdsBasedOnNames() {
    assertEquals(new HashSet<>(), resolver.factTypeNamesToIds(new HashSet<>()));
    assertEquals(new HashSet<>(), resolver.factTypeNamesToIds(null));
  }

  @Test
  public void testToFactTypeStructNotExists() {
    assertNull(resolver.toFactTypeStruct(null));
  }

  @Test
  public void testToFactTypeStruct() {
    UUID id = UUID.randomUUID();
    when(factManager.getFactType(id)).thenReturn(new FactTypeEntity().setId(id).setName("someFactType"));
    assertEquals(FactTypeStruct.builder().setId(id).setName("someFactType").build(), resolver.toFactTypeStruct(id));
  }

  @Test
  public void testToObjectTypeStructNotExists() {
    assertNull(resolver.toObjectTypeStruct(null));
    assertNull(resolver.toObjectTypeStruct(UUID.randomUUID()));
  }

  @Test
  public void testToObjectTypeStruct() {
    UUID id = UUID.randomUUID();
    when(objectManager.getObjectType(id)).thenReturn(new ObjectTypeEntity().setId(id).setName("someObjectType"));
    assertEquals(ObjectTypeStruct.builder().setId(id).setName("someObjectType").build(), resolver.toObjectTypeStruct(id));
  }
}
