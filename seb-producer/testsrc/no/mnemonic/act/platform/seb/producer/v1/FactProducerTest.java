package no.mnemonic.act.platform.seb.producer.v1;

import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;
import no.mnemonic.act.platform.seb.producer.v1.converters.FactConverter;
import no.mnemonic.messaging.documentchannel.DocumentChannel;
import no.mnemonic.messaging.documentchannel.DocumentDestination;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactProducerTest {

  @Mock
  private FactConverter converter;
  @Mock
  private DocumentDestination<FactSEB> destination;
  @Mock
  private DocumentChannel<FactSEB> channel;

  private FactProducer producer;

  @Before
  public void setUp() {
    initMocks(this);
    when(destination.getDocumentChannel()).thenReturn(channel);

    producer = new FactProducer(converter, destination);
  }

  @Test
  public void testConsumeNull() {
    producer.accept(null);
    verifyNoInteractions(channel);
  }

  @Test
  public void testConsumeObject() {
    when(converter.apply(any())).thenReturn(FactSEB.builder().build());

    producer.accept(new FactRecord());
    verify(converter).apply(notNull());
    verify(channel).sendDocument(notNull());
  }
}
