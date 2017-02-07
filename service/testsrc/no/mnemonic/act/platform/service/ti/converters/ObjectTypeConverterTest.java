package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Namespace;
import no.mnemonic.act.platform.api.model.v1.ObjectType;
import no.mnemonic.act.platform.entity.cassandra.ObjectTypeEntity;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;

public class ObjectTypeConverterTest {

  @Test
  public void testConvertObjectType() {
    ObjectTypeEntity entity = createEntity();
    assertModel(entity, createObjectTypeConverter().apply(entity));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(createObjectTypeConverter().apply(null));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutNamespaceConverterThrowsException() {
    ObjectTypeConverter.builder().build();
  }

  private ObjectTypeConverter createObjectTypeConverter() {
    Function<UUID, Namespace> namespaceConverter = id -> Namespace.builder().setId(id).setName("Namespace").build();
    return ObjectTypeConverter.builder().setNamespaceConverter(namespaceConverter).build();
  }

  private ObjectTypeEntity createEntity() {
    return new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setNamespaceID(UUID.randomUUID())
            .setName("ObjectType")
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter")
            .setEntityHandler("EntityHandler")
            .setEntityHandlerParameter("EntityHandlerParameter");
  }

  private void assertModel(ObjectTypeEntity entity, ObjectType model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getName(), model.getName());
    assertEquals(entity.getValidator(), model.getValidator());
    assertEquals(entity.getValidatorParameter(), model.getValidatorParameter());
    assertEquals(entity.getEntityHandler(), model.getEntityHandler());
    assertEquals(entity.getEntityHandlerParameter(), model.getEntityHandlerParameter());
    assertNotNull(model.getNamespace());
    assertEquals(entity.getNamespaceID(), model.getNamespace().getId());
    assertEquals("Namespace", model.getNamespace().getName());
  }

}
