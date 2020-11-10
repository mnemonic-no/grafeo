package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.ServiceAccountSPI;
import no.mnemonic.act.platform.auth.SubjectSPI;
import no.mnemonic.act.platform.seb.model.v1.SubjectInfoSEB;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubjectInfoServiceAccountResolverTest {

  @Mock
  private SubjectSPI subjectResolver;
  @Mock
  private ServiceAccountSPI credentialsResolver;

  private Map<UUID, Subject> subjectCache;
  private SubjectInfoServiceAccountResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    when(credentialsResolver.get()).thenReturn(new Credentials() {});
    subjectCache = new HashMap<>();
    resolver = new SubjectInfoServiceAccountResolver(subjectResolver, credentialsResolver, subjectCache);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveWithInvalidCredentials() throws Exception {
    UUID id = UUID.randomUUID();
    when(subjectResolver.resolveSubject(notNull(), isA(UUID.class))).thenThrow(InvalidCredentialsException.class);

    SubjectInfoSEB seb = resolver.apply(id);
    assertNotNull(seb);
    assertEquals(id, seb.getId());
    assertEquals("N/A", seb.getName());

    verify(subjectResolver).resolveSubject(notNull(), eq(id));
  }

  @Test
  public void testResolveNoSubjectFound() throws Exception {
    UUID id = UUID.randomUUID();

    SubjectInfoSEB seb = resolver.apply(id);
    assertNotNull(seb);
    assertEquals(id, seb.getId());
    assertEquals("N/A", seb.getName());

    verify(subjectResolver).resolveSubject(notNull(), eq(id));
  }

  @Test
  public void testResolveSubjectFound() throws Exception {
    Subject model = Subject.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    when(subjectResolver.resolveSubject(notNull(), isA(UUID.class))).thenReturn(model);

    SubjectInfoSEB seb = resolver.apply(model.getId());
    assertNotNull(seb);
    assertEquals(model.getId(), seb.getId());
    assertEquals(model.getName(), seb.getName());

    verify(subjectResolver).resolveSubject(notNull(), eq(model.getId()));
  }

  @Test
  public void testResolveCachesSubject() throws Exception {
    Subject model = Subject.builder().setId(UUID.randomUUID()).build();
    when(subjectResolver.resolveSubject(notNull(), isA(UUID.class))).thenReturn(model);

    resolver.apply(model.getId());
    assertEquals(1, subjectCache.size());
    assertSame(model, subjectCache.get(model.getId()));
  }

  @Test
  public void testResolvePreviouslyCachedSubject() {
    Subject model = Subject.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    subjectCache.put(model.getId(), model);

    SubjectInfoSEB seb = resolver.apply(model.getId());
    assertNotNull(seb);
    assertEquals(model.getId(), seb.getId());
    assertEquals(model.getName(), seb.getName());

    verifyNoInteractions(subjectResolver);
  }
}
