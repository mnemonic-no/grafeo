package no.mnemonic.services.grafeo.seb.producer.v1;

import no.mnemonic.messaging.documentchannel.DocumentChannel;
import no.mnemonic.messaging.documentchannel.DocumentDestination;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;
import no.mnemonic.services.grafeo.seb.producer.v1.converters.FactConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FactProducerTest {

  @Mock
  private FactConverter converter;
  @Mock
  private DocumentDestination<FactSEB> destination;
  @Mock
  private DocumentChannel<FactSEB> channel;
  @InjectMocks
  private FactProducer producer;

  @Test
  public void testConsumeNull() {
    producer.accept(null);
    verifyNoInteractions(channel);
  }

  @Test
  public void testConsumeObject() {
    when(destination.getDocumentChannel()).thenReturn(channel);
    when(converter.apply(any())).thenReturn(FactSEB.builder().build());

    producer.accept(new FactRecord());
    verify(converter).apply(notNull());
    verify(channel).sendDocument(notNull());
  }
}
