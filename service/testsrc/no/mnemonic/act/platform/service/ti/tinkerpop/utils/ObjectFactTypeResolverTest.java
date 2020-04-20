package no.mnemonic.act.platform.service.ti.tinkerpop.utils;

import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.FactTypeStruct;
import no.mnemonic.act.platform.service.ti.tinkerpop.utils.ObjectFactTypeResolver.ObjectTypeStruct;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ObjectFactTypeResolverTest {

  @Mock
  private FactManager factManager;
  @Mock
  private ObjectManager objectManager;

  private ObjectFactTypeResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new ObjectFactTypeResolver(factManager, objectManager);
  }

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
