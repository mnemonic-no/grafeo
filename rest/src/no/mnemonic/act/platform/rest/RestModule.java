package no.mnemonic.act.platform.rest;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.act.platform.rest.container.ApiServer;
import org.reflections.Reflections;

import javax.ws.rs.Path;
import java.lang.annotation.Annotation;

public class RestModule extends AbstractModule {

  @Override
  protected void configure() {
    bindAnnotatedClasses("no.mnemonic.act.platform.rest.api", Path.class);
    bind(ApiServer.class).in(Scopes.SINGLETON);
  }

  private void bindAnnotatedClasses(String packageName, Class<? extends Annotation> annotationClass) {
    new Reflections(packageName)
            .getTypesAnnotatedWith(annotationClass)
            .forEach(this::bind);
  }

}
