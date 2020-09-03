package no.mnemonic.act.platform.seb.producer.v1.converters;

import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.seb.model.v1.AclEntrySEB;
import no.mnemonic.act.platform.seb.model.v1.OriginInfoSEB;
import no.mnemonic.act.platform.seb.model.v1.SubjectInfoSEB;
import no.mnemonic.act.platform.seb.producer.v1.resolvers.OriginInfoResolver;
import no.mnemonic.act.platform.seb.producer.v1.resolvers.SubjectInfoResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AclEntryConverterTest {

  @Mock
  private SubjectInfoResolver subjectResolver;
  @Mock
  private OriginInfoResolver originResolver;

  private AclEntryConverter converter;

  @Before
  public void setUp() {
    initMocks(this);

    when(subjectResolver.apply(any())).thenReturn(SubjectInfoSEB.builder().build());
    when(originResolver.apply(any())).thenReturn(OriginInfoSEB.builder().build());

    converter = new AclEntryConverter(subjectResolver, originResolver);
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
