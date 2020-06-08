package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import no.mnemonic.services.common.auth.model.SessionDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static no.mnemonic.act.platform.service.ti.TiFunctionConstants.viewThreatIntelFact;
import static no.mnemonic.act.platform.service.ti.TiFunctionConstants.viewThreatIntelOrigin;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings("unchecked")
public class TiSecurityContextTest {

  @Mock
  private AccessController accessController;
  @Mock
  private IdentityResolver identityResolver;
  @Mock
  private Credentials credentials;
  @Mock
  private SessionDescriptor sessionDescriptor;
  @Mock
  private OrganizationIdentity organization;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private Function<UUID, List<FactAclEntity>> aclResolver;

  private TiSecurityContext context;

  @Before
  public void initialize() {
    initMocks(this);
    when(identityResolver.resolveOrganizationIdentity(any())).thenReturn(organization);

    context = TiSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setCredentials(credentials)
            .setObjectFactDao(objectFactDao)
            .setAclResolver(aclResolver)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutObjectFactDaoThrowsException() {
    TiSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setCredentials(credentials)
            .setAclResolver(aclResolver)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutAclResolverThrowsException() {
    TiSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setCredentials(credentials)
            .setObjectFactDao(objectFactDao)
            .build();
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithoutFact() throws Exception {
    context.checkReadPermission((FactEntity) null);
  }

  @Test
  public void testCheckReadPermissionWithAccessModePublic() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(true);
    context.checkReadPermission(new FactEntity().setAccessMode(AccessMode.Public));
    verify(accessController).hasPermission(credentials, viewThreatIntelFact);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithAccessModePublicNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(false);
    context.checkReadPermission(new FactEntity().setAccessMode(AccessMode.Public));
  }

  @Test
  public void testCheckReadPermissionWithAccessModeRoleBased() throws Exception {
    FactEntity fact = new FactEntity()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased);

    when(accessController.hasPermission(credentials, viewThreatIntelFact, organization)).thenReturn(true);
    context.checkReadPermission(fact);
    verify(accessController).hasPermission(credentials, viewThreatIntelFact, organization);
  }

  @Test
  public void testCheckReadPermissionWithAccessModeRoleBasedUserInAcl() throws Exception {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased);
    UUID currentUserID = mockCurrentUser();
    when(aclResolver.apply(any())).thenReturn(ListUtils.list(new FactAclEntity().setSubjectID(currentUserID)));

    context.checkReadPermission(fact);
    verify(aclResolver).apply(fact.getId());
    verify(accessController, never()).hasPermission(credentials, viewThreatIntelFact, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithAccessModeRoleBasedNoAccess() throws Exception {
    FactEntity fact = new FactEntity()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased);

    when(accessController.hasPermission(credentials, viewThreatIntelFact, organization)).thenReturn(false);
    context.checkReadPermission(fact);
  }

  @Test
  public void testCheckReadPermissionWithAccessModeExplicit() throws Exception {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit);
    UUID currentUserID = mockCurrentUser();
    when(aclResolver.apply(any())).thenReturn(ListUtils.list(new FactAclEntity().setSubjectID(currentUserID)));

    context.checkReadPermission(fact);
    verify(aclResolver).apply(fact.getId());
    verify(accessController, never()).hasPermission(credentials, viewThreatIntelFact, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithAccessModeExplicitNoAccess() throws Exception {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit);

    context.checkReadPermission(fact);
    verify(aclResolver).apply(fact.getId());
    verify(accessController, never()).hasPermission(credentials, viewThreatIntelFact, organization);
  }

  @Test
  public void testCheckReadPermissionFallbackToRoleBased() throws Exception {
    FactEntity fact = new FactEntity()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(null);

    when(accessController.hasPermission(credentials, viewThreatIntelFact, organization)).thenReturn(true);
    context.checkReadPermission(fact);
    verify(accessController).hasPermission(credentials, viewThreatIntelFact, organization);
  }

  @Test
  public void testHasReadPermissionReturnsTrueOnAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(true);
    assertTrue(context.hasReadPermission(new FactEntity().setAccessMode(AccessMode.Public)));
  }

  @Test
  public void testHasReadPermissionReturnsFalseOnNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(false);
    assertFalse(context.hasReadPermission(new FactEntity().setAccessMode(AccessMode.Public)));
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForFactRecordWithoutFact() throws Exception {
    context.checkReadPermission((FactRecord) null);
  }

