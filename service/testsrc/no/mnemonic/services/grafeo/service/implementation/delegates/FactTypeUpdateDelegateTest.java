package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.services.grafeo.api.request.v1.MetaFactBindingDefinition;
import no.mnemonic.services.grafeo.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactTypeResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.helpers.FactTypeHelper;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactTypeUpdateDelegateTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactTypeResponseConverter factTypeResponseConverter;
  @Mock
  private FactTypeHelper factTypeHelper;
  @Mock
  private FactTypeRequestResolver factTypeRequestResolver;
  @Mock
  private GrafeoSecurityContext securityContext;
  @InjectMocks
  private FactTypeUpdateDelegate delegate;

  private final FactTypeEntity retractionFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("Retraction");

  @BeforeEach
  public void setup() {
    lenient().when(factManager.saveFactType(any())).then(i -> i.getArgument(0));
    lenient().when(factTypeHelper.convertFactObjectBindingDefinitions(anyList())).thenReturn(SetUtils.set(new FactTypeEntity.FactObjectBindingDefinition()));
    lenient().when(factTypeHelper.convertMetaFactBindingDefinitions(anyList())).thenReturn(SetUtils.set(new FactTypeEntity.MetaFactBindingDefinition()));
    lenient().when(factTypeRequestResolver.resolveRetractionFactType()).thenReturn(retractionFactType);
  }

  @Test
  public void testUpdateFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(FunctionConstants.updateGrafeoType);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new UpdateFactTypeRequest()));
  }

  @Test
  public void testUpdateFactTypeNotExisting() throws Exception {
    UUID id = UUID.randomUUID();
    when(factTypeRequestResolver.fetchExistingFactType(id)).thenThrow(ObjectNotFoundException.class);
    assertThrows(ObjectNotFoundException.class, () -> delegate.handle(new UpdateFactTypeRequest().setId(id)));
  }

  @Test
  public void testUpdateRetractionFactTypeNotAllowed() throws Exception {
    when(factTypeRequestResolver.fetchExistingFactType(retractionFactType.getId())).thenReturn(retractionFactType);
    assertThrows(AccessDeniedException.class, () -> delegate.handle(new UpdateFactTypeRequest().setId(retractionFactType.getId())));
  }

  @Test
  public void testUpdateFactTypeWithName() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setName("newName");
    FactTypeEntity existingEntity = new FactTypeEntity();

    when(factTypeRequestResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
    verify(factTypeHelper).assertFactTypeNotExists(request.getName());
    verify(factTypeResponseConverter).apply(existingEntity);
    verify(factManager).saveFactType(argThat(entity -> {
      assertSame(existingEntity, entity);
      assertEquals(request.getName(), entity.getName());
      return true;
    }));
  }

  @Test
  public void testUpdateFactTypeWithDefaultConfidence() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setDefaultConfidence(0.1f);
    FactTypeEntity existingEntity = new FactTypeEntity();
    when(factTypeRequestResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
    verify(factTypeResponseConverter).apply(existingEntity);
    verify(factManager).saveFactType(argThat(entity -> {
      assertSame(existingEntity, entity);
      assertEquals(request.getDefaultConfidence(), entity.getDefaultConfidence(), 0.0);
      return true;
    }));
  }

  @Test
  public void testUpdateFactTypeWithObjectBindingsFailsOnExistingFactBindings() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddObjectBinding(new FactObjectBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity()
            .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition());
    when(factTypeRequestResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testUpdateFactTypeWithObjectBindingsSuccess() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddObjectBinding(new FactObjectBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity();
    when(factTypeRequestResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
    verify(factTypeHelper).assertObjectTypesToBindExist(request.getAddObjectBindings(), "addObjectBindings");
    verify(factTypeHelper).convertFactObjectBindingDefinitions(request.getAddObjectBindings());
    verify(factTypeResponseConverter).apply(existingEntity);
    verify(factManager).saveFactType(argThat(entity -> {
      assertSame(existingEntity, entity);
      assertEquals(1, entity.getRelevantObjectBindings().size());
      return true;
    }));
  }

  @Test
  public void testUpdateFactTypeWithFactBindingsFailsOnExistingObjectBindings() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddFactBinding(new MetaFactBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity()
            .addRelevantObjectBinding(new FactTypeEntity.FactObjectBindingDefinition());
    when(factTypeRequestResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }

  @Test
  public void testUpdateFactTypeWithFactBindingsSuccess() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddFactBinding(new MetaFactBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity();
    when(factTypeRequestResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
    verify(factTypeHelper).assertFactTypesToBindExist(request.getAddFactBindings(), "addFactBindings");
    verify(factTypeHelper).convertMetaFactBindingDefinitions(request.getAddFactBindings());
    verify(factTypeResponseConverter).apply(existingEntity);
    verify(factManager).saveFactType(argThat(entity -> {
      assertSame(existingEntity, entity);
      assertEquals(1, entity.getRelevantFactBindings().size());
      return true;
    }));
  }

  @Test
  public void testUpdateFactTypeWithBothObjectBindingsAndFactBindings() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddObjectBinding(new FactObjectBindingDefinition())
            .addAddFactBinding(new MetaFactBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity();
    when(factTypeRequestResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
  }
}
