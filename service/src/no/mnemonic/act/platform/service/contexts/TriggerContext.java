package no.mnemonic.act.platform.service.contexts;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import no.mnemonic.services.triggers.pipeline.api.TriggerEvent;
import no.mnemonic.services.triggers.pipeline.api.TriggerEventConsumer;

import java.util.HashSet;
import java.util.Set;

/**
 * The TriggerContext provides a mechanism to register {@link TriggerEvent}s which are queued for evaluation.
 * All registered {@link TriggerEvent}s will be submitted to evaluation once the TriggerContext is closed.
 */
public class TriggerContext implements AutoCloseable {

  private static final Logger LOGGER = Logging.getLogger(TriggerContext.class);
  private static final ThreadLocal<TriggerContext> currentContext = new ThreadLocal<>();

  private final Set<TriggerEvent> queuedTriggerEvents = new HashSet<>();
  private final TriggerEventConsumer triggerEventConsumer;

  private TriggerContext(TriggerEventConsumer triggerEventConsumer) {
    this.triggerEventConsumer = ObjectUtils.notNull(triggerEventConsumer, "'triggerEventConsumer' not set in TriggerContext.");
  }

  /**
   * Retrieve the current TriggerContext.
   *
   * @return Current TriggerContext
   * @throws IllegalStateException If TriggerContext is not set.
   */
  public static TriggerContext get() {
    if (currentContext.get() == null) throw new IllegalStateException("Trigger context not set.");
    return currentContext.get();
  }

  /**
   * Set a new TriggerContext (but it is not allowed to override an existing TriggerContext).
   *
   * @param ctx New TriggerContext
   * @return New TriggerContext
   * @throws IllegalStateException If TriggerContext is already set.
   */
  public static TriggerContext set(TriggerContext ctx) {
    if (currentContext.get() != null) throw new IllegalStateException("Trigger context already set.");
    currentContext.set(ctx);
    return ctx;
  }

  /**
   * Determine if TriggerContext is set.
   *
   * @return True, if TriggerContext is set.
   */
  public static boolean isSet() {
    return currentContext.get() != null;
  }

  /**
   * Remove the current TriggerContext.
   * <p>
   * Use with caution as many parts of the code rely on a set TriggerContext. This method is mainly useful for testing.
   *
   * @return Current TriggerContext, or NULL if it was not set.
   */
  public static TriggerContext clear() {
    TriggerContext oldCtx = currentContext.get();
    currentContext.remove();
    return oldCtx;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void close() throws Exception {
    // Submit all events whenever this context is closed.
    LambdaUtils.forEachTry(queuedTriggerEvents, triggerEventConsumer::submit, ex -> LOGGER.error(ex, "Could not submit TriggerEvent."));
    queuedTriggerEvents.clear();
    currentContext.remove();
  }

  /**
   * Queue a new {@link TriggerEvent} for evaluation. It will be submitted to evaluation when the TriggerContext is closed.
   *
   * @param event {@link TriggerEvent} to queue
   * @return Current TriggerContext
   */
  public TriggerContext registerTriggerEvent(TriggerEvent event) {
    if (event != null) {
      queuedTriggerEvents.add(event);
    }
    return this;
  }

  public static class Builder {
    private TriggerEventConsumer triggerEventConsumer;

    private Builder() {
    }

    public TriggerContext build() {
      return new TriggerContext(triggerEventConsumer);
    }

    public Builder setTriggerEventConsumer(TriggerEventConsumer triggerEventConsumer) {
      this.triggerEventConsumer = triggerEventConsumer;
      return this;
    }
  }

}
