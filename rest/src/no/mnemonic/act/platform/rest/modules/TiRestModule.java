package no.mnemonic.act.platform.rest.modules;

import com.google.common.reflect.ClassPath;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.act.platform.rest.api.auth.CredentialsResolver;
import no.mnemonic.act.platform.rest.api.auth.SubjectCredentialsResolver;
import no.mnemonic.act.platform.rest.container.ApiServer;
import no.mnemonic.act.platform.rest.swagger.SwaggerApiListingResource;
import no.mnemonic.services.common.documentation.swagger.ResultContainerTransformation;
import no.mnemonic.services.common.documentation.swagger.SwaggerModelTransformer;
import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;

/**
 * Module which configures the REST API including Swagger documentation of the ThreatIntelligenceService.
 */
public class TiRestModule extends AbstractModule {

  private static final String API_PACKAGE = "no.mnemonic.act.platform.rest.api";
  private static final String MAPPINGS_PACKAGE = "no.mnemonic.act.platform.rest.mappings";
  private static final String PROVIDERS_PACKAGE = "no.mnemonic.act.platform.rest.providers";

  @Override
  protected void configure() {
    // Instruct RESTEasy to handle @RequestScoped objects.
    install(new RequestScopeModule());

    // Bind classes composing the REST API (including Swagger documentation).
    bindAnnotatedClasses(API_PACKAGE, Path.class);
    bindAnnotatedClasses(MAPPINGS_PACKAGE, Provider.class);
    bindAnnotatedClasses(PROVIDERS_PACKAGE, Provider.class);
    bindSwagger();

    // Bind class to resolve credentials sent to service back-end.
    bind(CredentialsResolver.class).to(SubjectCredentialsResolver.class);

    // Bind class serving the REST API via HTTP.
    bind(ApiServer.class).in(Scopes.SINGLETON);
  }

  private void bindAnnotatedClasses(String packageName, Class<? extends Annotation> annotationClass) {
    try {
      ClassPath.from(ClassLoader.getSystemClassLoader())
              .getTopLevelClassesRecursive(packageName)
              .stream()
              .map(ClassPath.ClassInfo::load)
              .filter(c -> c.getAnnotation(annotationClass) != null)
              .forEach(this::bind);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not read classes with SystemClassLoader.", ex);
    }
  }

  private void bindSwagger() {
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setBasePath("/");
    beanConfig.setResourcePackage(API_PACKAGE);
    beanConfig.setScan(true);

    SwaggerModelTransformer transformer = SwaggerModelTransformer.builder()
            .addTransformation(new ResultContainerTransformation(ResultStash.class, "data"))
            .build();

    bind(SwaggerApiListingResource.class).in(Scopes.SINGLETON);
    bind(SwaggerSerializers.class).in(Scopes.SINGLETON);
    bind(SwaggerModelTransformer.class).toInstance(transformer);
  }
}
