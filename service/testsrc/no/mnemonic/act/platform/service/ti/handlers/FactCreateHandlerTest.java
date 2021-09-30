package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.auth.OrganizationSPI;
import no.mnemonic.act.platform.auth.SubjectSPI;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactCommentRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.result.ResultContainer;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.service.contexts.TriggerContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.TiServiceEvent;
import no.mnemonic.act.platform.service.ti.converters.response.FactResponseConverter;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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
  private TiSecurityContext securityContext;
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
  private TriggerContext triggerContext;
  @Mock
  private Credentials credentials;

  private FactCreateHandler handler;

  @Before
  public void setUp() {
    initMocks(this);
    when(securityContext.getCredentials()).thenReturn(credentials);
    handler = new FactCreateHandler(securityContext, subjectResolver, organizationResolver, originManager, validatorFactory, objectFactDao, factResponseConverter, triggerContext);
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
    FactRecord factToSave = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased)
            .setOrganizationID(UUID.randomUUID());
    mockSaveFact();

    List<UUID> subjectIds = list(UUID.randomUUID());
    handler.saveFact(factToSave, "some comment", subjectIds);

    verify(objectFactDao).storeFact(argThat(fact -> {
      assertEquals(factToSave, fact);
      assertEquals(set("some comment"), set(fact.getComments(), FactCommentRecord::getComment));
      assertEquals(set(subjectIds), set(fact.getAcl(), FactAclEntryRecord::getSubjectID));
      return true;
    }));

    verify(objectFactDao, never()).refreshFact(any());
    verify(factResponseConverter).apply(same(factToSave));
  }

  @Test
  public void testRefreshExistingFactWithElasticsearch() {
    FactRecord factToSave = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setOrganizationID(UUID.randomUUID());
    mockSaveFact();

    // Mock fetching of existing Fact.
    FactRecord existingFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased)
            .setOrganizationID(UUID.randomUUID());
    when(objectFactDao.retrieveExistingFacts(factToSave))
            .thenReturn(ResultContainer.<FactRecord>builder().setValues(list(existingFact).iterator()).build());
    when(securityContext.hasReadPermission(existingFact)).thenReturn(true);

    // Mock stuff needed for refreshing Fact.
    when(objectFactDao.refreshFact(existingFact)).thenReturn(existingFact);

    List<UUID> subjectIds = list(UUID.randomUUID());
    handler.saveFact(factToSave, "some comment", subjectIds);

    verify(objectFactDao).refreshFact(argThat(fact -> {
      assertEquals(existingFact, fact);
      assertEquals(set("some comment"), set(fact.getComments(), FactCommentRecord::getComment));
      assertEquals(set(subjectIds), set(fact.getAcl(), FactAclEntryRecord::getSubjectID));
      return true;
    }));

    verify(objectFactDao, never()).storeFact(any());
    verify(objectFactDao).retrieveExistingFacts(factToSave);
    verify(factResponseConverter).apply(same(existingFact));
  }

  @Test
  public void testRefreshExistingFactWithCassandra() {
    handler.setUseCassandraForFactExistenceCheck(true);

    FactRecord factToSave = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setOrganizationID(UUID.randomUUID());
    mockSaveFact();

    // Mock fetching of existing Fact.
    FactRecord existingFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased)
            .setOrganizationID(UUID.randomUUID());
    when(objectFactDao.retrieveExistingFact(factToSave)).thenReturn(Optional.of(existingFact));
    when(securityContext.hasReadPermission(existingFact)).thenReturn(true);

    // Mock stuff needed for refreshing Fact.
    when(objectFactDao.refreshFact(existingFact)).thenReturn(existingFact);

    List<UUID> subjectIds = list(UUID.randomUUID());
    handler.saveFact(factToSave, "some comment", subjectIds);

    verify(objectFactDao).refreshFact(argThat(fact -> {
      assertEquals(existingFact, fact);
      assertEquals(set("some comment"), set(fact.getComments(), FactCommentRecord::getComment));
      assertEquals(set(subjectIds), set(fact.getAcl(), FactAclEntryRecord::getSubjectID));
      return true;
    }));

    verify(objectFactDao, never()).storeFact(any());
    verify(objectFactDao).retrieveExistingFact(factToSave);
    verify(factResponseConverter).apply(same(existingFact));
  }

  @Test
  public void testSaveFactRegisterTriggerEvent() {
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setOrganizationID(UUID.randomUUID());
    mockSaveFact();

    Fact addedFact = handler.saveFact(fact, null, null);

    verify(triggerContext).registerTriggerEvent(argThat(event -> {
      assertNotNull(event);
      assertEquals(TiServiceEvent.EventName.FactAdded.name(), event.getEvent());
      assertEquals(fact.getOrganizationID(), event.getOrganization());
      assertEquals(fact.getAccessMode().name(), event.getAccessMode().name());
      assertSame(addedFact, event.getContextParameters().get(TiServiceEvent.ContextParameter.AddedFact.name()));
      return true;
    }));
  }

  private void mockSaveFact() {
    // Mocking
    mockFactConverter();
    // Mock fetching of existing Fact.
    when(objectFactDao.retrieveExistingFacts(any())).thenReturn(ResultContainer.<FactRecord>builder().build());
    // Mock stuff needed for saving Fact.
    when(objectFactDao.storeFact(any())).thenAnswer(i -> i.getArgument(0));
  }

  private void mockFactConverter() {
    // Mock FactConverter needed for registering TriggerEvent.
    when(factResponseConverter.apply(any())).then(i -> {
      FactRecord record = i.getArgument(0);
      return Fact.builder()
              .setId(record.getId())
              .setAccessMode(no.mnemonic.act.platform.api.model.v1.AccessMode.valueOf(record.getAccessMode().name()))
              .setOrganization(Organization.builder().setId(record.getOrganizationID()).build().toInfo())
              .build();
    });
  }
}
