package no.mnemonic.act.platform.service.contexts;

/**
 * The RequestContext provides access to common functionality needed while processing service calls,
 * e.g. converters or DAO objects.
 */
public class RequestContext implements AutoCloseable {

  private static final ThreadLocal<RequestContext> currentContext = new ThreadLocal<>();

  /**
   * Retrieve the current RequestContext.
   *
   * @return Current RequestContext
   * @throws IllegalStateException If RequestContext is not set.
   */
  public static RequestContext get() {
    if (currentContext.get() == null) throw new IllegalStateException("Request context not set.");
    return currentContext.get();
  }

  /**
   * Set a new RequestContext (but it is not allowed to override an existing RequestContext).
   *
   * @param ctx New RequestContext
   * @return New RequestContext
   * @throws IllegalStateException If RequestContext is already set.
   */
  public static RequestContext set(RequestContext ctx) {
    if (currentContext.get() != null) throw new IllegalStateException("Request context already set.");
    currentContext.set(ctx);
    return ctx;
  }

  /**
   * Determine if RequestContext is set.
   *
   * @return True, if RequestContext is set.
   */
  public static boolean isSet() {
    return currentContext.get() != null;
  }

  @Override
  public void close() throws Exception {
    currentContext.remove();
  }

}
