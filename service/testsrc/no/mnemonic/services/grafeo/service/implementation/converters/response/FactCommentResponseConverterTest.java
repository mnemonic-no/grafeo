package no.mnemonic.services.grafeo.service.implementation.converters.response;

import no.mnemonic.services.grafeo.api.model.v1.FactComment;
import no.mnemonic.services.grafeo.api.model.v1.Origin;
import no.mnemonic.services.grafeo.dao.api.record.FactCommentRecord;
import no.mnemonic.services.grafeo.service.implementation.resolvers.response.OriginByIdResponseResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FactCommentResponseConverterTest {

  @Mock
  private OriginByIdResponseResolver originConverter;
  @InjectMocks
  private FactCommentResponseConverter converter;

  @Test
  public void testConvertNull() {
    assertNull(converter.apply(null));
  }

  @Test
  public void testConvertEmpty() {
    assertNotNull(converter.apply(new FactCommentRecord()));
  }

  @Test
  public void testConvertFull() {
    when(originConverter.apply(notNull())).thenAnswer(i -> Origin.builder().setId(i.getArgument(0)).build());

    FactCommentRecord record = createRecord();
    assertModel(record, converter.apply(record));
  }

  private FactCommentRecord createRecord() {
    return new FactCommentRecord()
            .setId(UUID.randomUUID())
            .setReplyToID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setComment("Hello World!")
            .setTimestamp(123456789);
  }

  private void assertModel(FactCommentRecord record, FactComment model) {
    assertEquals(record.getId(), model.getId());
    assertEquals(record.getReplyToID(), model.getReplyTo());
    assertEquals(record.getOriginID(), model.getOrigin().getId());
    assertEquals(record.getComment(), model.getComment());
    assertEquals(record.getTimestamp(), (long) model.getTimestamp());
  }
}
