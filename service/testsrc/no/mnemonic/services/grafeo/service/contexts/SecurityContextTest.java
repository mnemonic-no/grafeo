package no.mnemonic.services.grafeo.service.contexts;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import no.mnemonic.services.common.auth.model.SessionDescriptor;
import no.mnemonic.services.common.auth.model.SubjectIdentity;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.UnexpectedAuthenticationFailedException;
import no.mnemonic.services.grafeo.auth.IdentitySPI;
import no.mnemonic.services.grafeo.service.TestSecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static no.mnemonic.services.grafeo.service.implementation.FunctionConstants.viewGrafeoFact;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
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
  @Mock
  private SubjectIdentity subject;

  private SecurityContext context;

  @BeforeEach
  public void setUp() {
    context = new TestSecurityContext(accessController, identityResolver, credentials);
  }

  @Test
  public void testCreateContextWithoutAccessControllerThrowsException() {
    assertThrows(RuntimeException.class, () -> new TestSecurityContext(null, identityResolver, credentials));
  }

  @Test
  public void testCreateContextWithoutIdentityResolverThrowsException() {
    assertThrows(RuntimeException.class, () -> new TestSecurityContext(accessController, null, credentials));
  }

  @Test
  public void testCreateContextWithoutCredentialsThrowsException() {
    assertThrows(RuntimeException.class, () -> new TestSecurityContext(accessController, identityResolver, null));
  }

  @Test
  public void testGetContextNotSet() {
    assertThrows(IllegalStateException.class, SecurityContext::get);
  }

  @Test
  public void testSetContextTwice() {
    try (SecurityContext ignored = SecurityContext.set(context)) {
      assertThrows(IllegalStateException.class, () -> SecurityContext.set(context));
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
    when(accessController.hasPermission(credentials, viewGrafeoFact)).thenReturn(true);
    context.checkPermission(viewGrafeoFact);
    verify(accessController).hasPermission(credentials, viewGrafeoFact);
  }

  @Test
  public void testCheckPermissionThrowsAccessDeniedException() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoFact)).thenReturn(false);
    assertThrows(AccessDeniedException.class, () -> context.checkPermission(viewGrafeoFact));
  }

  @Test
  public void testCheckPermissionThrowsAuthenticationFailedException() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoFact)).thenThrow(InvalidCredentialsException.class);
    assertThrows(AuthenticationFailedException.class, () -> context.checkPermission(viewGrafeoFact));
  }

  @Test
  public void testCheckPermissionForOrganizationWithAccess() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewGrafeoFact, organization)).thenReturn(true);
    context.checkPermission(viewGrafeoFact, organizationID);
    verify(accessController).hasPermission(credentials, viewGrafeoFact, organization);
  }

  @Test
  public void testCheckPermissionForOrganizationThrowsAccessDeniedException() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewGrafeoFact, organization)).thenReturn(false);
    assertThrows(AccessDeniedException.class, () -> context.checkPermission(viewGrafeoFact, organizationID));
  }

  @Test
  public void testCheckPermissionForOrganizationThrowsAuthenticationFailedException() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewGrafeoFact, organization)).thenThrow(InvalidCredentialsException.class);
    assertThrows(AuthenticationFailedException.class, () -> context.checkPermission(viewGrafeoFact, organizationID));
  }

  @Test
  public void testGetCurrentUserID() throws Exception {
    UUID currentUserID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(accessController.validate(credentials)).thenReturn(sessionDescriptor);
    when(identityResolver.resolveSubjectUUID(sessionDescriptor)).thenReturn(currentUserID);
    assertEquals(currentUserID, context.getCurrentUserID());
  }

  @Test
  public void testGetCurrentUserIdThrowsUnexpectedAuthenticationFailedException() throws Exception {
    when(accessController.validate(credentials)).thenThrow(InvalidCredentialsException.class);
    assertThrows(UnexpectedAuthenticationFailedException.class, () -> context.getCurrentUserID());
  }

  @Test
  public void testGetCurrentUserIdentities() throws Exception {
    UUID subjectID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(accessController.getSubjectIdentities(credentials)).thenReturn(SetUtils.set(subject));
    when(identityResolver.resolveSubjectUUID(subject)).thenReturn(subjectID);
    assertEquals(SetUtils.set(subjectID), context.getCurrentUserIdentities());
  }

  @Test
  public void testGetCurrentUserIdentitiesThrowsUnexpectedAuthenticationFailedException() throws Exception {
    when(accessController.getSubjectIdentities(credentials)).thenThrow(InvalidCredentialsException.class);
    assertThrows(UnexpectedAuthenticationFailedException.class, () -> context.getCurrentUserIdentities());
  }

  @Test
  public void testGetAvailableOrganizationID() throws Exception {
    UUID organizationID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(accessController.getAvailableOrganizations(credentials)).thenReturn(SetUtils.set(organization));
    when(identityResolver.resolveOrganizationUUID(organization)).thenReturn(organizationID);
    assertEquals(SetUtils.set(organizationID), context.getAvailableOrganizationID());
  }

  @Test
  public void testGetAvailableOrganizationIdThrowsUnexpectedAuthenticationFailedException() throws Exception {
    when(accessController.getAvailableOrganizations(credentials)).thenThrow(InvalidCredentialsException.class);
    assertThrows(UnexpectedAuthenticationFailedException.class, () -> context.getAvailableOrganizationID());
  }

}
