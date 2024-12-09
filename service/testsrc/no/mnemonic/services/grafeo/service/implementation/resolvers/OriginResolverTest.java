package no.mnemonic.services.grafeo.service.implementation.resolvers;

import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.auth.ServiceAccountSPI;
import no.mnemonic.services.grafeo.auth.SubjectSPI;
import no.mnemonic.services.grafeo.dao.cassandra.OriginManager;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OriginResolverTest {

  @Mock
  private OriginManager originManager;
  @Mock
  private SubjectSPI subjectResolver;
  @Mock
  private ServiceAccountSPI credentialsResolver;
  @InjectMocks
  private OriginResolver resolver;

  private final OriginEntity groupOrigin = new OriginEntity()
          .setId(UUID.randomUUID())
          .setType(OriginEntity.Type.Group);
  private final OriginEntity userOrigin = new OriginEntity()
          .setId(UUID.randomUUID())
          .setType(OriginEntity.Type.User)
          .setName("originalName")
          .setOrganizationID(UUID.randomUUID());

  @BeforeEach
  public void setUp() {
    lenient().when(credentialsResolver.get()).thenReturn(new Credentials() {});
  }

  @Test
  public void testApplyNonExistingOrigin() {
    UUID id = UUID.randomUUID();
    assertNull(resolver.apply(id));
    verify(originManager).getOrigin(id);
  }

  @Test
  public void testApplyFetchGroupOrigin() {
    when(originManager.getOrigin(groupOrigin.getId())).thenReturn(groupOrigin);

    assertSame(groupOrigin, resolver.apply(groupOrigin.getId()));
    verify(originManager).getOrigin(groupOrigin.getId());
    verifyNoMoreInteractions(originManager);
    verifyNoInteractions(subjectResolver);
  }

  @Test
  public void testApplyFetchUserOriginWithoutUpdate() throws Exception {
    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);

    assertSame(userOrigin, resolver.apply(userOrigin.getId()));
    verify(originManager).getOrigin(userOrigin.getId());
    verify(subjectResolver).resolveSubject(notNull(), eq(userOrigin.getId()));
    verifyNoMoreInteractions(originManager);
  }

  @Test
  public void testApplyFetchUserOriginWithInvalidCredentials() throws Exception {
    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(subjectResolver.resolveSubject(notNull(), eq(userOrigin.getId()))).thenThrow(InvalidCredentialsException.class);

    assertSame(userOrigin, resolver.apply(userOrigin.getId()));
    verify(originManager).getOrigin(userOrigin.getId());
    verify(subjectResolver).resolveSubject(notNull(), eq(userOrigin.getId()));
    verifyNoMoreInteractions(originManager);
  }

  @Test
  public void testApplyFetchUserOriginWithUpdate() throws Exception {
    Subject userSubject = Subject.builder()
            .setId(userOrigin.getId())
            .setName("updatedName")
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).build().toInfo())
            .build();

    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(subjectResolver.resolveSubject(notNull(), eq(userOrigin.getId()))).thenReturn(userSubject);

    resolver.apply(userOrigin.getId());
    verify(originManager).saveOrigin(argThat(entity -> {
      assertEquals(userSubject.getName(), entity.getName());
      assertEquals(userSubject.getOrganization().getId(), entity.getOrganizationID());
      return true;
    }));
  }

  @Test
  public void testApplyFetchUserOriginSkipUpdateFields() throws Exception {
    Subject userSubject = Subject.builder()
            .setId(userOrigin.getId())
            .setName(" ")
            .build();

    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(subjectResolver.resolveSubject(notNull(), eq(userOrigin.getId()))).thenReturn(userSubject);

    resolver.apply(userOrigin.getId());
    verify(originManager, never()).saveOrigin(any());
  }

  @Test
  public void testApplyFetchUserOriginSkipUpdateNameNA() throws Exception {
    Subject userSubject = Subject.builder()
            .setId(userOrigin.getId())
            .setName("N/A")
            .build();

    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(subjectResolver.resolveSubject(notNull(), eq(userOrigin.getId()))).thenReturn(userSubject);

    resolver.apply(userOrigin.getId());
    verify(originManager, never()).saveOrigin(any());
  }

  @Test
  public void testApplyFetchUserOriginSkipUpdateNoChanges() throws Exception {
    Subject userSubject = Subject.builder()
            .setId(userOrigin.getId())
            .setName(userOrigin.getName())
            .setOrganization(Organization.builder().setId(userOrigin.getOrganizationID()).build().toInfo())
            .build();

    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(subjectResolver.resolveSubject(notNull(), eq(userOrigin.getId()))).thenReturn(userSubject);

    resolver.apply(userOrigin.getId());
    verify(originManager, never()).saveOrigin(any());
  }

  @Test
  public void testApplyFetchUserOriginSkipUpdateNameFromOtherOrigin() throws Exception {
    OriginEntity otherOrigin = new OriginEntity()
            .setId(UUID.randomUUID())
            .setName("otherOrigin");
    Subject userSubject = Subject.builder()
            .setId(userOrigin.getId())
            .setName(otherOrigin.getName())
            .build();

    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(originManager.getOrigin(otherOrigin.getName())).thenReturn(otherOrigin);
    when(subjectResolver.resolveSubject(notNull(), eq(userOrigin.getId()))).thenReturn(userSubject);

    resolver.apply(userOrigin.getId());
    verify(originManager, never()).saveOrigin(any());
  }
}
