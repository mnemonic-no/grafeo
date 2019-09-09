package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.model.v1.Origin;
import no.mnemonic.act.platform.dao.cassandra.entity.FactCommentEntity;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FactCommentConverterTest {

  private final Function<UUID, Origin> originConverter = id -> Origin.builder().setId(id).build();
  private final FactCommentConverter converter = new FactCommentConverter(originConverter);

  @Test
  public void testConvertFactComment() {
    FactCommentEntity entity = createEntity();
    assertModel(entity, converter.apply(entity));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(converter.apply(null));
  }

  private FactCommentEntity createEntity() {
    return new FactCommentEntity()
            .setId(UUID.randomUUID())
            .setFactID(UUID.randomUUID())
            .setReplyToID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setComment("Hello World!")
            .setTimestamp(123456789);
  }

  private void assertModel(FactCommentEntity entity, FactComment model) {
    assertEquals(entity.getId(), model.getId());
    assertEquals(entity.getReplyToID(), model.getReplyTo());
    assertEquals(entity.getSourceID(), model.getOrigin().getId());
    assertEquals(entity.getComment(), model.getComment());
    assertEquals(entity.getTimestamp(), (long) model.getTimestamp());
  }
}
