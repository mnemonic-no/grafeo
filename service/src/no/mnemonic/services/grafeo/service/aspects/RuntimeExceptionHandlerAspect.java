package no.mnemonic.services.grafeo.service.aspects;

import com.datastax.oss.driver.api.core.DriverTimeoutException;
import com.google.inject.matcher.Matchers;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.services.common.api.ServiceTimeOutException;
import no.mnemonic.services.grafeo.api.exceptions.UnexpectedAuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.UnhandledRuntimeException;
import no.mnemonic.services.grafeo.service.Service;
import org.aopalliance.intercept.MethodInvocation;

/**
 * The RuntimeExceptionHandlerAspect catches all unhandled RuntimeExceptions and replaces them with UnhandledRuntimeException.
 * This avoids exposing internal RuntimeExceptions outside the service and prevents deserialization errors in the REST layer.
 */
public class RuntimeExceptionHandlerAspect extends AbstractAspect {

  private static final Logger LOGGER = Logging.getLogger(RuntimeExceptionHandlerAspect.class);

  @Override
  protected void configure() {
    bindInterceptor(Matchers.subclassesOf(Service.class), matchServiceMethod(), this);
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    try {
      // Just execute service method which might or might not throw a RuntimeException.
      return invocation.proceed();
    } catch (UnexpectedAuthenticationFailedException | UnhandledRuntimeException | ServiceTimeOutException ex) {
      throw ex; // Allow well-known RuntimeExceptions to pass through.
    } catch (RuntimeException ex) {
      if (containsException(ex, DriverTimeoutException.class)) {
        LOGGER.warning(ex, "Received timeout from Cassandra client driver in service call %s().", invocation.getMethod().getName());
        // Timeouts from the Cassandra client driver are re-thrown as ServiceTimeOutException
        // such that REST clients will receive a 503 error code.
        throw new ServiceTimeOutException("Received timeout from Cassandra client driver.", invocation.getMethod().getDeclaringClass().getSimpleName());
      }

      String msg = String.format("Exception in service call %s(): %s", invocation.getMethod().getName(), ex.getMessage());
      LOGGER.error(ex, msg);
      // All other RuntimeExceptions are replaced with a generic UnhandledRuntimeException.
      throw new UnhandledRuntimeException(msg);
    }
  }

  private boolean containsException(Throwable ex, Class<? extends Throwable> target) {
    if (target.isInstance(ex)) return true;
    if (ex.getCause() == null) return false;

    return containsException(ex.getCause(), target);
  }
}
