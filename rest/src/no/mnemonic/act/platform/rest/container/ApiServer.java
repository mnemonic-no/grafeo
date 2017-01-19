package no.mnemonic.act.platform.rest.container;

import no.mnemonic.commons.component.LifecycleAspect;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import javax.inject.Inject;
import javax.inject.Named;

public class ApiServer implements LifecycleAspect {

  private final int port;
  private final GuiceResteasyBootstrapServletContextListener listener;
  private Server server;

  @Inject
  public ApiServer(@Named("api.server.port") String port, GuiceResteasyBootstrapServletContextListener listener) {
    this.port = Integer.parseInt(port);
    this.listener = listener;
  }

  @Override
  public void startComponent() {
    // Initialize servlet using RESTEasy and it's Guice bridge.
    // The listener must be injected by the same Guice module which also binds the REST endpoints.
    ServletContextHandler servletHandler = new ServletContextHandler();
    servletHandler.addEventListener(listener);
    servletHandler.addServlet(HttpServletDispatcher.class, "/*");

    // Starting up Jetty to serve the REST API.
    server = new Server(port);
    server.setHandler(servletHandler);

    try {
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stopComponent() {
    try {
      // Stop server to free up any resources.
      if (server != null) server.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
