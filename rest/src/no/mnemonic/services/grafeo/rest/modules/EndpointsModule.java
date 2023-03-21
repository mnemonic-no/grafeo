package no.mnemonic.services.grafeo.rest.modules;

import com.google.common.reflect.ClassPath;
import com.google.inject.AbstractModule;
import no.mnemonic.services.grafeo.rest.providers.ObjectMapperResolver;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;

/**
 * Module which configures all classes required for exposing the REST API.
 */
public class EndpointsModule extends AbstractModule {

  private static final String API_PACKAGE = "no.mnemonic.services.grafeo.rest.api";
  private static final String MAPPINGS_PACKAGE = "no.mnemonic.services.grafeo.rest.mappings";

  @Override
  protected void configure() {
    // Instruct RESTEasy to use a customized ObjectMapper (required to correctly serialize ResultStash).
    bind(ObjectMapperResolver.class);

    // Bind classes composing the REST API (endpoints and exception mappings).
    bindAnnotatedClasses(API_PACKAGE, Path.class);
    bindAnnotatedClasses(MAPPINGS_PACKAGE, Provider.class);
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
}
