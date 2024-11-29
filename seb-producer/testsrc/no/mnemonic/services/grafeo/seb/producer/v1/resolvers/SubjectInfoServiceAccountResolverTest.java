package no.mnemonic.services.grafeo.seb.producer.v1.resolvers;

import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.auth.ServiceAccountSPI;
import no.mnemonic.services.grafeo.auth.SubjectSPI;
import no.mnemonic.services.grafeo.seb.model.v1.SubjectInfoSEB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubjectInfoServiceAccountResolverTest {

  @Mock
  private SubjectSPI subjectResolver;
  @Mock
  private ServiceAccountSPI credentialsResolver;

  private Map<UUID, Subject> subjectCache;
  private SubjectInfoServiceAccountResolver resolver;

  @BeforeEach
  public void setUp() {
    lenient().when(credentialsResolver.get()).thenReturn(new Credentials() {});
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
