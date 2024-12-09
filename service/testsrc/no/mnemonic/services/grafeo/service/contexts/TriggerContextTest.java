package no.mnemonic.services.grafeo.service.contexts;

import no.mnemonic.services.triggers.pipeline.api.TriggerEvent;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TriggerContextTest {

  @Mock
  private TriggerEventConsumer triggerEventConsumer;
  @Mock
  private TriggerEvent triggerEvent;

  private TriggerContext context;

  @BeforeEach
  public void setUp() {
    context = TriggerContext.builder().setTriggerEventConsumer(triggerEventConsumer).build();
  }

  @Test
  public void testCreateContextWithoutTriggerEventConsumerThrowsException() {
    assertThrows(RuntimeException.class, () -> TriggerContext.builder().build());
  }

  @Test
  public void testGetContextNotSet() {
    assertThrows(IllegalStateException.class, TriggerContext::get);
  }

  @Test
  public void testSetContextTwice() throws Exception {
    try (TriggerContext ignored = TriggerContext.set(context)) {
      assertThrows(IllegalStateException.class, () -> TriggerContext.set(context));
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
