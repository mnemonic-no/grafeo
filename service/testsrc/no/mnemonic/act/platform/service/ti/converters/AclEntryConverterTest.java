package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AclEntryConverterTest {

  private final Function<UUID, Origin> originConverter = id -> Origin.builder().setId(id).build();
  private final Function<UUID, Subject> subjectConverter = id -> Subject.builder().setId(id).build();
  private final AclEntryConverter converter = new AclEntryConverter(originConverter, subjectConverter);

  @Test
  public void testConvertAclEntry() {
    FactAclEntity entity = createEntity();
    assertModel(entity, converter.apply(entity));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }

  private FactAclEntity createEntity() {
    return new FactAclEntity()
            .setId(UUID.randomUUID())
            .setFactID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setSubjectID(UUID.randomUUID())
            .setTimestamp(123456789);
  }

  private void assertModel(FactAclEntity entity, AclEntry model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getOriginID(), model.getOrigin().getId());
    assertEquals(entity.getSubjectID(), model.getSubject().getId());
    assertEquals(entity.getTimestamp(), (long) model.getTimestamp());
  }
}
