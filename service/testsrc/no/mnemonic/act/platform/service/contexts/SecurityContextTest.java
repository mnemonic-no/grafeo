package no.mnemonic.act.platform.service.contexts;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.UnexpectedAuthenticationFailedException;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.service.TestSecurityContext;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.TiFunctionConstants.viewFactObjects;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("unchecked")
public class SecurityContextTest {

  @Mock
  private AccessController accessController;
  @Mock
  private IdentityResolver identityResolver;
  @Mock
  private OrganizationResolver organizationResolver;
  @Mock
  private SubjectResolver subjectResolver;
  @Mock
  private Credentials credentials;
  @Mock
  private OrganizationIdentity organization;

  private SecurityContext context;

  @Before
  public void setUp() {
    initMocks(this);
    context = new TestSecurityContext(accessController, identityResolver, organizationResolver, subjectResolver, credentials);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutAccessControllerThrowsException() {
    new TestSecurityContext(null, identityResolver, organizationResolver, subjectResolver, credentials);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutIdentityResolverThrowsException() {
    new TestSecurityContext(accessController, null, organizationResolver, subjectResolver, credentials);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutOrganizationResolverThrowsException() {
    new TestSecurityContext(accessController, identityResolver, null, subjectResolver, credentials);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutSubjectResolverThrowsException() {
    new TestSecurityContext(accessController, identityResolver, organizationResolver, null, credentials);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutCredentialsThrowsException() {
    new TestSecurityContext(accessController, identityResolver, organizationResolver, subjectResolver, null);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetContextNotSet() {
    SecurityContext.get();
  }

  @Test(expected = IllegalStateException.class)
  public void testSetContextTwice() throws Exception {
    try (SecurityContext ignored = SecurityContext.set(context)) {
      SecurityContext.set(context);
    }
  }

  @Test
  public void testSetAndGetContext() throws Exception {
    assertFalse(SecurityContext.isSet());

    try (SecurityContext ctx = SecurityContext.set(context)) {
      assertTrue(SecurityContext.isSet());
      assertSame(ctx, SecurityContext.get());
    }

    assertFalse(SecurityContext.isSet());
  }

  @Test
  public void testClearExistingContext() {
    SecurityContext ctx = SecurityContext.set(context);
    assertTrue(SecurityContext.isSet());
    SecurityContext oldCtx = SecurityContext.clear();
    assertFalse(SecurityContext.isSet());
    assertSame(ctx, oldCtx);
  }

  @Test
  public void testClearNonExistingContext() {
    assertNull(SecurityContext.clear());
    assertFalse(SecurityContext.isSet());
  }

  @Test
  public void testCheckPermissionWithAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewFactObjects)).thenReturn(true);
    context.checkPermission(viewFactObjects);
    verify(accessController).hasPermission(credentials, viewFactObjects);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckPermissionThrowsAccessDeniedException() throws Exception {
    when(accessController.hasPermission(credentials, viewFactObjects)).thenReturn(false);
    context.checkPermission(viewFactObjects);
  }

  @Test(expected = AuthenticationFailedException.class)
  public void testCheckPermissionThrowsAuthenticationFailedException() throws Exception {
    when(accessController.hasPermission(credentials, viewFactObjects)).thenThrow(InvalidCredentialsException.class);
    context.checkPermission(viewFactObjects);
  }

  @Test
  public void testCheckPermissionForOrganizationWithAccess() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewFactObjects, organization)).thenReturn(true);
    context.checkPermission(viewFactObjects, organizationID);
    verify(accessController).hasPermission(credentials, viewFactObjects, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckPermissionForOrganizationThrowsAccessDeniedException() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewFactObjects, organization)).thenReturn(false);
    context.checkPermission(viewFactObjects, organizationID);
  }

  @Test(expected = AuthenticationFailedException.class)
  public void testCheckPermissionForOrganizationThrowsAuthenticationFailedException() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewFactObjects, organization)).thenThrow(InvalidCredentialsException.class);
    context.checkPermission(viewFactObjects, organizationID);
  }

  @Test
  public void testGetCurrentUserID() throws Exception {
    UUID currentUserID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(subjectResolver.resolveCurrentUser(credentials)).thenReturn(Subject.builder().setId(currentUserID).build());
    assertEquals(currentUserID, context.getCurrentUserID());
  }

  @Test(expected = UnexpectedAuthenticationFailedException.class)
  public void testGetCurrentUserIdThrowsUnexpectedAuthenticationFailedException() throws Exception {
    when(subjectResolver.resolveCurrentUser(credentials)).thenThrow(InvalidCredentialsException.class);
    context.getCurrentUserID();
  }

  @Test
  public void testGetCurrentUserOrganizationID() throws Exception {
    UUID currentUserOrganizationID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(organizationResolver.resolveCurrentUserAffiliation(credentials)).thenReturn(Organization.builder().setId(currentUserOrganizationID).build());
    assertEquals(currentUserOrganizationID, context.getCurrentUserOrganizationID());
  }

  @Test(expected = UnexpectedAuthenticationFailedException.class)
  public void testGetCurrentUserOrganizationIdThrowsUnexpectedAuthenticationFailedException() throws Exception {
    when(organizationResolver.resolveCurrentUserAffiliation(credentials)).thenThrow(InvalidCredentialsException.class);
    context.getCurrentUserOrganizationID();
  }

  @Test
  public void testGetAvailableOrganizationID() throws Exception {
    UUID organizationID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(accessController.getAvailableOrganizations(credentials)).thenReturn(SetUtils.set(organization));
    when(identityResolver.resolveOrganizationUUID(organization)).thenReturn(organizationID);
    assertEquals(SetUtils.set(organizationID), context.getAvailableOrganizationID());
  }

  @Test(expected = UnexpectedAuthenticationFailedException.class)
  public void testGetAvailableOrganizationIdThrowsUnexpectedAuthenticationFailedException() throws Exception {
    when(accessController.getAvailableOrganizations(credentials)).thenThrow(InvalidCredentialsException.class);
    context.getAvailableOrganizationID();
  }

}
