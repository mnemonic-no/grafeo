package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static no.mnemonic.act.platform.service.ti.TiFunctionConstants.viewFactObjects;
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
  private OrganizationResolver organizationResolver;
  @Mock
  private SubjectResolver subjectResolver;
  @Mock
  private Credentials credentials;
  @Mock
  private OrganizationIdentity organization;
  @Mock
  private Function<UUID, List<FactAclEntity>> aclResolver;
  @Mock
  private Function<UUID, Iterator<FactEntity>> factsBoundToObjectResolver;

  private TiSecurityContext context;

  @Before
  public void initialize() {
    initMocks(this);

    context = TiSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setOrganizationResolver(organizationResolver)
            .setSubjectResolver(subjectResolver)
            .setCredentials(credentials)
            .setAclResolver(aclResolver)
            .setFactsBoundToObjectResolver(factsBoundToObjectResolver)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutAclResolverThrowsException() {
    TiSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setOrganizationResolver(organizationResolver)
            .setSubjectResolver(subjectResolver)
            .setCredentials(credentials)
            .setFactsBoundToObjectResolver(factsBoundToObjectResolver)
            .build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutFactsBoundToObjectResolverThrowsException() {
    TiSecurityContext.builder()
            .setAccessController(accessController)
            .setIdentityResolver(identityResolver)
            .setOrganizationResolver(organizationResolver)
            .setSubjectResolver(subjectResolver)
            .setCredentials(credentials)
            .setAclResolver(aclResolver)
            .build();
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithoutFact() throws Exception {
    context.checkReadPermission((FactEntity) null);
  }

  @Test
  public void testCheckReadPermissionWithAccessModePublic() throws Exception {
    mockHasPermission(true);
    context.checkReadPermission(new FactEntity().setAccessMode(AccessMode.Public));
    verify(accessController).hasPermission(credentials, viewFactObjects);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithAccessModePublicNoAccess() throws Exception {
    mockHasPermission(false);
    context.checkReadPermission(new FactEntity().setAccessMode(AccessMode.Public));
  }

  @Test
  public void testCheckReadPermissionWithAccessModeRoleBased() throws Exception {
    FactEntity fact = new FactEntity()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased);

    mockHasPermission(fact.getOrganizationID(), true);
    context.checkReadPermission(fact);
    verify(accessController).hasPermission(credentials, viewFactObjects, organization);
  }

  @Test
  public void testCheckReadPermissionWithAccessModeRoleBasedUserInAcl() throws Exception {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased);
    UUID currentUserID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(subjectResolver.resolveCurrentUser(any())).thenReturn(Subject.builder().setId(currentUserID).build());
    when(aclResolver.apply(any())).thenReturn(ListUtils.list(new FactAclEntity().setSubjectID(currentUserID)));

    context.checkReadPermission(fact);
    verify(aclResolver).apply(fact.getId());
    verify(accessController, never()).hasPermission(credentials, viewFactObjects, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithAccessModeRoleBasedNoAccess() throws Exception {
    FactEntity fact = new FactEntity()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased);

    mockHasPermission(fact.getOrganizationID(), false);
    context.checkReadPermission(fact);
  }

  @Test
  public void testCheckReadPermissionWithAccessModeExplicit() throws Exception {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit);
    UUID currentUserID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(subjectResolver.resolveCurrentUser(any())).thenReturn(Subject.builder().setId(currentUserID).build());
    when(aclResolver.apply(any())).thenReturn(ListUtils.list(new FactAclEntity().setSubjectID(currentUserID)));

    context.checkReadPermission(fact);
    verify(aclResolver).apply(fact.getId());
    verify(accessController, never()).hasPermission(credentials, viewFactObjects, organization);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithAccessModeExplicitNoAccess() throws Exception {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit);

    context.checkReadPermission(fact);
    verify(aclResolver).apply(fact.getId());
    verify(accessController, never()).hasPermission(credentials, viewFactObjects, organization);
  }

  @Test
  public void testCheckReadPermissionFallbackToRoleBased() throws Exception {
    FactEntity fact = new FactEntity()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(null);

    mockHasPermission(fact.getOrganizationID(), true);
    context.checkReadPermission(fact);
    verify(accessController).hasPermission(credentials, viewFactObjects, organization);
  }

  @Test
  public void testHasReadPermissionReturnsTrueOnAccess() throws Exception {
    mockHasPermission(true);
    assertTrue(context.hasReadPermission(new FactEntity().setAccessMode(AccessMode.Public)));
  }

  @Test
  public void testHasReadPermissionReturnsFalseOnNoAccess() throws Exception {
    mockHasPermission(false);
    assertFalse(context.hasReadPermission(new FactEntity().setAccessMode(AccessMode.Public)));
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForObjectWithoutObject() throws Exception {
    context.checkReadPermission((ObjectEntity) null);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForObjectWithoutBoundFact() throws Exception {
    ObjectEntity object = new ObjectEntity().setId(UUID.randomUUID());
    when(factsBoundToObjectResolver.apply(object.getId())).thenReturn(Collections.emptyIterator());
    context.checkReadPermission(object);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionForObjectWithoutAccessToFact() throws Exception {
    ObjectEntity object = mockCheckPermissionForObject(false);
    context.checkReadPermission(object);
  }

  @Test
  public void testCheckReadPermissionForObjectWithAccessToFact() throws Exception {
    ObjectEntity object = mockCheckPermissionForObject(true);
    context.checkReadPermission(object);
    verify(accessController).hasPermission(credentials, viewFactObjects);
  }

  @Test
  public void testCheckReadPermissionForObjectWithAccessToSecondFact() throws Exception {
    ObjectEntity object = new ObjectEntity().setId(UUID.randomUUID());
    FactEntity fact = new FactEntity().setAccessMode(AccessMode.Public);
    when(factsBoundToObjectResolver.apply(object.getId())).thenReturn(ListUtils.list(fact, fact, fact).iterator());
    when(accessController.hasPermission(credentials, viewFactObjects)).thenReturn(false, true, false);
    context.checkReadPermission(object);
    verify(accessController, times(2)).hasPermission(credentials, viewFactObjects);
  }

  @Test
  public void testHasReadPermissionForObjectReturnsTrueOnAccess() throws Exception {
    ObjectEntity object = mockCheckPermissionForObject(true);
    assertTrue(context.hasReadPermission(object));
  }

  @Test
  public void testHasReadPermissionForObjectReturnsFalseOnNoAccess() throws Exception {
    ObjectEntity object = mockCheckPermissionForObject(false);
    assertFalse(context.hasReadPermission(object));
  }

  private ObjectEntity mockCheckPermissionForObject(boolean result) throws Exception {
    ObjectEntity object = new ObjectEntity().setId(UUID.randomUUID());
    FactEntity fact = new FactEntity().setAccessMode(AccessMode.Public);
    when(factsBoundToObjectResolver.apply(object.getId())).thenReturn(ListUtils.list(fact).iterator());
    mockHasPermission(result); // Mock access to public Fact.
    return object;
  }

  private void mockHasPermission(boolean result) throws Exception {
    when(accessController.hasPermission(credentials, viewFactObjects)).thenReturn(result);
  }

  private void mockHasPermission(UUID organizationID, boolean result) throws Exception {
    when(identityResolver.resolveOrganizationIdentity(organizationID)).thenReturn(organization);
    when(accessController.hasPermission(credentials, viewFactObjects, organization)).thenReturn(result);
  }

}
