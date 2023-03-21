package no.mnemonic.services.grafeo.service.ti.converters.response;

import no.mnemonic.services.grafeo.api.model.v1.AclEntry;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.OriginByIdResponseResolver;
import no.mnemonic.services.grafeo.service.ti.resolvers.response.SubjectByIdResponseResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AclEntryResponseConverterTest {

  @Mock
  private OriginByIdResponseResolver originConverter;
  @Mock
  private SubjectByIdResponseResolver subjectConverter;

  private AclEntryResponseConverter converter;

  @Before
  public void setUp() {
    initMocks(this);

    when(originConverter.apply(notNull())).thenAnswer(i -> Origin.builder().setId(i.getArgument(0)).build());
    when(subjectConverter.apply(notNull())).thenAnswer(i -> Subject.builder().setId(i.getArgument(0)).build());

    converter = new AclEntryResponseConverter(originConverter, subjectConverter);
  }

  @Test
  public void testConvertNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmpty() {
    assertNotNull(converter.apply(new FactAclEntryRecord()));
  }

  @Test
  public void testConvertFull() {
    FactAclEntryRecord record = createRecord();
    assertModel(record, converter.apply(record));
  }

  private FactAclEntryRecord createRecord() {
    return new FactAclEntryRecord()
            .setId(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setSubjectID(UUID.randomUUID())
            .setTimestamp(123456789);
  }

  private void assertModel(FactAclEntryRecord record, AclEntry model) {
    assertEquals(record.getId(), model.getId());
    assertEquals(record.getOriginID(), model.getOrigin().getId());
    assertEquals(record.getSubjectID(), model.getSubject().getId());
    assertEquals(record.getTimestamp(), (long) model.getTimestamp());
  }
}
