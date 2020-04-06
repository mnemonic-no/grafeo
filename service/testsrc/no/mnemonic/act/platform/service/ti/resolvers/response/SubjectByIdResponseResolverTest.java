package no.mnemonic.act.platform.service.ti.resolvers.response;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.SubjectResolver;
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
  private SubjectResolver subjectResolver;

  private Map<UUID, Subject> responseCache;
  private SubjectByIdResponseResolver converter;

  @Before
  public void setup() {
    initMocks(this);
    responseCache = new HashMap<>();
    converter = new SubjectByIdResponseResolver(subjectResolver, responseCache);
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
  public void testConvertUncachedSubject() {
    UUID id = UUID.randomUUID();
    Subject model = Subject.builder().build();

    when(subjectResolver.resolveSubject(id)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(subjectResolver).resolveSubject(id);
  }

  @Test
  public void testConvertUncachedSubjectNotAvailable() {
    UUID id = UUID.randomUUID();
    Subject model = converter.apply(id);

    assertNotNull(model);
    assertEquals(id, model.getId());
    assertEquals("N/A", model.getName());

    verify(subjectResolver).resolveSubject(id);
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }
}
