package no.mnemonic.services.grafeo.rest.providers;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.jboss.resteasy.plugins.interceptors.CorsFilter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Provider
public class CorsFilterFeature implements Feature {

  private final Set<String> allowedOrigins;

  @Inject
  public CorsFilterFeature(@Named("grafeo.api.cors.allowed.origins") String allowedOrigins) {
    this.allowedOrigins = SetUtils.set(allowedOrigins.split(","));
  }

  @Override
  public boolean configure(FeatureContext context) {
    // For a reference about CORS see https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS.
    CorsFilter filter = new CorsFilter();

    // Add all allowed origins configured in application.properties file.
    allowedOrigins.forEach(origin -> filter.getAllowedOrigins().add(origin));
    // Allow the following HTTP methods.
    filter.setAllowedMethods("GET, POST, PUT, DELETE");
    // Allow the following headers (in addition to CORS-safelisted headers).
    filter.setAllowedHeaders("ACT-User-ID, Content-Type, Grafeo-User-ID");
    // Allow the browser to send credentials, i.e. cookies.
    filter.setAllowCredentials(true);
    // Allow the browser to cache results of preflight requests for 1 minute.
    filter.setCorsMaxAge((int) TimeUnit.MINUTES.toSeconds(1));

    context.register(filter);
    return true;
  }

}