  @Test
  public void testCheckReadPermissionForFactRecordWithAccessModePublic() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(true);
    context.checkReadPermission(new FactRecord().setAccessMode(FactRecord.AccessMode.Public));
    verify(accessController).hasPermission(credentials, viewThreatIntelFact);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForFactRecordWithAccessModePublicNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(false);
    context.checkReadPermission(new FactRecord().setAccessMode(FactRecord.AccessMode.Public));
  }

  @Test
  public void testCheckReadPermissionForFactRecordWithAccessModeRoleBased() throws Exception {
    FactRecord fact = new FactRecord()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased);

    when(accessController.hasPermission(credentials, viewThreatIntelFact, organization)).thenReturn(true);
    context.checkReadPermission(fact);
    verify(accessController).hasPermission(credentials, viewThreatIntelFact, organization);
  }

  @Test
  public void testCheckReadPermissionForFactRecordWithAccessModeRoleBasedUserInAcl() throws Exception {
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased)
            .addAclEntry(new FactAclEntryRecord().setSubjectID(mockCurrentUser()));

    context.checkReadPermission(fact);
    verify(accessController, never()).hasPermission(credentials, viewThreatIntelFact, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForFactRecordWithAccessModeRoleBasedNoAccess() throws Exception {
    FactRecord fact = new FactRecord()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased);

    when(accessController.hasPermission(credentials, viewThreatIntelFact, organization)).thenReturn(false);
    context.checkReadPermission(fact);
  }

  @Test
  public void testCheckReadPermissionForFactRecordWithAccessModeExplicit() throws Exception {
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Explicit)
            .addAclEntry(new FactAclEntryRecord().setSubjectID(mockCurrentUser()));

    context.checkReadPermission(fact);
    verify(accessController, never()).hasPermission(credentials, viewThreatIntelFact, organization);
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

    when(accessController.hasPermission(credentials, viewThreatIntelFact, organization)).thenReturn(true);
    context.checkReadPermission(fact);
    verify(accessController).hasPermission(credentials, viewThreatIntelFact, organization);
  }

  @Test
  public void testHasReadPermissionForFactRecordReturnsTrueOnAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(true);
    assertTrue(context.hasReadPermission(new FactRecord().setAccessMode(FactRecord.AccessMode.Public)));
  }

  @Test
  public void testHasReadPermissionForFactRecordReturnsFalseOnNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(false);
    assertFalse(context.hasReadPermission(new FactRecord().setAccessMode(FactRecord.AccessMode.Public)));
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForObjectRecordWithoutObject() throws Exception {
    context.checkReadPermission((ObjectRecord) null);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForObjectRecordWithoutBoundFact() throws Exception {
    mockCurrentUser();
    mockAvailableOrganization();
    when(objectFactDao.searchFacts(notNull())).thenReturn(ResultContainer.<FactRecord>builder().build());

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
    verify(accessController).hasPermission(credentials, viewThreatIntelFact);
  }

  @Test
  public void testCheckReadPermissionForObjectRecordWithAccessToSecondFact() throws Exception {
    FactRecord fact = new FactRecord().setAccessMode(FactRecord.AccessMode.Public);

    mockCurrentUser();
    mockAvailableOrganization();
    when(objectFactDao.searchFacts(notNull()))
            .thenReturn(ResultContainer.<FactRecord>builder().setValues(ListUtils.list(fact, fact, fact).iterator()).build());
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(false, true, false);

    context.checkReadPermission(new ObjectRecord().setId(UUID.randomUUID()));
    verify(accessController, times(2)).hasPermission(credentials, viewThreatIntelFact);
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
    when(accessController.hasPermission(credentials, viewThreatIntelOrigin, organization)).thenReturn(false);
    context.checkReadPermission(new OriginEntity().setOrganizationID(UUID.randomUUID()));
  }

  @Test
  public void testCheckReadPermissionForOriginWithOrganizationAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelOrigin, organization)).thenReturn(true);
    context.checkReadPermission(new OriginEntity().setOrganizationID(UUID.randomUUID()));
    verify(accessController).hasPermission(credentials, viewThreatIntelOrigin, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForOriginWithoutOrganizationNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelOrigin)).thenReturn(false);
    context.checkReadPermission(new OriginEntity());
  }

  @Test
  public void testCheckReadPermissionForOriginWithoutOrganizationAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelOrigin)).thenReturn(true);
    context.checkReadPermission(new OriginEntity());
    verify(accessController).hasPermission(credentials, viewThreatIntelOrigin);
  }

  @Test
  public void testHasReadPermissionForOriginReturnsTrueOnAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelOrigin)).thenReturn(true);
    assertTrue(context.hasReadPermission(new OriginEntity()));
  }

  @Test
  public void testHasReadPermissionForOriginReturnsFalseOnNoAccess() throws Exception {
    when(accessController.hasPermission(credentials, viewThreatIntelOrigin)).thenReturn(false);
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

    // Mock search for bound Facts.
    mockCurrentUser();
    mockAvailableOrganization();
    when(objectFactDao.searchFacts(notNull()))
            .thenReturn(ResultContainer.<FactRecord>builder().setValues(ListUtils.list(fact).iterator()).build());
    // Mock access to bound Facts.
    when(accessController.hasPermission(credentials, viewThreatIntelFact)).thenReturn(result);

    return object;
  }

  private UUID mockCurrentUser() throws Exception {
    UUID currentUserID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(accessController.validate(credentials)).thenReturn(sessionDescriptor);
    when(identityResolver.resolveSubjectUUID(sessionDescriptor)).thenReturn(currentUserID);
    return currentUserID;
  }

  private Organization mockAvailableOrganization() throws Exception {
    UUID organizationID = UUID.randomUUID();
    when(accessController.getAvailableOrganizations(credentials)).thenReturn(Collections.singleton(organization));
    when(identityResolver.resolveOrganizationUUID(organization)).thenReturn(organizationID);
    return Organization.builder().setId(organizationID).build();
  }

}
