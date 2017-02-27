package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.model.v1.Source;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.entity.cassandra.FactAclEntity;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AclEntryConverterTest {

  private final Function<UUID, Source> sourceConverter = id -> Source.builder().setId(id).build();
  private final Function<UUID, Subject> subjectConverter = id -> Subject.builder().setId(id).build();

  @Test
  public void testConvertAclEntry() {
    FactAclEntity entity = createEntity();
    assertModel(entity, createConverter().apply(entity));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(createConverter().apply(null));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutSourceConverterThrowsException() {
    AclEntryConverter.builder().setSubjectConverter(subjectConverter).build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutSubjectConverterThrowsException() {
    AclEntryConverter.builder().setSourceConverter(sourceConverter).build();
  }

  private AclEntryConverter createConverter() {
    return AclEntryConverter.builder()
            .setSourceConverter(sourceConverter)
            .setSubjectConverter(subjectConverter)
            .build();
  }

  private FactAclEntity createEntity() {
    return new FactAclEntity()
            .setId(UUID.randomUUID())
            .setFactID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setSubjectID(UUID.randomUUID())
            .setTimestamp(123456789);
  }

  private void assertModel(FactAclEntity entity, AclEntry model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getSourceID(), model.getSource().getId());
    assertEquals(entity.getSubjectID(), model.getSubject().getId());
    assertEquals(entity.getTimestamp(), (long) model.getTimestamp());
  }

}
