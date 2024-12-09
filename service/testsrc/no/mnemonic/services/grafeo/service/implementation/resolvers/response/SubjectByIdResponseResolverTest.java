package no.mnemonic.services.grafeo.service.implementation.resolvers.response;

import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.auth.ServiceAccountSPI;
import no.mnemonic.services.grafeo.auth.SubjectSPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubjectByIdResponseResolverTest {

  @Mock
  private SubjectSPI subjectResolver;
  @Mock
  private ServiceAccountSPI credentialsResolver;

  private Map<UUID, Subject> responseCache;
  private SubjectByIdResponseResolver converter;

  @BeforeEach
  public void setup() {
    lenient().when(credentialsResolver.get()).thenReturn(new Credentials() {});
    responseCache = new HashMap<>();
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
