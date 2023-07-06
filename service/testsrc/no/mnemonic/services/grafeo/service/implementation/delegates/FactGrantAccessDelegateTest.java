package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.api.request.v1.GrantFactAccessRequest;
import no.mnemonic.services.grafeo.auth.SubjectSPI;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.AclEntryResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactGrantAccessDelegateTest {

  @Mock
  private ObjectFactDao objectFactDao;
  @Mock
  private FactRequestResolver factRequestResolver;
  @Mock
  private SubjectSPI subjectResolver;
  @Mock
  private AclEntryResponseConverter aclEntryResponseConverter;
  @Mock
  private GrafeoSecurityContext securityContext;

  private final Subject subject = Subject.builder()
          .setId(UUID.randomUUID())
          .setName("subject")
          .build();

  private FactGrantAccessDelegate delegate;

  @Before
  public void setup() {
    initMocks(this);
    when(securityContext.getCredentials()).thenReturn(new Credentials() {});
    delegate = new FactGrantAccessDelegate(
            securityContext,
            objectFactDao,
            factRequestResolver,
            subjectResolver,
            aclEntryResponseConverter
    );
  }

  @Test(expected = AccessDeniedException.class)
  public void testGrantFactAccessNoAccessToFact() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkReadPermission(isA(FactRecord.class));

    delegate.handle(request);
  }

  @Test(expected = AccessDeniedException.class)
  public void testGrantFactAccessNoGrantPermission() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    doThrow(AccessDeniedException.class).when(securityContext).checkPermission(eq(FunctionConstants.grantGrafeoFactAccess), any());

    delegate.handle(request);
  }

  @Test
  public void testGrantFactAccessToPublicFact() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord().setAccessMode(FactRecord.AccessMode.Public));

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("fact.is.public"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testGrantFactAccessWithInvalidCredentials() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());
    when(subjectResolver.resolveSubject(notNull(), eq(request.getSubject()))).thenThrow(InvalidCredentialsException.class);

    assertThrows(AuthenticationFailedException.class, () -> delegate.handle(request));
    verify(subjectResolver).resolveSubject(notNull(), eq(request.getSubject()));
  }

  @Test
  public void testGrantFactAccessSubjectNotFound() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(new FactRecord());

    InvalidArgumentException ex = assertThrows(InvalidArgumentException.class, () -> delegate.handle(request));
    assertEquals(set("subject.not.exist"), set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
    verify(subjectResolver).resolveSubject(notNull(), eq(request.getSubject()));
  }

  @Test
  public void testGrantFactAccessSubjectAlreadyInAcl() throws Exception {
    GrantFactAccessRequest request = createGrantAccessRequest();
    FactAclEntryRecord existingEntry = createFactAclEntryRecord();
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(createFactRecord(request).addAclEntry(existingEntry));
    when(subjectResolver.resolveSubject(notNull(), eq(subject.getName()))).thenReturn(subject);

    delegate.handle(request);

    verify(objectFactDao, never()).storeFactAclEntry(any(), any());
    verify(aclEntryResponseConverter).apply(matchFactAclEntryRecord(existingEntry.getOriginID()));
  }

  @Test
  public void testGrantFactAccessBySubjectId() throws Exception {
    UUID currentUser = UUID.randomUUID();
    GrantFactAccessRequest request = createGrantAccessRequest().setSubject(subject.getId().toString());
    FactRecord fact = createFactRecord(request);
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(fact);
    when(subjectResolver.resolveSubject(notNull(), eq(subject.getId()))).thenReturn(subject);
    when(objectFactDao.storeFactAclEntry(notNull(), notNull())).then(i -> i.getArgument(1));
    when(securityContext.getCurrentUserID()).thenReturn(currentUser);

    delegate.handle(request);

    verify(objectFactDao).storeFactAclEntry(same(fact), matchFactAclEntryRecord(currentUser));
    verify(subjectResolver).resolveSubject(notNull(), eq(subject.getId()));
    verify(aclEntryResponseConverter).apply(matchFactAclEntryRecord(currentUser));
  }

  @Test
  public void testGrantFactAccessBySubjectName() throws Exception {
    UUID currentUser = UUID.randomUUID();
    GrantFactAccessRequest request = createGrantAccessRequest();
    FactRecord fact = createFactRecord(request);
    when(factRequestResolver.resolveFact(request.getFact())).thenReturn(fact);
    when(subjectResolver.resolveSubject(notNull(), eq(subject.getName()))).thenReturn(subject);
    when(objectFactDao.storeFactAclEntry(notNull(), notNull())).then(i -> i.getArgument(1));
    when(securityContext.getCurrentUserID()).thenReturn(currentUser);

    delegate.handle(request);

    verify(objectFactDao).storeFactAclEntry(same(fact), matchFactAclEntryRecord(currentUser));
    verify(subjectResolver).resolveSubject(notNull(), eq(subject.getName()));
    verify(aclEntryResponseConverter).apply(matchFactAclEntryRecord(currentUser));
  }

  private GrantFactAccessRequest createGrantAccessRequest() {
    return new GrantFactAccessRequest()
            .setFact(UUID.randomUUID())
            .setSubject(subject.getName());
  }

  private FactRecord createFactRecord(GrantFactAccessRequest request) {
    return new FactRecord()
            .setId(request.getFact())
            .setAccessMode(FactRecord.AccessMode.RoleBased);
  }

  private FactAclEntryRecord createFactAclEntryRecord() {
    return new FactAclEntryRecord()
            .setSubjectID(subject.getId())
            .setId(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789);
  }

  private FactAclEntryRecord matchFactAclEntryRecord(UUID origin) {
    return argThat(entry -> {
      assertNotNull(entry.getId());
      assertEquals(subject.getId(), entry.getSubjectID());
      assertEquals(origin, entry.getOriginID());
      assertTrue(entry.getTimestamp() > 0);
      return true;
    });
  }
}
