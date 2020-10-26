package no.mnemonic.act.platform.service.contexts;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.UnexpectedAuthenticationFailedException;
import no.mnemonic.act.platform.auth.IdentitySPI;
import no.mnemonic.act.platform.service.TestSecurityContext;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import no.mnemonic.services.common.auth.model.SessionDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static no.mnemonic.act.platform.service.ti.TiFunctionConstants.viewThreatIntelFact;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("unchecked")
public class SecurityContextTest {

  @Mock
  private AccessController accessController;
  @Mock
  private IdentitySPI identityResolver;
  @Mock
  private Credentials credentials;
  @Mock
  private SessionDescriptor sessionDescriptor;
  @Mock
  private OrganizationIdentity organization;

  private SecurityContext context;

  @Before
  public void setUp() {
    initMocks(this);
    context = new TestSecurityContext(accessController, identityResolver, credentials);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutAccessControllerThrowsException() {
    new TestSecurityContext(null, identityResolver, credentials);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutIdentityResolverThrowsException() {
    new TestSecurityContext(accessController, null, credentials);
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutCredentialsThrowsException() {
    new TestSecurityContext(accessController, identityResolver, null);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetContextNotSet() {
    SecurityContext.get();
  }

  @Test(expected = IllegalStateException.class)
  public void testSetContextTwice() {
    try (SecurityContext ignored = SecurityContext.set(context)) {
      SecurityContext.set(context);
    }
  }

  @Test
  public void testSetAndGetContext() {
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
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(true);
    context.checkPermission(viewThreatIntelFact);
    verify(accessController).hasPermission(credentials, viewThreatIntelFact);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckPermissionThrowsAccessDeniedException() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(false);
    context.checkPermission(viewThreatIntelFact);
  }

  @Test(expected = AuthenticationFailedException.class)
  public void testCheckPermissionThrowsAuthenticationFailedException() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenThrow(InvalidCredentialsException.class);
    context.checkPermission(viewThreatIntelFact);
  }

  @Test
  public void testCheckPermissionForOrganizationWithAccess() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewThreatIntelFact, organization)).thenReturn(true);
    context.checkPermission(viewThreatIntelFact, organizationID);
    verify(accessController).hasPermission(credentials, viewThreatIntelFact, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckPermissionForOrganizationThrowsAccessDeniedException() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewThreatIntelFact, organization)).thenReturn(false);
    context.checkPermission(viewThreatIntelFact, organizationID);
  }

  @Test(expected = AuthenticationFailedException.class)
  public void testCheckPermissionForOrganizationThrowsAuthenticationFailedException() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewThreatIntelFact, organization)).thenThrow(InvalidCredentialsException.class);
    context.checkPermission(viewThreatIntelFact, organizationID);
  }

  @Test
  public void testGetCurrentUserID() throws Exception {
    UUID currentUserID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(accessController.validate(credentials)).thenReturn(sessionDescriptor);
    when(identityResolver.resolveSubjectUUID(sessionDescriptor)).thenReturn(currentUserID);
    assertEquals(currentUserID, context.getCurrentUserID());
  }

  @Test(expected = UnexpectedAuthenticationFailedException.class)
  public void testGetCurrentUserIdThrowsUnexpectedAuthenticationFailedException() throws Exception {
    when(accessController.validate(credentials)).thenThrow(InvalidCredentialsException.class);
    context.getCurrentUserID();
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
