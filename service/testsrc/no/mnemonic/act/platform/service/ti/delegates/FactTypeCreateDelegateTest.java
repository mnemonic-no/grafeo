package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.request.v1.CreateFactTypeRequest;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.api.request.v1.MetaFactBindingDefinition;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeHelper;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FactTypeCreateDelegateTest extends AbstractDelegateTest {

  @Mock
  private FactTypeHelper factTypeHelper;

  private FactTypeCreateDelegate delegate;

  @Before
  public void setup() {
    // initMocks() will be called by base class.
    when(factTypeHelper.convertFactObjectBindingDefinitions(anyList())).thenReturn(SetUtils.set(new FactTypeEntity.FactObjectBindingDefinition()));
    when(factTypeHelper.convertMetaFactBindingDefinitions(anyList())).thenReturn(SetUtils.set(new FactTypeEntity.MetaFactBindingDefinition()));
    delegate = new FactTypeCreateDelegate(getSecurityContext(), getFactManager(), factTypeHelper, getFactTypeConverter());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCreateFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(getSecurityContext()).checkPermission(TiFunctionConstants.addTypes);
    delegate.handle(createFactTypeRequest());
  }

  @Test
  public void testCreateFactTypeWithoutBindings() {
    CreateFactTypeRequest request = new CreateFactTypeRequest()
            .setName("FactType")
            .setValidator("Validator");
    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(SetUtils.set("invalid.fact.type.definition"), SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testCreateFactTypeWithBothObjectAndFactBindings() {
    CreateFactTypeRequest request = new CreateFactTypeRequest()
            .setName("FactType")
            .setValidator("Validator")
            .addRelevantObjectBinding(new FactObjectBindingDefinition())
            .addRelevantFactBinding(new MetaFactBindingDefinition());
    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(SetUtils.set("invalid.fact.type.definition"), SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testCreateFactTypeValidatorNotFound() {
    CreateFactTypeRequest request = createFactTypeRequest();
    when(getValidatorFactory().get(request.getValidator(), request.getValidatorParameter())).thenThrow(IllegalArgumentException.class);
    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(SetUtils.set("validator.not.exist"), SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testCreateFactType() throws Exception {
    CreateFactTypeRequest request = createFactTypeRequest();
    FactTypeEntity newEntity = new FactTypeEntity();
    when(getFactManager().saveFactType(any())).thenReturn(newEntity);

    delegate.handle(request);
    verify(factTypeHelper).assertFactTypeNotExists(request.getName());
    verify(factTypeHelper).assertObjectTypesToBindExist(request.getRelevantObjectBindings(), "relevantObjectBindings");
    verify(factTypeHelper).convertFactObjectBindingDefinitions(request.getRelevantObjectBindings());
    verify(getFactTypeConverter()).apply(newEntity);
    verify(getFactManager()).saveFactType(argThat(entity -> {
      assertCommonEntity(request, entity);
      assertEquals(1, entity.getRelevantObjectBindings().size());
      return true;
    }));
  }

  @Test
  public void testCreateMetaFactType() throws Exception {
    CreateFactTypeRequest request = createMetaFactTypeRequest();
    FactTypeEntity newEntity = new FactTypeEntity();
    when(getFactManager().saveFactType(any())).thenReturn(newEntity);

    delegate.handle(request);
    verify(factTypeHelper).assertFactTypeNotExists(request.getName());
    verify(factTypeHelper).assertFactTypesToBindExist(request.getRelevantFactBindings(), "relevantFactBindings");
    verify(factTypeHelper).convertMetaFactBindingDefinitions(request.getRelevantFactBindings());
    verify(getFactTypeConverter()).apply(newEntity);
    verify(getFactManager()).saveFactType(argThat(entity -> {
      assertCommonEntity(request, entity);
      assertEquals(1, entity.getRelevantFactBindings().size());
      return true;
    }));
  }

  private CreateFactTypeRequest createFactTypeRequest() {
    FactObjectBindingDefinition binding = new FactObjectBindingDefinition()
            .setSourceObjectType(UUID.randomUUID())
            .setDestinationObjectType(UUID.randomUUID())
            .setBidirectionalBinding(true);

    return new CreateFactTypeRequest()
            .setName("FactType")
            .setDefaultConfidence(0.1f)
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter")
            .addRelevantObjectBinding(binding);
  }

  private CreateFactTypeRequest createMetaFactTypeRequest() {
    MetaFactBindingDefinition binding = new MetaFactBindingDefinition()
            .setFactType(UUID.randomUUID());

    return new CreateFactTypeRequest()
            .setName("FactType")
            .setDefaultConfidence(0.1f)
            .setValidator("Validator")
            .setValidatorParameter("ValidatorParameter")
            .addRelevantFactBinding(binding);
  }

  private void assertCommonEntity(CreateFactTypeRequest request, FactTypeEntity entity) {
    assertNotNull(entity.getId());
    assertNotNull(entity.getNamespaceID());
    assertEquals(request.getName(), entity.getName());
    assertEquals(request.getDefaultConfidence(), entity.getDefaultConfidence(), 0.0);
    assertEquals(request.getValidator(), entity.getValidator());
    assertEquals(request.getValidatorParameter(), entity.getValidatorParameter());
  }
}
