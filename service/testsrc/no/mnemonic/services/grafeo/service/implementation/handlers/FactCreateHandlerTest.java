package no.mnemonic.services.grafeo.service.implementation.handlers;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.api.request.v1.AccessMode;
import no.mnemonic.services.grafeo.auth.OrganizationSPI;
import no.mnemonic.services.grafeo.auth.SubjectSPI;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactCommentRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.grafeo.dao.facade.helpers.FactRecordHasher;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.FactResponseConverter;
import no.mnemonic.services.grafeo.service.providers.LockProvider;
import no.mnemonic.services.grafeo.service.validators.Validator;
import no.mnemonic.services.grafeo.service.validators.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactCreateHandlerTest {

  @Mock
  private GrafeoSecurityContext securityContext;
  @Mock
  private SubjectSPI subjectResolver;
  @Mock
  private OrganizationSPI organizationResolver;
  @Mock
  private OriginManager originManager;
  @Mock
  private ValidatorFactory validatorFactory;
  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactResponseConverter factResponseConverter;
  @Mock
  private LockProvider lockProvider;
  @Mock
  private Credentials credentials;
  @Mock
  private Clock clock;

  private FactCreateHandler handler;

  @Before
  public void setUp() {
    initMocks(this);
    when(securityContext.getCredentials()).thenReturn(credentials);
    handler = new FactCreateHandler(
            securityContext,
            subjectResolver,
            organizationResolver,
            originManager,
            validatorFactory,
            objectFactDao,
            factResponseConverter,
            lockProvider
    ).withClock(clock);
  }

  @Test
  public void testResolveOrganizationProvidedOrganizationNotExists() throws Exception {
    assertThrows(InvalidArgumentException.class, () -> handler.resolveOrganization("organization", null));
    verify(organizationResolver).resolveOrganization(credentials, "organization");
  }

  @Test
  public void testResolveOrganizationProvidedOrganizationWithInvalidCredentials() throws Exception {
    when(organizationResolver.resolveOrganization(notNull(), isA(String.class))).thenThrow(InvalidCredentialsException.class);
    assertThrows(AuthenticationFailedException.class, () -> handler.resolveOrganization("organization", null));
    verify(organizationResolver).resolveOrganization(credentials, "organization");
  }

  @Test
  public void testResolveOrganizationProvidedFetchesExistingOrganizationById() throws Exception {
    UUID organizationID = UUID.randomUUID();
    Organization organization = Organization.builder().build();
    when(organizationResolver.resolveOrganization(notNull(), eq(organizationID))).thenReturn(organization);

    assertSame(organization, handler.resolveOrganization(organizationID.toString(), null));
    verify(organizationResolver).resolveOrganization(notNull(), eq(organizationID));
    verify(securityContext).checkReadPermission(organization);
  }

  @Test
  public void testResolveOrganizationProvidedFetchesExistingOrganizationByName() throws Exception {
    Organization organization = Organization.builder().build();
    when(organizationResolver.resolveOrganization(notNull(), eq("organization"))).thenReturn(organization);

    assertSame(organization, handler.resolveOrganization("organization", null));
    verify(organizationResolver).resolveOrganization(notNull(), eq("organization"));
    verify(securityContext).checkReadPermission(organization);
  }

  @Test
  public void testResolveOrganizationFallbackToOriginOrganizationNotExists() throws Exception {
    UUID organizationID = UUID.randomUUID();
    assertThrows(InvalidArgumentException.class, () -> handler.resolveOrganization(null, new OriginEntity().setOrganizationID(organizationID)));
    verify(organizationResolver).resolveOrganization(credentials, organizationID);
  }

  @Test
  public void testResolveOrganizationFallbackToOriginFetchesExistingOrganization() throws Exception {
    OriginEntity origin = new OriginEntity().setOrganizationID(UUID.randomUUID());
    Organization organization = Organization.builder().build();
    when(organizationResolver.resolveOrganization(notNull(), eq(origin.getOrganizationID()))).thenReturn(organization);

    assertSame(organization, handler.resolveOrganization(null, origin));
    verify(organizationResolver).resolveOrganization(notNull(), eq(origin.getOrganizationID()));
  }

  @Test
  public void testResolveOrganizationFallbackToCurrentUserOrganizationInvalidCredentials() throws Exception {
    when(organizationResolver.resolveCurrentUserAffiliation(any())).thenThrow(InvalidCredentialsException.class);
    assertThrows(AuthenticationFailedException.class, () -> handler.resolveOrganization(null, new OriginEntity()));
    verify(organizationResolver).resolveCurrentUserAffiliation(credentials);
  }

  @Test
  public void testResolveOrganizationFallbackToCurrentUserOrganizationNotExists() throws Exception {
    assertThrows(InvalidArgumentException.class, () -> handler.resolveOrganization(null, new OriginEntity()));
    verify(organizationResolver).resolveCurrentUserAffiliation(credentials);
  }

  @Test
  public void testResolveOrganizationFallbackToCurrentUserFetchesExistingOrganization() throws Exception {
    Organization organization = Organization.builder().build();
    when(organizationResolver.resolveCurrentUserAffiliation(any())).thenReturn(organization);

    assertSame(organization, handler.resolveOrganization(null, new OriginEntity()));
    verify(organizationResolver).resolveCurrentUserAffiliation(credentials);
  }

  @Test
  public void testResolveOriginProvidedOriginNotExists() {
    assertThrows(InvalidArgumentException.class, () -> handler.resolveOrigin("origin"));
    verify(originManager).getOrigin("origin");
  }

  @Test
  public void testResolveOriginProvidedFetchesExistingOriginById() throws Exception {
    UUID originID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity().setId(originID);
    when(originManager.getOrigin(originID)).thenReturn(origin);

    assertSame(origin, handler.resolveOrigin(originID.toString()));
    verify(originManager).getOrigin(originID);
    verify(securityContext).checkReadPermission(origin);
    verifyNoMoreInteractions(originManager);
  }

  @Test
  public void testResolveOriginProvidedFetchesExistingOriginByName() throws Exception {
    OriginEntity origin = new OriginEntity();
    when(originManager.getOrigin("origin")).thenReturn(origin);

    assertSame(origin, handler.resolveOrigin("origin"));
    verify(originManager).getOrigin("origin");
    verify(securityContext).checkReadPermission(origin);
    verifyNoMoreInteractions(originManager);
  }

  @Test
  public void testResolveOriginProvidedFetchesDeletedOrigin() {
    UUID originID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity()
            .setId(originID)
            .addFlag(OriginEntity.Flag.Deleted);
    when(originManager.getOrigin(originID)).thenReturn(origin);

    assertThrows(InvalidArgumentException.class, () -> handler.resolveOrigin(originID.toString()));
  }

  @Test
  public void testResolveOriginNonProvidedFetchesExistingOrigin() throws Exception {
    UUID currentUserID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity().setId(currentUserID);
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(originManager.getOrigin(currentUserID)).thenReturn(origin);

    assertSame(origin, handler.resolveOrigin(null));
    verify(originManager).getOrigin(currentUserID);
    verifyNoMoreInteractions(originManager);
  }

  @Test
  public void testResolveOriginNonProvidedFetchesDeletedOrigin() {
    UUID currentUserID = UUID.randomUUID();
    OriginEntity origin = new OriginEntity()
            .setId(currentUserID)
            .addFlag(OriginEntity.Flag.Deleted);
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(originManager.getOrigin(currentUserID)).thenReturn(origin);

    assertThrows(InvalidArgumentException.class, () -> handler.resolveOrigin(null));
  }

  @Test
  public void testResolveOriginNonProvidedFailsOnCurrentUser() throws Exception {
    when(securityContext.getCurrentUserID()).thenReturn(UUID.randomUUID());
    when(subjectResolver.resolveCurrentUser(any())).thenThrow(InvalidCredentialsException.class);
    assertThrows(AuthenticationFailedException.class, () -> handler.resolveOrigin(null));
    verify(subjectResolver).resolveCurrentUser(credentials);
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
    when(subjectResolver.resolveCurrentUser(any())).thenReturn(currentUser);
    when(originManager.saveOrigin(notNull())).thenReturn(newOrigin);

    assertSame(newOrigin, handler.resolveOrigin(null));
    verify(originManager).getOrigin(currentUserID);
    verify(subjectResolver).resolveCurrentUser(credentials);
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
    when(subjectResolver.resolveCurrentUser(any())).thenReturn(currentUser);
    when(originManager.getOrigin(currentUser.getName())).thenReturn(new OriginEntity()
            .setId(UUID.randomUUID())
            .setName(currentUser.getName()));

    handler.resolveOrigin(null);
    verify(originManager).getOrigin(currentUser.getName());
    verify(originManager).saveOrigin(argThat(entity -> {
      assertEquals(currentUserID, entity.getId());
      assertNotEquals(currentUser.getName(), entity.getName());
      assertTrue(entity.getName().startsWith(currentUser.getName()));
      return true;
    }));
  }

  @Test
  public void testResolveSubjectsWithoutAcl() throws Exception {
    assertNotNull(handler.resolveSubjects(null));
    assertNotNull(handler.resolveSubjects(new ArrayList<>()));
  }

  @Test
  public void testResolveSubjectsByIdOrName() throws Exception {
    when(subjectResolver.resolveSubject(notNull(), isA(UUID.class))).thenReturn(Subject.builder().build());
    when(subjectResolver.resolveSubject(notNull(), isA(String.class))).thenReturn(Subject.builder().build());

    List<String> acl = list(UUID.randomUUID().toString(), "name", UUID.randomUUID().toString());
    assertEquals(acl.size(), handler.resolveSubjects(acl).size());
    verify(subjectResolver, times(2)).resolveSubject(notNull(), isA(UUID.class));
    verify(subjectResolver).resolveSubject(notNull(), eq("name"));
  }

  @Test
  public void testResolveSubjectsFailsOnUnresolved() throws Exception {
    when(subjectResolver.resolveSubject(notNull(), isA(String.class))).thenReturn(Subject.builder().build());

    List<String> acl = list(UUID.randomUUID().toString(), "name", UUID.randomUUID().toString());
    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> handler.resolveSubjects(acl));
    assertEquals(set("acl[0]", "acl[2]"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getProperty));
    verify(subjectResolver, times(2)).resolveSubject(notNull(), isA(UUID.class));
    verify(subjectResolver).resolveSubject(notNull(), eq("name"));
  }

  @Test
  public void testResolveSubjectsFailsOnInvalidCredentials() throws Exception {
    when(subjectResolver.resolveSubject(notNull(), isA(String.class))).thenThrow(InvalidCredentialsException.class);
    assertThrows(AuthenticationFailedException.class, () -> handler.resolveSubjects(list("name")));
    verify(subjectResolver).resolveSubject(notNull(), eq("name"));
  }

  @Test
  public void testResolveAccessModeWithNull() throws Exception {
    assertNull(handler.resolveAccessMode(null, AccessMode.Public));
    assertNull(handler.resolveAccessMode(new FactRecord(), AccessMode.Public));
  }

  @Test
  public void testResolveAccessModeFallsBackToReferencedFact() throws Exception {
    FactRecord referencedFact = new FactRecord().setAccessMode(FactRecord.AccessMode.Public);
    assertEquals(referencedFact.getAccessMode(), handler.resolveAccessMode(referencedFact, null));
  }

  @Test
  public void testResolveAccessModeAllowsEqualOrMoreRestrictive() throws Exception {
    assertEquals(FactRecord.AccessMode.Public, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Public), AccessMode.Public));
    assertEquals(FactRecord.AccessMode.RoleBased, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Public), AccessMode.RoleBased));
    assertEquals(FactRecord.AccessMode.Explicit, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Public), AccessMode.Explicit));

    assertEquals(FactRecord.AccessMode.RoleBased, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.RoleBased), AccessMode.RoleBased));
    assertEquals(FactRecord.AccessMode.Explicit, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.RoleBased), AccessMode.Explicit));

    assertEquals(FactRecord.AccessMode.Explicit, handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit), AccessMode.Explicit));
  }

  @Test
  public void testResolveAccessModeDisallowsLessRestrictive() {
    assertThrows(InvalidArgumentException.class, () -> handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit), AccessMode.RoleBased));
    assertThrows(InvalidArgumentException.class, () -> handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.Explicit), AccessMode.Public));
    assertThrows(InvalidArgumentException.class, () -> handler.resolveAccessMode(new FactRecord().setAccessMode(FactRecord.AccessMode.RoleBased), AccessMode.Public));
  }

  @Test
  public void testAssertValidFactValue() {
    FactTypeEntity factType = new FactTypeEntity().setValidator("SomeValidator").setValidatorParameter("SomeParam");
    Validator validator = mock(Validator.class);
    when(validator.validate(eq("test"))).thenReturn(false);
    when(validatorFactory.get(factType.getValidator(), factType.getValidatorParameter())).thenReturn(validator);

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class,
            () -> handler.assertValidFactValue(factType, "test"));
    assertEquals(SetUtils.set("fact.not.valid"), SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testSaveNewFact() {
    FactRecord factToSave = new FactRecord();
    when(objectFactDao.retrieveExistingFact(factToSave)).thenReturn(Optional.empty());
    when(objectFactDao.storeFact(factToSave)).thenReturn(factToSave);

    List<UUID> subjectIds = list(UUID.randomUUID());
    handler.saveFact(factToSave, "some comment", subjectIds);

    verify(objectFactDao).storeFact(argThat(fact -> {
      assertEquals(factToSave, fact);
      assertEquals(set("some comment"), set(fact.getComments(), FactCommentRecord::getComment));
      assertEquals(set(subjectIds), set(fact.getAcl(), FactAclEntryRecord::getSubjectID));
      return true;
    }));

    verify(objectFactDao, never()).refreshFact(any());
    verify(factResponseConverter).apply(factToSave);
    verify(lockProvider).acquireLock("FactCreateHandler", FactRecordHasher.toHash(factToSave));
  }

  @Test
  public void testRefreshExistingFact() {
    long lastSeenTimestamp = 123456789L;
    UUID currentUserID = UUID.randomUUID();
    FactRecord factToSave = new FactRecord();

    // Mock fetching of existing Fact.
    FactRecord existingFact = new FactRecord();
    when(objectFactDao.retrieveExistingFact(factToSave)).thenReturn(Optional.of(existingFact));

    // Mock stuff needed for refreshing Fact.
    when(securityContext.getCurrentUserID()).thenReturn(currentUserID);
    when(clock.millis()).thenReturn(lastSeenTimestamp);
    when(objectFactDao.refreshFact(existingFact)).thenReturn(existingFact);

    List<UUID> subjectIds = list(UUID.randomUUID());
    handler.saveFact(factToSave, "some comment", subjectIds);

    verify(objectFactDao).refreshFact(argThat(fact -> {
      assertEquals(existingFact, fact);
      assertEquals(lastSeenTimestamp, fact.getLastSeenTimestamp());
      assertEquals(currentUserID, fact.getLastSeenByID());
      assertEquals(set("some comment"), set(fact.getComments(), FactCommentRecord::getComment));
      assertEquals(set(subjectIds), set(fact.getAcl(), FactAclEntryRecord::getSubjectID));
      return true;
    }));

    verify(objectFactDao, never()).storeFact(any());
    verify(objectFactDao).retrieveExistingFact(factToSave);
    verify(factResponseConverter).apply(existingFact);
  }
}
