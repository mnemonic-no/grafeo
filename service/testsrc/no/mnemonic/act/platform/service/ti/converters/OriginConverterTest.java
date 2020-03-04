package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Namespace;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OriginConverterTest {

  private OriginConverter converter;

  @Before
  public void setUp() {
    NamespaceByIdConverter namespaceConverter = mock(NamespaceByIdConverter.class);
    when(namespaceConverter.apply(notNull())).thenAnswer(i -> Namespace.builder().setId(i.getArgument(0)).build());

    Function<UUID, Organization> organizationConverter = id -> Organization.builder().setId(id).build();
    converter = new OriginConverter(namespaceConverter, organizationConverter);
  }

  @Test
  public void testConvertOrigin() {
    OriginEntity entity = createEntity();
    assertModel(entity, converter.apply(entity));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }

  private OriginEntity createEntity() {
    return new OriginEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setName("name")
            .setDescription("description")
            .setTrust(0.1f)
            .setType(OriginEntity.Type.Group)
            .addFlag(OriginEntity.Flag.Deleted);
  }

  private void assertModel(OriginEntity entity, Origin model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getNamespaceID(), model.getNamespace().getId());
    assertEquals(entity.getOrganizationID(), model.getOrganization().getId());
    assertEquals(entity.getName(), model.getName());
    assertEquals(entity.getDescription(), model.getDescription());
    assertEquals(entity.getTrust(), model.getTrust(), 0.0);
    assertEquals(entity.getType().name(), model.getType().name());
    assertEquals(SetUtils.set(entity.getFlags(), Enum::name), SetUtils.set(model.getFlags(), Enum::name));
  }
}
