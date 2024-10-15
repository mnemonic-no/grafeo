package no.mnemonic.services.grafeo.service.implementation;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import no.mnemonic.services.common.auth.model.SubjectIdentity;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.auth.IdentitySPI;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.UUID;

import static no.mnemonic.services.grafeo.service.implementation.FunctionConstants.viewGrafeoFact;
import static no.mnemonic.services.grafeo.service.implementation.FunctionConstants.viewGrafeoOrigin;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("unchecked")
public class GrafeoSecurityContextTest {

  @Mock
  private AccessController accessController;
  @Mock
  private IdentitySPI identityResolver;
  @Mock
  private Credentials credentials;
  @Mock
  private OrganizationIdentity organization;
  @Mock
  private SubjectIdentity subject;
  @Mock
  private ObjectFactDao objectFactDao;

  private GrafeoSecurityContext context;

  @Before
  public void initialize() {
    initMocks(this);
    when(identityResolver.resolveOrganizationIdentity(any())).thenReturn(organization);

    context = GrafeoSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setCredentials(credentials)
            .setObjectFactDao(objectFactDao)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutObjectFactDaoThrowsException() {
    GrafeoSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setCredentials(credentials)
            .build();
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForFactRecordWithoutFact() throws Exception {
    context.checkReadPermission((FactRecord) null);
  }

  @Test
  public void testCheckReadPermissionForFactRecordWithAccessModePublic() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoFact)).thenReturn(true);
    context.checkReadPermission(new FactRecord().setAccessMode(FactRecord.AccessMode.Public));
    verify(accessController).hasPermission(credentials, viewGrafeoFact);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForFactRecordWithAccessModePublicNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoFact)).thenReturn(false);
    context.checkReadPermission(new FactRecord().setAccessMode(FactRecord.AccessMode.Public));
  }

  @Test
  public void testCheckReadPermissionForFactRecordWithAccessModeRoleBased() throws Exception {
    FactRecord fact = new FactRecord()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased);

    when(accessController.hasPermission(credentials, viewGrafeoFact, organization)).thenReturn(true);
    context.checkReadPermission(fact);
    verify(accessController).hasPermission(credentials, viewGrafeoFact, organization);
  }

  @Test
  public void testCheckReadPermissionForFactRecordWithAccessModeRoleBasedUserInAcl() throws Exception {
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased)
            .addAclEntry(new FactAclEntryRecord().setSubjectID(mockCurrentUserIdentities()));

    context.checkReadPermission(fact);
    verify(accessController, never()).hasPermission(credentials, viewGrafeoFact, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForFactRecordWithAccessModeRoleBasedNoAccess() throws Exception {
    FactRecord fact = new FactRecord()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased);

    when(accessController.hasPermission(credentials, viewGrafeoFact, organization)).thenReturn(false);
    context.checkReadPermission(fact);
  }

  @Test
  public void testCheckReadPermissionForFactRecordWithAccessModeExplicit() throws Exception {
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Explicit)
            .addAclEntry(new FactAclEntryRecord().setSubjectID(mockCurrentUserIdentities()));

    context.checkReadPermission(fact);
    verify(accessController, never()).hasPermission(credentials, viewGrafeoFact, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForFactRecordWithAccessModeExplicitNoAccess() throws Exception {
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Explicit);

    context.checkReadPermission(fact);
  }

  @Test
  public void testCheckReadPermissionForFactRecordFallbackToRoleBased() throws Exception {
    FactRecord fact = new FactRecord()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(null);

    when(accessController.hasPermission(credentials, viewGrafeoFact, organization)).thenReturn(true);
    context.checkReadPermission(fact);
    verify(accessController).hasPermission(credentials, viewGrafeoFact, organization);
  }

  @Test
  public void testHasReadPermissionForFactRecordReturnsTrueOnAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoFact)).thenReturn(true);
    assertTrue(context.hasReadPermission(new FactRecord().setAccessMode(FactRecord.AccessMode.Public)));
  }

  @Test
  public void testHasReadPermissionForFactRecordReturnsFalseOnNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoFact)).thenReturn(false);
    assertFalse(context.hasReadPermission(new FactRecord().setAccessMode(FactRecord.AccessMode.Public)));
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForObjectRecordWithoutObject() throws Exception {
    context.checkReadPermission((ObjectRecord) null);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForObjectRecordWithoutBoundFact() throws Exception {
    when(objectFactDao.retrieveObjectFacts(notNull())).thenReturn(Collections.emptyIterator());
    context.checkReadPermission(new ObjectRecord().setId(UUID.randomUUID()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForObjectRecordWithoutAccessToFact() throws Exception {
    ObjectRecord object = mockCheckPermissionForObjectRecord(false);
    context.checkReadPermission(object);
  }

  @Test
  public void testCheckReadPermissionForObjectRecordWithAccessToFact() throws Exception {
    ObjectRecord object = mockCheckPermissionForObjectRecord(true);
    context.checkReadPermission(object);
    verify(accessController).hasPermission(credentials, viewGrafeoFact);
  }

  @Test
  public void testCheckReadPermissionForObjectRecordWithAccessToSecondFact() throws Exception {
    FactRecord fact = new FactRecord().setAccessMode(FactRecord.AccessMode.Public);

    when(objectFactDao.retrieveObjectFacts(notNull())).thenReturn(ListUtils.list(fact, fact, fact).iterator());
    when(accessController.hasPermission(credentials, viewGrafeoFact)).thenReturn(false, true, false);

    context.checkReadPermission(new ObjectRecord().setId(UUID.randomUUID()));
    verify(accessController, times(2)).hasPermission(credentials, viewGrafeoFact);
  }

  @Test
  public void testHasReadPermissionForObjectRecordReturnsTrueOnAccess() throws Exception {
    ObjectRecord object = mockCheckPermissionForObjectRecord(true);
    assertTrue(context.hasReadPermission(object));
  }

  @Test
  public void testHasReadPermissionForObjectRecordReturnsFalseOnNoAccess() throws Exception {
    ObjectRecord object = mockCheckPermissionForObjectRecord(false);
    assertFalse(context.hasReadPermission(object));
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForOriginWithoutOrigin() throws Exception {
    context.checkReadPermission((OriginEntity) null);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForOriginWithOrganizationNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoOrigin, organization)).thenReturn(false);
    context.checkReadPermission(new OriginEntity().setOrganizationID(UUID.randomUUID()));
  }

  @Test
  public void testCheckReadPermissionForOriginWithOrganizationAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoOrigin, organization)).thenReturn(true);
    context.checkReadPermission(new OriginEntity().setOrganizationID(UUID.randomUUID()));
    verify(accessController).hasPermission(credentials, viewGrafeoOrigin, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForOriginWithoutOrganizationNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoOrigin)).thenReturn(false);
    context.checkReadPermission(new OriginEntity());
  }

  @Test
  public void testCheckReadPermissionForOriginWithoutOrganizationAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoOrigin)).thenReturn(true);
    context.checkReadPermission(new OriginEntity());
    verify(accessController).hasPermission(credentials, viewGrafeoOrigin);
  }

  @Test
  public void testHasReadPermissionForOriginReturnsTrueOnAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoOrigin)).thenReturn(true);
    assertTrue(context.hasReadPermission(new OriginEntity()));
  }

  @Test
  public void testHasReadPermissionForOriginReturnsFalseOnNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewGrafeoOrigin)).thenReturn(false);
    assertFalse(context.hasReadPermission(new OriginEntity()));
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForOrganizationWithoutOrganization() throws Exception {
    context.checkReadPermission((Organization) null);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForOrganizationWithNoAccess() throws Exception {
    when(accessController.getAvailableOrganizations(credentials)).thenReturn(Collections.emptySet());
    context.checkReadPermission(Organization.builder().setId(UUID.randomUUID()).build());
  }

  @Test
  public void testCheckReadPermissionForOrganizationWithAccess() throws Exception {
    Organization org = mockAvailableOrganization();
    context.checkReadPermission(org);
    verify(accessController).getAvailableOrganizations(credentials);
  }

  @Test
  public void testHasReadPermissionForOrganizationReturnsTrueOnAccess() throws Exception {
    Organization org = mockAvailableOrganization();
    assertTrue(context.hasReadPermission(org));
  }

  @Test
  public void testHasReadPermissionForOrganizationReturnsFalseOnNoAccess() throws Exception {
    when(accessController.getAvailableOrganizations(credentials)).thenReturn(Collections.emptySet());
    assertFalse(context.hasReadPermission(Organization.builder().setId(UUID.randomUUID()).build()));
  }

  private ObjectRecord mockCheckPermissionForObjectRecord(boolean result) throws Exception {
    ObjectRecord object = new ObjectRecord().setId(UUID.randomUUID());
    FactRecord fact = new FactRecord().setAccessMode(FactRecord.AccessMode.Public);

    // Mock retrieval of bound Facts.
    when(objectFactDao.retrieveObjectFacts(notNull())).thenReturn(ListUtils.list(fact).iterator());
    // Mock access to bound Facts.
    when(accessController.hasPermission(credentials, viewGrafeoFact)).thenReturn(result);

    return object;
  }

  private UUID mockCurrentUserIdentities() throws Exception {
    UUID currentUserID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(accessController.getSubjectIdentities(credentials)).thenReturn(Collections.singleton(subject));
    when(identityResolver.resolveSubjectUUID(subject)).thenReturn(currentUserID);
    return currentUserID;
  }

  private Organization mockAvailableOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(accessController.getAvailableOrganizations(credentials)).thenReturn(Collections.singleton(organization));
    when(identityResolver.resolveOrganizationUUID(organization)).thenReturn(organizationID);
    return Organization.builder().setId(organizationID).build();
  }

}
