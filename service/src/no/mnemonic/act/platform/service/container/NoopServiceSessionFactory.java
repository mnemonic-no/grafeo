package no.mnemonic.act.platform.service.container;

import no.mnemonic.services.common.api.ServiceSession;
import no.mnemonic.services.common.api.ServiceSessionFactory;

/**
 * Implementation of a {@link ServiceSessionFactory} providing a {@link NoopSession} instance.
 */
public class NoopServiceSessionFactory implements ServiceSessionFactory {

  private static final ServiceSession instance = new NoopSession();

  @Override
  public ServiceSession openSession() {
    return instance;
  }

  @Override
  public void close() throws Exception {
    instance.close();
  }

  /**
   * Implementation of a {@link ServiceSession} doing nothing.
   */
  private static class NoopSession implements ServiceSession {
    @Override
    public void close() {
      // NOOP
    }
  }
}
