package no.mnemonic.services.grafeo.seb.esengine.v1.consumers;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.dao.elastic.FactSearchManager;
import no.mnemonic.services.grafeo.dao.elastic.document.FactDocument;
import no.mnemonic.services.grafeo.seb.esengine.v1.converters.FactConverter;
import no.mnemonic.services.grafeo.seb.model.v1.FactSEB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.Daily;
import static no.mnemonic.services.grafeo.dao.elastic.FactSearchManager.TargetIndex.TimeGlobal;
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
    verify(factSearchManager, times(3)).indexFact(notNull(), notNull());
  }

  @Test
  public void testConsumeIndexesDaily() {
    when(factConverter.apply(any())).thenReturn(new FactDocument());
    factConsumer.consume(SetUtils.set(FactSEB.builder().build()));

    verify(factSearchManager).indexFact(notNull(), eq(Daily));
  }

  @Test
  public void testConsumeIndexesTimeGlobal() {
    when(factConverter.apply(any())).thenReturn(new FactDocument());
    factConsumer.consume(SetUtils.set(FactSEB.builder().addFlag(FactSEB.Flag.TimeGlobalIndex).build()));

    verify(factSearchManager).indexFact(notNull(), eq(TimeGlobal));
  }
}
