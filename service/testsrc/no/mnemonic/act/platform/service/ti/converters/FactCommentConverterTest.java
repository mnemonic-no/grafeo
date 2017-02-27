package no.mnemonic.act.platform.service.ti.converters;

import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.model.v1.Source;
import no.mnemonic.act.platform.entity.cassandra.FactCommentEntity;
import org.junit.Test;

import java.util.UUID;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FactCommentConverterTest {

  private final Function<UUID, Source> sourceConverter = id -> Source.builder().setId(id).build();

  @Test
  public void testConvertFactComment() {
    FactCommentEntity entity = createEntity();
    assertModel(entity, createConverter().apply(entity));
  }

  @Test
  public void testConvertNullReturnsNull() {
    assertNull(createConverter().apply(null));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateConverterWithoutSourceConverterThrowsException() {
    FactCommentConverter.builder().build();
  }

  private FactCommentConverter createConverter() {
    return FactCommentConverter.builder()
            .setSourceConverter(sourceConverter)
            .build();
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
    assertEquals(entity.getSourceID(), model.getSource().getId());
    assertEquals(entity.getComment(), model.getComment());
    assertEquals(entity.getTimestamp(), (long) model.getTimestamp());
  }

}
