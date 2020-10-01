package no.mnemonic.act.platform.seb.esengine.v1.consumers;

import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.seb.esengine.v1.converters.FactConverter;
import no.mnemonic.act.platform.seb.model.v1.FactSEB;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactConsumerTest {

  @Mock
  private FactSearchManager factSearchManager;
  @Mock
  private FactConverter factConverter;

  private FactConsumer factConsumer;

  @Before
  public void setUp() {
    initMocks(this);

    factConsumer = new FactConsumer(factSearchManager, factConverter);
  }

  @Test
  public void testConsumeNothing() {
    factConsumer.consume(null);
    factConsumer.consume(SetUtils.set());

    verifyNoInteractions(factConverter);
    verifyNoInteractions(factSearchManager);
  }

  @Test
  public void testConsumeSkipsNullElements() {
    FactSEB fact = FactSEB.builder().build();
    factConsumer.consume(SetUtils.set(fact));

    verify(factConverter).apply(fact);
    verifyNoInteractions(factSearchManager);
  }

  @Test
  public void testConsumeIndexesFacts() {
    when(factConverter.apply(any())).thenReturn(new FactDocument());
    factConsumer.consume(SetUtils.set(FactSEB.builder().build(), FactSEB.builder().build(), FactSEB.builder().build()));

    verify(factConverter, times(3)).apply(notNull());
    verify(factSearchManager, times(3)).indexFact(notNull());
  }
}
