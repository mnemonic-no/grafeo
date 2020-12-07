package no.mnemonic.act.platform.service.aspects;

import com.google.inject.matcher.Matchers;
import no.mnemonic.act.platform.api.exceptions.UnexpectedAuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.UnhandledRuntimeException;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.services.common.api.ServiceTimeOutException;
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
      String msg = String.format("Exception in service call %s(): %s", invocation.getMethod().getName(), ex.getMessage());
      LOGGER.error(ex, msg);

      // All other RuntimeExceptions are replaced with a generic UnhandledRuntimeException.
      throw new UnhandledRuntimeException(msg);
    }
  }

}
