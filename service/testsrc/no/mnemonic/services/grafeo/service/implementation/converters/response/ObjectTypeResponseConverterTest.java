package no.mnemonic.services.grafeo.service.implementation.converters.response;

import no.mnemonic.services.grafeo.api.model.v1.Namespace;
import no.mnemonic.services.grafeo.api.model.v1.ObjectType;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.NamespaceByIdResponseResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObjectTypeResponseConverterTest {

  @Mock
  private NamespaceByIdResponseResolver namespaceConverter;
  @InjectMocks
  private ObjectTypeResponseConverter converter;

  @Test
  public void testConvertObjectType() {
    when(namespaceConverter.apply(notNull())).thenAnswer(i -> Namespace.builder().setId(i.getArgument(0)).setName("Namespace").build());

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
