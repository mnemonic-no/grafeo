package no.mnemonic.act.platform.service.ti.converters.response;

import no.mnemonic.act.platform.api.model.v1.Namespace;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.service.ti.resolvers.response.NamespaceByIdResponseResolver;
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
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }

  private ObjectTypeEntity createEntity() {
    return new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName("ObjectType")
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter");
  }

  private void assertModel(ObjectTypeEntity entity, ObjectType model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getName(), model.getName());
    assertEquals(entity.getValidator(), model.getValidator());
    assertEquals(entity.getValidatorParameter(), model.getValidatorParameter());
    assertNotNull(model.getNamespace());
    assertEquals(entity.getNamespaceID(), model.getNamespace().getId());
    assertEquals("Namespace", model.getNamespace().getName());
  }
}
