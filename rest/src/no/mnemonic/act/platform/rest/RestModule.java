package no.mnemonic.act.platform.rest;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.act.platform.rest.container.ApiServer;
import no.mnemonic.act.platform.rest.swagger.ResultStashTransformation;
import no.mnemonic.act.platform.rest.swagger.SwaggerApiListingResource;
import no.mnemonic.act.platform.rest.swagger.SwaggerModelTransformer;
import org.reflections.Reflections;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;

public class RestModule extends AbstractModule {

  private static final String API_PACKAGE = "no.mnemonic.act.platform.rest.api";
  private static final String MAPPINGS_PACKAGE = "no.mnemonic.act.platform.rest.mappings";

  @Override
  protected void configure() {
    bindAnnotatedClasses(API_PACKAGE, Path.class);
    bindAnnotatedClasses(MAPPINGS_PACKAGE, Provider.class);
    bindSwagger();
    bind(ApiServer.class).in(Scopes.SINGLETON);
  }

  private void bindAnnotatedClasses(String packageName, Class<? extends Annotation> annotationClass) {
    new Reflections(packageName)
            .getTypesAnnotatedWith(annotationClass)
            .forEach(this::bind);
  }

  private void bindSwagger() {
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setBasePath("/");
    beanConfig.setResourcePackage(API_PACKAGE);
    beanConfig.setScan(true);

    SwaggerModelTransformer transformer = SwaggerModelTransformer.builder()
            .addTransformation(new ResultStashTransformation(ResultStash.class, "data"))
            .build();

    bind(SwaggerApiListingResource.class).in(Scopes.SINGLETON);
    bind(SwaggerSerializers.class).in(Scopes.SINGLETON);
    bind(SwaggerModelTransformer.class).toInstance(transformer);
  }

}
