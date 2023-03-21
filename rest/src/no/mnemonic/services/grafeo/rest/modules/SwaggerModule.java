package no.mnemonic.services.grafeo.rest.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.mnemonic.services.common.documentation.swagger.ResultContainerTransformation;
import no.mnemonic.services.common.documentation.swagger.SwaggerModelTransformer;
import no.mnemonic.services.grafeo.rest.api.ResultStash;
import no.mnemonic.services.grafeo.rest.swagger.SwaggerApiListingResource;

/**
 * Module which configures Swagger used for API documentation.
 */
public class SwaggerModule extends AbstractModule {

  private static final String API_PACKAGE = "no.mnemonic.services.grafeo.rest.api";

  @Override
  protected void configure() {
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
