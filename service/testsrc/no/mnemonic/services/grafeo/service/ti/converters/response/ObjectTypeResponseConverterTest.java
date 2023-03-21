package no.mnemonic.services.grafeo.service.ti.converters.response;

import no.mnemonic.services.grafeo.api.model.v1.Namespace;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.NamespaceByIdResponseResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ObjectTypeResponseConverterTest {

  private ObjectTypeResponseConverter converter;

  @Before
  public void setUp() {
    NamespaceByIdResponseResolver namespaceConverter = mock(NamespaceByIdResponseResolver.class);
    when(namespaceConverter.apply(notNull())).thenAnswer(i -> Namespace.builder().setId(i.getArgument(0)).setName("Namespace").build());
    converter = new ObjectTypeResponseConverter(namespaceConverter);
  }

  @Test
  public void testConvertObjectType() {
    ObjectTypeEntity entity = createEntity();
    assertModel(entity, converter.apply(entity));
  }

  @Test
  public void testConvertEmptyReturnsDefaultValues() {
    ObjectType model = converter.apply(new ObjectTypeEntity());
    assertEquals(ObjectType.IndexOption.Daily, model.getIndexOption());
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }

  private ObjectTypeEntity createEntity() {
    return new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName("ObjectType")
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter")
            .addFlag(ObjectTypeEntity.Flag.TimeGlobalIndex);
  }

  private void assertModel(ObjectTypeEntity entity, ObjectType model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getName(), model.getName());
    assertEquals(entity.getValidator(), model.getValidator());
    assertEquals(entity.getValidatorParameter(), model.getValidatorParameter());
    assertNotNull(model.getNamespace());
    assertEquals(entity.getNamespaceID(), model.getNamespace().getId());
    assertEquals("Namespace", model.getNamespace().getName());
    assertEquals(ObjectType.IndexOption.TimeGlobal, model.getIndexOption());
  }
}
