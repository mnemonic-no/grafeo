package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.entity.cassandra.AccessMode;
import no.mnemonic.act.platform.entity.cassandra.FactAclEntity;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class TiSecurityContextTest {

  @Mock
  private Function<UUID, List<FactAclEntity>> aclResolver;

  private TiSecurityContext context;

  @Before
  public void initialize() {
    initMocks(this);

    // Need to spy on context in order to be able to stub methods of base SecurityContext.
    context = spy(TiSecurityContext.builder().setAclResolver(aclResolver).build());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutAclResolverThrowsException() {
    TiSecurityContext.builder().build();
  }

  @Test
  public void testCheckReadPermissionWithAccessModePublic() throws Exception {
    context.checkReadPermission(new FactEntity().setAccessMode(AccessMode.Public));
    verify(context).checkPermission(TiFunctionConstants.viewFactObjects);
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithAccessModePublicNoAccess() throws Exception {
    doThrow(AccessDeniedException.class).when(context).checkPermission(TiFunctionConstants.viewFactObjects);
    context.checkReadPermission(new FactEntity().setAccessMode(AccessMode.Public));
  }

  @Test
  public void testCheckReadPermissionWithAccessModeRoleBased() throws Exception {
    FactEntity fact = new FactEntity()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased);

    context.checkReadPermission(fact);
    verify(context).checkPermission(TiFunctionConstants.viewFactObjects, fact.getOrganizationID());
  }

  @Test
  public void testCheckReadPermissionWithAccessModeRoleBasedUserInAcl() throws Exception {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased);
    FactAclEntity acl = new FactAclEntity()
            .setSubjectID(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    when(aclResolver.apply(any())).thenReturn(ListUtils.list(acl));

    context.checkReadPermission(fact);
    verify(aclResolver).apply(fact.getId());
    verify(context, never()).checkPermission(TiFunctionConstants.viewFactObjects, fact.getOrganizationID());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithAccessModeRoleBasedNoAccess() throws Exception {
    FactEntity fact = new FactEntity()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(AccessMode.RoleBased);

    doThrow(AccessDeniedException.class).when(context).checkPermission(TiFunctionConstants.viewFactObjects, fact.getOrganizationID());
    context.checkReadPermission(fact);
  }

  @Test
  public void testCheckReadPermissionWithAccessModeExplicit() throws Exception {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit);
    FactAclEntity acl = new FactAclEntity()
            .setSubjectID(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    when(aclResolver.apply(any())).thenReturn(ListUtils.list(acl));

    context.checkReadPermission(fact);
    verify(aclResolver).apply(fact.getId());
    verify(context, never()).checkPermission(TiFunctionConstants.viewFactObjects, fact.getOrganizationID());
  }

  @Test(expected = AccessDeniedException.class)
  public void testCheckReadPermissionWithAccessModeExplicitNoAccess() throws Exception {
    FactEntity fact = new FactEntity()
            .setId(UUID.randomUUID())
            .setAccessMode(AccessMode.Explicit);

    context.checkReadPermission(fact);
    verify(aclResolver).apply(fact.getId());
    verify(context, never()).checkPermission(TiFunctionConstants.viewFactObjects, fact.getOrganizationID());
  }

  @Test
  public void testCheckReadPermissionFallbackToRoleBased() throws Exception {
    FactEntity fact = new FactEntity()
            .setOrganizationID(UUID.randomUUID())
            .setAccessMode(null);

    context.checkReadPermission(fact);
    verify(context).checkPermission(TiFunctionConstants.viewFactObjects, fact.getOrganizationID());
  }

}
