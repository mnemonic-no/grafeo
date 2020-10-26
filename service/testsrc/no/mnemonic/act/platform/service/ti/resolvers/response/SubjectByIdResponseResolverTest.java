package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.ServiceAccountSPI;
import no.mnemonic.act.platform.auth.SubjectSPI;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubjectByIdResponseResolverTest {

  @Mock
  private SubjectSPI subjectResolver;
  @Mock
  private ServiceAccountSPI credentialsResolver;

  private Map<UUID, Subject> responseCache;
  private SubjectByIdResponseResolver converter;

  @Before
  public void setup() {
    initMocks(this);
    responseCache = new HashMap<>();
    when(credentialsResolver.get()).thenReturn(new Credentials() {});
    converter = new SubjectByIdResponseResolver(subjectResolver, credentialsResolver, responseCache);
  }

  @Test
  public void testConvertCachedSubject() {
    UUID id = UUID.randomUUID();
    Subject model = Subject.builder().build();
    responseCache.put(id, model);

    assertSame(model, converter.apply(id));
    verifyNoInteractions(subjectResolver);
  }

  @Test
  public void testConvertUncachedSubject() throws Exception {
    UUID id = UUID.randomUUID();
    Subject model = Subject.builder().build();

    when(subjectResolver.resolveSubject(notNull(), isA(UUID.class))).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(subjectResolver).resolveSubject(notNull(), eq(id));
  }

  @Test
  public void testConvertUncachedSubjectWithInvalidCredentials() throws Exception {
    UUID id = UUID.randomUUID();
    when(subjectResolver.resolveSubject(notNull(), isA(UUID.class))).thenThrow(InvalidCredentialsException.class);

    Subject model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(subjectResolver).resolveSubject(notNull(), eq(id));
  }

  @Test
  public void testConvertUncachedSubjectNotAvailable() throws Exception {
    UUID id = UUID.randomUUID();
    Subject model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(subjectResolver).resolveSubject(notNull(), eq(id));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
