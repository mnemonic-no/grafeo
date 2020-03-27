package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.request.v1.FactObjectBindingDefinition;
import no.mnemonic.act.platform.api.request.v1.MetaFactBindingDefinition;
import no.mnemonic.act.platform.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.FactTypeConverter;
import no.mnemonic.act.platform.service.ti.helpers.FactTypeHelper;
import no.mnemonic.act.platform.service.ti.resolvers.FactTypeResolver;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactTypeUpdateDelegateTest {

  @Mock
  private FactManager factManager;
  @Mock
  private FactTypeConverter factTypeConverter;
  @Mock
  private FactTypeHelper factTypeHelper;
  @Mock
  private FactTypeResolver factTypeResolver;
  @Mock
  private TiSecurityContext securityContext;

  private final FactTypeEntity retractionFactType = new FactTypeEntity()
          .setId(UUID.randomUUID())
          .setName("Retraction");

  private FactTypeUpdateDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    when(factManager.saveFactType(any())).then(i -> i.getArgument(0));
    when(factTypeHelper.convertFactObjectBindingDefinitions(anyList())).thenReturn(SetUtils.set(new FactTypeEntity.FactObjectBindingDefinition()));
    when(factTypeHelper.convertMetaFactBindingDefinitions(anyList())).thenReturn(SetUtils.set(new FactTypeEntity.MetaFactBindingDefinition()));
    when(factTypeResolver.resolveRetractionFactType()).thenReturn(retractionFactType);
    delegate = new FactTypeUpdateDelegate(
      securityContext,
      factManager,
      factTypeHelper,
      factTypeResolver,
      factTypeConverter);
  }

  @Test(expected = AccessDeniedException.class)
  public void testUpdateFactTypeWithoutPermission() throws Exception {
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(TiFunctionConstants.updateTypes);
    delegate.handle(new UpdateFactTypeRequest());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void testUpdateFactTypeNotExisting() throws Exception {
    UUID id = UUID.randomUUID();
    when(factTypeResolver.fetchExistingFactType(id)).thenThrow(ObjectNotFoundException.class);
    delegate.handle(new UpdateFactTypeRequest().setId(id));
  }

  @Test(expected = AccessDeniedException.class)
  public void testUpdateRetractionFactTypeNotAllowed() throws Exception {
    when(factTypeResolver.fetchExistingFactType(retractionFactType.getId())).thenReturn(retractionFactType);
    delegate.handle(new UpdateFactTypeRequest().setId(retractionFactType.getId()));
  }

  @Test
  public void testUpdateFactTypeWithName() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .setName("newName");
    FactTypeEntity existingEntity = new FactTypeEntity();

    when(factTypeResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
    verify(factTypeHelper).assertFactTypeNotExists(request.getName());
    verify(factTypeConverter).apply(existingEntity);
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
    when(factTypeResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
    verify(factTypeConverter).apply(existingEntity);
    verify(factManager).saveFactType(argThat(entity -> {
      assertSame(existingEntity, entity);
      assertEquals(request.getDefaultConfidence(), entity.getDefaultConfidence(), 0.0);
      return true;
    }));
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateFactTypeWithObjectBindingsFailsOnExistingFactBindings() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddObjectBinding(new FactObjectBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity()
            .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition());
    when(factTypeResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
  }

  @Test
  public void testUpdateFactTypeWithObjectBindingsSuccess() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddObjectBinding(new FactObjectBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity();
    when(factTypeResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
    verify(factTypeHelper).assertObjectTypesToBindExist(request.getAddObjectBindings(), "addObjectBindings");
    verify(factTypeHelper).convertFactObjectBindingDefinitions(request.getAddObjectBindings());
    verify(factTypeConverter).apply(existingEntity);
    verify(factManager).saveFactType(argThat(entity -> {
      assertSame(existingEntity, entity);
      assertEquals(1, entity.getRelevantObjectBindings().size());
      return true;
    }));
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateFactTypeWithFactBindingsFailsOnExistingObjectBindings() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddFactBinding(new MetaFactBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity()
            .addRelevantObjectBinding(new FactTypeEntity.FactObjectBindingDefinition());
    when(factTypeResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
  }

  @Test
  public void testUpdateFactTypeWithFactBindingsSuccess() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddFactBinding(new MetaFactBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity();
    when(factTypeResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
    verify(factTypeHelper).assertFactTypesToBindExist(request.getAddFactBindings(), "addFactBindings");
    verify(factTypeHelper).convertMetaFactBindingDefinitions(request.getAddFactBindings());
    verify(factTypeConverter).apply(existingEntity);
    verify(factManager).saveFactType(argThat(entity -> {
      assertSame(existingEntity, entity);
      assertEquals(1, entity.getRelevantFactBindings().size());
      return true;
    }));
  }

  @Test(expected = InvalidArgumentException.class)
  public void testUpdateFactTypeWithBothObjectBindingsAndFactBindings() throws Exception {
    UpdateFactTypeRequest request = new UpdateFactTypeRequest()
            .setId(UUID.randomUUID())
            .addAddObjectBinding(new FactObjectBindingDefinition())
            .addAddFactBinding(new MetaFactBindingDefinition());
    FactTypeEntity existingEntity = new FactTypeEntity();
    when(factTypeResolver.fetchExistingFactType(request.getId())).thenReturn(existingEntity);

    delegate.handle(request);
  }
}
