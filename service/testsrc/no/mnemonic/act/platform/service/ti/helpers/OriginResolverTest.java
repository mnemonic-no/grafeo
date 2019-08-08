package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class OriginResolverTest {

  @Mock
  private OriginManager originManager;
  @Mock
  private SubjectResolver subjectResolver;

  private OriginResolver resolver;

  private final OriginEntity groupOrigin = new OriginEntity()
          .setId(UUID.randomUUID())
          .setType(OriginEntity.Type.Group);
  private final OriginEntity userOrigin = new OriginEntity()
          .setId(UUID.randomUUID())
          .setType(OriginEntity.Type.User)
          .setName("originalName")
          .setOrganizationID(UUID.randomUUID());

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new OriginResolver(originManager, subjectResolver);
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
    verifyZeroInteractions(subjectResolver);
  }

  @Test
  public void testApplyFetchUserOriginWithoutUpdate() {
    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);

    assertSame(userOrigin, resolver.apply(userOrigin.getId()));
    verify(originManager).getOrigin(userOrigin.getId());
    verify(subjectResolver).resolveSubject(userOrigin.getId());
    verifyNoMoreInteractions(originManager);
  }

  @Test
  public void testApplyFetchUserOriginWithUpdate() {
    Subject userSubject = Subject.builder()
            .setId(userOrigin.getId())
            .setName("updatedName")
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).build().toInfo())
            .build();

    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(subjectResolver.resolveSubject(userOrigin.getId())).thenReturn(userSubject);

    resolver.apply(userOrigin.getId());
    verify(originManager).saveOrigin(argThat(entity -> {
      assertEquals(userSubject.getName(), entity.getName());
      assertEquals(userSubject.getOrganization().getId(), entity.getOrganizationID());
      return true;
    }));
  }

  @Test
  public void testApplyFetchUserOriginSkipUpdateFields() {
    Subject userSubject = Subject.builder()
            .setId(userOrigin.getId())
            .setName(" ")
            .build();

    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(subjectResolver.resolveSubject(userOrigin.getId())).thenReturn(userSubject);

    resolver.apply(userOrigin.getId());
    verify(originManager, never()).saveOrigin(any());
  }

  @Test
  public void testApplyFetchUserOriginSkipUpdateNameNA() {
    Subject userSubject = Subject.builder()
            .setId(userOrigin.getId())
            .setName("N/A")
            .build();

    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(subjectResolver.resolveSubject(userOrigin.getId())).thenReturn(userSubject);

    resolver.apply(userOrigin.getId());
    verify(originManager, never()).saveOrigin(any());
  }

  @Test
  public void testApplyFetchUserOriginSkipUpdateNoChanges() {
    Subject userSubject = Subject.builder()
            .setId(userOrigin.getId())
            .setName(userOrigin.getName())
            .setOrganization(Organization.builder().setId(userOrigin.getOrganizationID()).build().toInfo())
            .build();

    when(originManager.getOrigin(userOrigin.getId())).thenReturn(userOrigin);
    when(subjectResolver.resolveSubject(userOrigin.getId())).thenReturn(userSubject);

    resolver.apply(userOrigin.getId());
    verify(originManager, never()).saveOrigin(any());
  }
}
