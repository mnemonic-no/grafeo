package no.mnemonic.services.grafeo.rest.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.services.grafeo.rest.api.auth.CredentialsResolver;
import no.mnemonic.services.grafeo.rest.api.auth.SubjectCredentialsResolver;
import no.mnemonic.services.grafeo.rest.container.ApiServer;
import no.mnemonic.services.grafeo.rest.providers.CorsFilterFeature;
import no.mnemonic.services.grafeo.rest.resteasy.GuiceResteasyContextDataProvider;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

/**
 * Module which configures the REST API including Swagger documentation of the ThreatIntelligenceService.
 */
public class TiRestModule extends AbstractModule {

  private boolean skipDefaultCredentialsResolver;
  private boolean skipDefaultCorsFilter;

  @Override
  protected void configure() {
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

    // Make all injectable JAX-RS interfaces available by utilizing ResteasyContext.
    bind(HttpHeaders.class).toProvider(new GuiceResteasyContextDataProvider<>(HttpHeaders.class));
    bind(Request.class).toProvider(new GuiceResteasyContextDataProvider<>(Request.class));
    bind(UriInfo.class).toProvider(new GuiceResteasyContextDataProvider<>(UriInfo.class));
    bind(SecurityContext.class).toProvider(new GuiceResteasyContextDataProvider<>(SecurityContext.class));

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
