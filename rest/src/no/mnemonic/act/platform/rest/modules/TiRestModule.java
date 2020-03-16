package no.mnemonic.act.platform.rest.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.rest.api.auth.CredentialsResolver;
import no.mnemonic.act.platform.rest.api.auth.SubjectCredentialsResolver;
import no.mnemonic.act.platform.rest.container.ApiServer;
import no.mnemonic.act.platform.rest.providers.CorsFilterFeature;
import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;

/**
 * Module which configures the REST API including Swagger documentation of the ThreatIntelligenceService.
 */
public class TiRestModule extends AbstractModule {

  private boolean skipDefaultCredentialsResolver;
  private boolean skipDefaultCorsFilter;

  @Override
  protected void configure() {
    // Instruct RESTEasy to handle @RequestScoped objects.
    install(new RequestScopeModule());

    // Install modules for REST endpoints and Swagger.
    install(new EndpointsModule());
    install(new SwaggerModule());

    if (!skipDefaultCredentialsResolver) {
      // Bind class to resolve credentials sent to service back-end.
      bind(CredentialsResolver.class).to(SubjectCredentialsResolver.class);
    }

    if (!skipDefaultCorsFilter) {
      // Register default CORS filter.
      bind(CorsFilterFeature.class);
    }

    // Bind class serving the REST API via HTTP.
    bind(ApiServer.class).in(Scopes.SINGLETON);
  }

  /**
   * Instruct the module to omit the default credentials resolver implementation. In this case an alternative
   * implementation must be configured in Guice.
   *
   * @return this
   */
  public TiRestModule withoutDefaultCredentialsResolver() {
    this.skipDefaultCredentialsResolver = true;
    return this;
  }

  /**
   * Instruct the module to omit the default CORS filter.
   *
   * @return this
   */
  public TiRestModule withoutDefaultCorsFilter() {
    this.skipDefaultCorsFilter = true;
    return this;
  }
}
