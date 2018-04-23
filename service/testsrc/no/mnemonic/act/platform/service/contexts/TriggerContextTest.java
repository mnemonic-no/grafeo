package no.mnemonic.act.platform.service.contexts;

import no.mnemonic.services.triggers.pipeline.api.TriggerEvent;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class TriggerContextTest {

  @Mock
  private TriggerEventConsumer triggerEventConsumer;
  @Mock
  private TriggerEvent triggerEvent;

  private TriggerContext context;

  @Before
  public void setUp() {
    initMocks(this);
    context = TriggerContext.builder().setTriggerEventConsumer(triggerEventConsumer).build();
  }

  @Test(expected = RuntimeException.class)
  public void testCreateContextWithoutTriggerEventConsumerThrowsException() {
    TriggerContext.builder().build();
  }

  @Test(expected = IllegalStateException.class)
  public void testGetContextNotSet() {
    TriggerContext.get();
  }

  @Test(expected = IllegalStateException.class)
  public void testSetContextTwice() throws Exception {
    try (TriggerContext ignored = TriggerContext.set(context)) {
      TriggerContext.set(context);
    }
  }

  @Test
  public void testSetAndGetContext() throws Exception {
    assertFalse(TriggerContext.isSet());

    try (TriggerContext ctx = TriggerContext.set(context)) {
      assertTrue(TriggerContext.isSet());
      assertSame(ctx, TriggerContext.get());
    }

    assertFalse(TriggerContext.isSet());
  }

  @Test
  public void testClearExistingContext() {
    TriggerContext ctx = TriggerContext.set(context);
    assertTrue(TriggerContext.isSet());
    TriggerContext oldCtx = TriggerContext.clear();
    assertFalse(TriggerContext.isSet());
    assertSame(ctx, oldCtx);
  }

  @Test
  public void testClearNonExistingContext() {
    assertNull(TriggerContext.clear());
    assertFalse(TriggerContext.isSet());
  }

  @Test
  public void testCloseContextSubmitsRegisteredTriggerEvents() throws Exception {
    try (TriggerContext ctx = TriggerContext.set(context)) {
      ctx.registerTriggerEvent(triggerEvent);
      verify(triggerEventConsumer, never()).submit(any());
    }

    verify(triggerEventConsumer).submit(triggerEvent);
  }

}
