package no.mnemonic.act.platform.seb.producer.v1.resolvers;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.seb.model.v1.SubjectInfoSEB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubjectInfoResolverTest {

  @Mock
  private SubjectResolver subjectResolver;

  private SubjectInfoResolver resolver;

  @Before
  public void setUp() {
    initMocks(this);
    resolver = new SubjectInfoResolver(subjectResolver);
  }

  @Test
  public void testResolveNull() {
    assertNull(resolver.apply(null));
  }

  @Test
  public void testResolveNoSubjectFound() {
    UUID id = UUID.randomUUID();
    assertNull(resolver.apply(id));
    verify(subjectResolver).resolveSubject(id);
  }

  @Test
  public void testResolveSubjectFound() {
    Subject model = Subject.builder()
            .setId(UUID.randomUUID())
            .setName("name")
            .build();
    when(subjectResolver.resolveSubject(isA(UUID.class))).thenReturn(model);

    SubjectInfoSEB seb = resolver.apply(model.getId());
    assertNotNull(seb);
    assertEquals(model.getId(), seb.getId());
    assertEquals(model.getName(), seb.getName());

    verify(subjectResolver).resolveSubject(model.getId());
  }
}
