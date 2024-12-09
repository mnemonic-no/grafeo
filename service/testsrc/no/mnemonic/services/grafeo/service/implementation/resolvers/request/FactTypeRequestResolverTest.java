package no.mnemonic.services.grafeo.service.implementation.resolvers.request;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static no.mnemonic.services.grafeo.service.implementation.GrafeoServiceImpl.GLOBAL_NAMESPACE;
import static no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver.RETRACTION_FACT_TYPE_ID;
import static no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver.RETRACTION_FACT_TYPE_NAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactTypeRequestResolverTest {

  @Mock
  private FactManager factManager;
  @InjectMocks
  private FactTypeRequestResolver resolver;

  @Test
  public void testFetchFactType() throws Exception {
    UUID id = UUID.randomUUID();
    FactTypeEntity entity = new FactTypeEntity();
    when(factManager.getFactType(id)).thenReturn(entity);
    assertSame(entity, resolver.fetchExistingFactType(id));
  }

  @Test
  public void testFetchFactTypeNotFound() {
    UUID id = UUID.randomUUID();
    assertThrows(ObjectNotFoundException.class, () -> resolver.fetchExistingFactType(id));
    verify(factManager).getFactType(id);
  }

  @Test
  public void testResolveFactTypeById() throws Exception {
    UUID type = UUID.randomUUID();
    FactTypeEntity entity = new FactTypeEntity();
    when(factManager.getFactType(type)).thenReturn(entity);

    assertSame(entity, resolver.resolveFactType(type.toString()));

    verify(factManager).getFactType(type);
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testResolveFactTypeByName() throws Exception {
    String type = "FactType";
    FactTypeEntity entity = new FactTypeEntity();
    when(factManager.getFactType(type)).thenReturn(entity);

    assertSame(entity, resolver.resolveFactType(type));

    verify(factManager).getFactType(type);
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testResolveFactTypeByIdThrowsException() {
    assertThrows(InvalidArgumentException.class, () -> resolver.resolveFactType(UUID.randomUUID().toString()));
  }

  @Test
  public void testResolveFactTypeByNameThrowsException() {
    assertThrows(InvalidArgumentException.class, () -> resolver.resolveFactType("FactType"));
  }

  @Test
  public void testResolveFactTypeRetractionThrowsException() {
    FactTypeEntity retraction = new FactTypeEntity().setId(RETRACTION_FACT_TYPE_ID);
    when(factManager.getFactType(retraction.getId())).thenReturn(retraction);
    assertThrows(AccessDeniedException.class, () -> resolver.resolveFactType(retraction.getId().toString()));
  }

  @Test
  public void testResolveRetractionFactType() {
    FactTypeEntity retraction = new FactTypeEntity();
    when(factManager.getFactType(RETRACTION_FACT_TYPE_ID)).thenReturn(retraction);

    assertSame(retraction, resolver.resolveRetractionFactType());

    verify(factManager).getFactType(RETRACTION_FACT_TYPE_ID);
    verifyNoMoreInteractions(factManager);
  }

  @Test
  public void testCreateRetractionFactTypeOnDemand() {
    when(factManager.saveFactType(any())).thenAnswer(i -> i.getArgument(0));

    assertRetractionFactType(resolver.resolveRetractionFactType());

    verify(factManager).saveFactType(argThat(e -> {
      assertRetractionFactType(e);
      return true;
    }));
  }

  @Test
  public void testCreateRetractionFactTypeAvoidNameCollision() {
    when(factManager.getFactType(RETRACTION_FACT_TYPE_ID)).thenReturn(null);
    when(factManager.getFactType(RETRACTION_FACT_TYPE_NAME)).thenReturn(new FactTypeEntity());
    when(factManager.saveFactType(any())).thenAnswer(i -> i.getArgument(0));

    assertRetractionFactType(resolver.resolveRetractionFactType());

    verify(factManager).saveFactType(argThat(e -> {
      assertRetractionFactType(e);
      return true;
    }));
  }

  private void assertRetractionFactType(FactTypeEntity retraction) {
    assertEquals(RETRACTION_FACT_TYPE_ID, retraction.getId());
    assertEquals(GLOBAL_NAMESPACE, retraction.getNamespaceID());
    assertTrue(retraction.getName().startsWith(RETRACTION_FACT_TYPE_NAME));
    assertEquals(1.0f, retraction.getDefaultConfidence(), 0.0);
    assertEquals("TrueValidator", retraction.getValidator());
  }
}
