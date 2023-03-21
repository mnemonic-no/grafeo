package no.mnemonic.services.grafeo.service.ti.converters.response;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.model.v1.Namespace;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.NamespaceByIdResponseResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.OrganizationByIdResponseResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OriginResponseConverterTest {

  private OriginResponseConverter converter;

  @Before
  public void setUp() {
    NamespaceByIdResponseResolver namespaceConverter = mock(NamespaceByIdResponseResolver.class);
    when(namespaceConverter.apply(notNull())).thenAnswer(i -> Namespace.builder().setId(i.getArgument(0)).build());

    OrganizationByIdResponseResolver organizationConverter = mock(OrganizationByIdResponseResolver.class);
    when(organizationConverter.apply(notNull())).thenAnswer(i -> Organization.builder().setId(i.getArgument(0)).build());

    converter = new OriginResponseConverter(namespaceConverter, organizationConverter);
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
