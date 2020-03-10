package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.SubjectResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubjectByIdConverterTest {

  @Mock
  private SubjectResolver subjectResolver;

  private SubjectByIdConverter converter;

  @Before
  public void setup() {
    initMocks(this);
    converter = new SubjectByIdConverter(subjectResolver);
  }

  @Test
  public void testConvertSubject() {
    UUID id = UUID.randomUUID();
    Subject model = Subject.builder().build();

    when(subjectResolver.resolveSubject(id)).thenReturn(model);

    assertSame(model, converter.apply(id));
    verify(subjectResolver).resolveSubject(id);
  }

  @Test
  public void testConvertSubjectNotAvailable() {
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
