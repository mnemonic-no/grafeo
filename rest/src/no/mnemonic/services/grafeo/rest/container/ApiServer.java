package no.mnemonic.services.grafeo.rest.container;

import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import no.mnemonic.services.grafeo.rest.resteasy.GuiceResteasyBootstrapServletContextListener;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class ApiServer implements LifecycleAspect {

  private static final Logger logger = Logging.getLogger(ApiServer.class);

  private final int port;
  private final GuiceResteasyBootstrapServletContextListener listener;
  private final Server server = new Server();

  @Inject
  public ApiServer(@Named("grafeo.api.server.port") String port, GuiceResteasyBootstrapServletContextListener listener) {
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

    // Configure Jetty: Remove 'server' header from response and set listen port.
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSendServerVersion(false);
    ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    connector.setPort(port);

    // Starting up Jetty to serve the REST API.
    server.addConnector(connector);
    server.setHandler(servletHandler);

    if (!LambdaUtils.tryTo(server::start, ex -> logger.error(ex, "Failed to start REST API."))) {
      throw new IllegalStateException("Failed to start REST API.");
    }
  }

  @Override
  public void stopComponent() {
    // Stop server to free up any resources.
    LambdaUtils.tryTo(server::stop, ex -> logger.error(ex, "Failed to cleanly shutdown REST API."));
  }

}
