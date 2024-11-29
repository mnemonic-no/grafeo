package no.mnemonic.services.grafeo.seb.producer.v1.converters;

import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.seb.model.v1.AclEntrySEB;
import no.mnemonic.services.grafeo.seb.model.v1.OriginInfoSEB;
import no.mnemonic.services.grafeo.seb.model.v1.SubjectInfoSEB;
import no.mnemonic.services.grafeo.seb.producer.v1.resolvers.OriginInfoDaoResolver;
import no.mnemonic.services.grafeo.seb.producer.v1.resolvers.SubjectInfoServiceAccountResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AclEntryConverterTest {

  @Mock
  private SubjectInfoServiceAccountResolver subjectResolver;
  @Mock
  private OriginInfoDaoResolver originResolver;
  @InjectMocks
  private AclEntryConverter converter;

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
    when(subjectResolver.apply(any())).thenReturn(SubjectInfoSEB.builder().build());
    when(originResolver.apply(any())).thenReturn(OriginInfoSEB.builder().build());

    FactAclEntryRecord record = new FactAclEntryRecord()
            .setId(UUID.randomUUID())
            .setSubjectID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789);

    AclEntrySEB seb = converter.apply(record);
    assertNotNull(seb);
    assertEquals(record.getId(), seb.getId());
    assertNotNull(seb.getSubject());
    assertNotNull(seb.getOrigin());
    assertEquals(record.getTimestamp(), seb.getTimestamp());

    verify(subjectResolver).apply(record.getSubjectID());
    verify(originResolver).apply(record.getOriginID());
  }
}
