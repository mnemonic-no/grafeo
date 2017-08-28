package no.mnemonic.act.platform.rest;

import com.google.common.reflect.ClassPath;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.act.platform.rest.container.ApiServer;
import no.mnemonic.act.platform.rest.swagger.SwaggerApiListingResource;
import no.mnemonic.services.common.documentation.swagger.ResultContainerTransformation;
import no.mnemonic.services.common.documentation.swagger.SwaggerModelTransformer;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
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
    try {
      ClassPath.from(ClassLoader.getSystemClassLoader())
              .getTopLevelClassesRecursive(packageName)
              .stream()
              .map(ClassPath.ClassInfo::load)
              .filter(c -> c.getAnnotation(annotationClass) != null)
              .forEach(this::bind);
    } catch (IOException e) {
      throw new RuntimeException("Could not read classes with SystemClassLoader.", e);
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
