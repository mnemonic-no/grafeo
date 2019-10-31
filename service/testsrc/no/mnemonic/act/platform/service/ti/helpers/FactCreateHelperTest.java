package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
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
    verifyNoInteractions(originManager);
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
    verifyNoInteractions(originManager);
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

  @Test
  public void testResolveAccessModeWithNull() throws Exception {
    assertNull(helper.resolveAccessMode(null, AccessMode.Public));
    assertNull(helper.resolveAccessMode(new FactRecord(), AccessMode.Public));
  }

  @Test
  public void testResolveAccessModeFallsBackToReferencedFact() throws Exception {
    FactRecord referencedFact = new FactRecord().setAccessMode(FactRecord.AccessMode.Public);
    assertEquals(referencedFact.getAccessMode(), helper.resolveAccessMode(referencedFact, null));
  }

  @Test
  public void testResolveAccessModeAllowsEqualOrMoreRestrictive() throws Exception {
    assertEquals(FactRecord.AccessMode.Public, helper.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Public), AccessMode.Public));
    assertEquals(FactRecord.AccessMode.RoleBased, helper.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Public), AccessMode.RoleBased));
    assertEquals(FactRecord.AccessMode.Explicit, helper.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Public), AccessMode.Explicit));

    assertEquals(FactRecord.AccessMode.RoleBased, helper.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.RoleBased), AccessMode.RoleBased));
    assertEquals(FactRecord.AccessMode.Explicit, helper.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.RoleBased), AccessMode.Explicit));

    assertEquals(FactRecord.AccessMode.Explicit, helper.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit), AccessMode.Explicit));
  }

  @Test
  public void testResolveAccessModeDisallowsLessRestrictive() {
    expectInvalidArgumentException(() -> helper.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit), AccessMode.RoleBased));
    expectInvalidArgumentException(() -> helper.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit), AccessMode.Public));

    expectInvalidArgumentException(() -> helper.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.RoleBased), AccessMode.Public));
  }

  @Test
  public void testWithAclNullFact() {
    assertNull(helper.withAcl(null, ListUtils.list(UUID.randomUUID())));
  }

  @Test
  public void testWithAclSkipsEmptyAcl() {
    assertTrue(CollectionUtils.isEmpty(helper.withAcl(new FactRecord(), null).getAcl()));
    assertTrue(CollectionUtils.isEmpty(helper.withAcl(new FactRecord(), ListUtils.list()).getAcl()));
  }

  @Test
  public void testWithAclAddsEntryToEmptyAcl() {
    UUID subjectID = UUID.randomUUID();
    FactRecord fact = new FactRecord().setOriginID(UUID.randomUUID());

    helper.withAcl(fact, ListUtils.list(subjectID));
    assertEquals(1, fact.getAcl().size());
    assertNotNull(fact.getAcl().get(0).getId());
    assertEquals(fact.getOriginID(), fact.getAcl().get(0).getOriginID());
    assertEquals(subjectID, fact.getAcl().get(0).getSubjectID());
    assertTrue(fact.getAcl().get(0).getTimestamp() > 0);
  }

  @Test
  public void testWithAclAddsEntryToExistingAcl() {
    UUID subjectID = UUID.randomUUID();
    FactRecord fact = new FactRecord().addAclEntry(new FactAclEntryRecord().setSubjectID(UUID.randomUUID()));

    helper.withAcl(fact, ListUtils.list(subjectID));
    assertEquals(2, fact.getAcl().size());
  }

  @Test
  public void testWithAclSkipsExistingEntry() {
    UUID subjectID = UUID.randomUUID();
    FactRecord fact = new FactRecord().addAclEntry(new FactAclEntryRecord().setSubjectID(subjectID));

    helper.withAcl(fact, ListUtils.list(subjectID));
    assertEquals(1, fact.getAcl().size());
  }

  @Test
  public void testWithAclAccessModeExplicitAddsCurrentUser() {
    UUID currentUser = UUID.randomUUID();
    FactRecord fact = new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit);
    when(securityContext.getCurrentUserID()).thenReturn(currentUser);

    helper.withAcl(fact, null);
    assertEquals(1, fact.getAcl().size());
    assertEquals(currentUser, fact.getAcl().get(0).getSubjectID());
  }

  @Test
  public void testWithAclAccessModeExplicitSkipsExistingCurrentUser() {
    UUID currentUser = UUID.randomUUID();
    FactRecord fact = new FactRecord()
            .setAccessMode(FactRecord.AccessMode.Explicit)
            .addAclEntry(new FactAclEntryRecord().setSubjectID(currentUser));
    when(securityContext.getCurrentUserID()).thenReturn(currentUser);

    helper.withAcl(fact, null);
    assertEquals(1, fact.getAcl().size());
    assertEquals(currentUser, fact.getAcl().get(0).getSubjectID());
  }

  @Test
  public void testWithCommentNullFact() {
    assertNull(helper.withComment(null, "Hello World!"));
  }

  @Test
  public void testWithCommentSkipsBlankComment() {
    assertTrue(CollectionUtils.isEmpty(helper.withComment(new FactRecord(), null).getComments()));
    assertTrue(CollectionUtils.isEmpty(helper.withComment(new FactRecord(), "").getComments()));
    assertTrue(CollectionUtils.isEmpty(helper.withComment(new FactRecord(), " ").getComments()));
  }

  @Test
  public void testWithCommentAddsAdditionalComment() {
    FactRecord fact = new FactRecord()
            .setOriginID(UUID.randomUUID());

    helper.withComment(fact, "Hello World!");
    assertEquals(1, fact.getComments().size());
    assertNotNull(fact.getComments().get(0).getId());
    assertEquals(fact.getOriginID(), fact.getComments().get(0).getOriginID());
    assertEquals("Hello World!", fact.getComments().get(0).getComment());
    assertTrue(fact.getComments().get(0).getTimestamp() > 0);
  }

  private void expectInvalidArgumentException(InvalidArgumentExceptionTest test) {
    try {
      test.execute();
      fail("Expected InvalidArgumentException!");
    } catch (InvalidArgumentException ignored) {
    }
  }

  private interface InvalidArgumentExceptionTest {
    void execute() throws InvalidArgumentException;
  }
}
