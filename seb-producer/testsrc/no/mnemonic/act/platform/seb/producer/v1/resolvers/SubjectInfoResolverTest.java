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

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubjectInfoResolverTest {

  @Mock
  private SubjectSPI subjectResolver;
  @Mock
  private ServiceAccountSPI credentialsResolver;

  private SubjectInfoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    when(credentialsResolver.get()).thenReturn(new Credentials() {});
    resolver = new SubjectInfoResolver(subjectResolver, credentialsResolver);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveWithInvalidCredentials() throws Exception {
    when(subjectResolver.resolveSubject(notNull(), isA(UUID.class))).thenThrow(InvalidCredentialsException.class);
    assertNull(resolver.apply(UUID.randomUUID()));
    verify(subjectResolver).resolveSubject(notNull(), isA(UUID.class));
  }

  @Test
  public void testResolveNoSubjectFound() throws Exception {
    UUID id = UUID.randomUUID();
    assertNull(resolver.apply(id));
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
}
