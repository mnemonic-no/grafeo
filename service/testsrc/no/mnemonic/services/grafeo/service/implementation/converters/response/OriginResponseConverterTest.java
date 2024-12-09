package no.mnemonic.services.grafeo.service.implementation.converters.response;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.model.v1.Namespace;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.NamespaceByIdResponseResolver;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.OrganizationByIdResponseResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OriginResponseConverterTest {

  @Mock
  private NamespaceByIdResponseResolver namespaceConverter;
  @Mock
  private OrganizationByIdResponseResolver organizationConverter;
  @InjectMocks
  private OriginResponseConverter converter;

  @Test
  public void testConvertOrigin() {
    when(namespaceConverter.apply(notNull())).thenAnswer(i -> Namespace.builder().setId(i.getArgument(0)).build());
    when(organizationConverter.apply(notNull())).thenAnswer(i -> Organization.builder().setId(i.getArgument(0)).build());

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
