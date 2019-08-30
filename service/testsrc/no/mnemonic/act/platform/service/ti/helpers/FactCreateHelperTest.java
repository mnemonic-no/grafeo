package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactCreateHelperTest {

  @Mock
  private TiSecurityContext securityContext;
  @Mock
  private SubjectResolver subjectResolver;
  @Mock
  private OrganizationResolver organizationResolver;
  @Mock
  private Function<UUID, OriginEntity> originResolver;
  @Mock
  private OriginManager originManager;

  private FactCreateHelper helper;

  @Before
  public void setUp() {
    initMocks(this);
    helper = new FactCreateHelper(securityContext, subjectResolver, organizationResolver, originResolver, originManager);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOrganizationProvidedOrganizationNotExists() throws Exception {
    helper.resolveOrganization(UUID.randomUUID(), null);
  }

  @Test
  public void testResolveOrganizationProvidedFetchesExistingOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    Organization organization = Organization.builder().build();
    when(organizationResolver.resolveOrganization(organizationID)).thenReturn(organization);

    assertSame(organization, helper.resolveOrganization(organizationID, null));
    verify(organizationResolver).resolveOrganization(organizationID);
    verify(securityContext).checkReadPermission(organization);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOrganizationFallbackToOriginOrganizationNotExists() throws Exception {
    helper.resolveOrganization(null, new OriginEntity().setOrganizationID(UUID.randomUUID()));
  }

  @Test
  public void testResolveOrganizationFallbackToOriginFetchesExistingOrganization() throws Exception {
    OriginEntity origin = new OriginEntity().setOrganizationID(UUID.randomUUID());
    Organization organization = Organization.builder().build();
    when(organizationResolver.resolveOrganization(origin.getOrganizationID())).thenReturn(organization);

    assertSame(organization, helper.resolveOrganization(null, origin));
    verify(organizationResolver).resolveOrganization(origin.getOrganizationID());
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOrganizationFallbackToCurrentUserOrganizationNotExists() throws Exception {
    when(securityContext.getCurrentUserOrganizationID()).thenReturn(UUID.randomUUID());
    helper.resolveOrganization(null, new OriginEntity());
  }

  @Test
  public void testResolveOrganizationFallbackToCurrentUserFetchesExistingOrganization() throws Exception {
    UUID currentUserOrganizationID = UUID.randomUUID();
    Organization organization = Organization.builder().build();
    when(securityContext.getCurrentUserOrganizationID()).thenReturn(currentUserOrganizationID);
    when(organizationResolver.resolveOrganization(currentUserOrganizationID)).thenReturn(organization);

    assertSame(organization, helper.resolveOrganization(null, new OriginEntity()));
    verify(securityContext).getCurrentUserOrganizationID();
    verify(organizationResolver).resolveOrganization(currentUserOrganizationID);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOriginProvidedOriginNotExists() throws Exception {
    helper.resolveOrigin(UUID.randomUUID());
  }

  @Test
  public void testResolveOriginProvidedFetchesExistingOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity().setId(originID);
    when(originResolver.apply(originID)).thenReturn(origin);

    assertSame(origin, helper.resolveOrigin(originID));
    verify(originResolver).apply(originID);
    verify(securityContext).checkReadPermission(origin);
    verifyZeroInteractions(originManager);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOriginProvidedFetchesDeletedOrigin() throws Exception {
    UUID originID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity()
            .setId(originID)
            .addFlag(OriginEntity.Flag.Deleted);
    when(originResolver.apply(originID)).thenReturn(origin);

    helper.resolveOrigin(originID);
  }

  @Test
  public void testResolveOriginNonProvidedFetchesExistingOrigin() throws Exception {
    UUID currentUserID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity().setId(currentUserID);
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(originResolver.apply(currentUserID)).thenReturn(origin);

    assertSame(origin, helper.resolveOrigin(null));
    verify(originResolver).apply(currentUserID);
    verifyZeroInteractions(originManager);
  }

  @Test(expected = InvalidArgumentException.class)
  public void testResolveOriginNonProvidedFetchesDeletedOrigin() throws Exception {
    UUID currentUserID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity()
            .setId(currentUserID)
            .addFlag(OriginEntity.Flag.Deleted);
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(originResolver.apply(currentUserID)).thenReturn(origin);

    helper.resolveOrigin(null);
  }

  @Test
  public void testResolveOriginNonProvidedCreatesNewOrigin() throws Exception {
    UUID currentUserID = UUID.randomUUID();
    Subject currentUser = Subject.builder()
            .setId(currentUserID)
            .setName("name")
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).build().toInfo())
            .build();
    OriginEntity newOrigin = new OriginEntity().setId(currentUserID);
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(subjectResolver.resolveSubject(currentUserID)).thenReturn(currentUser);
    when(originManager.saveOrigin(notNull())).thenReturn(newOrigin);

    assertSame(newOrigin, helper.resolveOrigin(null));
    verify(originResolver).apply(currentUserID);
    verify(subjectResolver).resolveSubject(currentUserID);
    verify(originManager).saveOrigin(argThat(entity -> {
      assertEquals(currentUserID, entity.getId());
      assertNotNull(entity.getNamespaceID());
      assertEquals(currentUser.getOrganization().getId(), entity.getOrganizationID());
      assertEquals(currentUser.getName(), entity.getName());
      assertEquals(0.8f, entity.getTrust(), 0.0);
      assertEquals(OriginEntity.Type.User, entity.getType());
      return true;
    }));
  }

  @Test
  public void testResolveOriginNonProvidedCreatesNewOriginAvoidingNameCollision() throws Exception {
    UUID currentUserID = UUID.randomUUID();
    Subject currentUser = Subject.builder()
            .setId(currentUserID)
            .setName("name")
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).build().toInfo())
            .build();
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(subjectResolver.resolveSubject(currentUserID)).thenReturn(currentUser);
    when(originManager.getOrigin(currentUser.getName())).thenReturn(new OriginEntity()
            .setId(UUID.randomUUID())
            .setName(currentUser.getName()));

    helper.resolveOrigin(null);
    verify(originManager).getOrigin(currentUser.getName());
    verify(originManager).saveOrigin(argThat(entity -> {
      assertEquals(currentUserID, entity.getId());
      assertNotEquals(currentUser.getName(), entity.getName());
      assertTrue(entity.getName().startsWith(currentUser.getName()));
      return true;
    }));
  }
}
