package no.mnemonic.services.grafeo.service.implementation.resolvers.request;

import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.dao.cassandra.FactManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static no.mnemonic.services.grafeo.service.implementation.GrafeoServiceImpl.GLOBAL_NAMESPACE;
import static no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver.RETRACTION_FACT_TYPE_ID;
import static no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactTypeRequestResolver.RETRACTION_FACT_TYPE_NAME;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactTypeRequestResolverTest {

  @Mock
  private FactManager factManager;

  private FactTypeRequestResolver resolver;

  @Before
  public void initialize() {
    initMocks(this);
    resolver = new FactTypeRequestResolver(factManager);
  }

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

  @Test(expected = InvalidArgumentException.class)
  public void testResolveFactTypeByIdThrowsException() throws Exception {
    resolver.resolveFactType(UUID.randomUUID().toString());
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveFactTypeByNameThrowsException() throws Exception {
    resolver.resolveFactType("FactType");
  }

  @Test(expected = AccessDeniedException.class)
  public void testResolveFactTypeRetractionThrowsException() throws Exception {
    FactTypeEntity retraction = new FactTypeEntity().setId(RETRACTION_FACT_TYPE_ID);
    when(factManager.getFactType(retraction.getId())).thenReturn(retraction);
    resolver.resolveFactType(retraction.getId().toString());
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
