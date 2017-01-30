package no.mnemonic.act.platform.rest.swagger;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.util.List;

/**
 * This class can be used to transform a Swagger model in order to customize it. In order to do so a number of
 * SwaggerModelTransformations are applied to a Swagger model. Those transformations are applied in the following way:
 * <p>
 * 1. All 'beforeHook()' methods are executed first.
 * This can be used, for example, to inject own model definitions into the Swagger model.
 * 2. For each API operation defined in the Swagger model 'transformOperation()' is applied for all transformations.
 * This can be used to transform a specific API operation.
 * 3. At the very end all 'afterHook()' methods are executed.
 * This can be used to apply some final transformations to the whole Swagger model.
 */
public class SwaggerModelTransformer {

  private final List<SwaggerModelTransformation> transformations;

  private SwaggerModelTransformer(List<SwaggerModelTransformation> transformations) {
    this.transformations = transformations;
  }

  public Swagger transform(Swagger swagger) {
    transformations.forEach(t -> t.beforeHook(swagger));

    for (Path path : swagger.getPaths().values()) {
      for (Operation operation : path.getOperations()) {
        transformations.forEach(t -> t.transformOperation(swagger, operation));
      }
    }

    transformations.forEach(t -> t.afterHook(swagger));

    return swagger;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<SwaggerModelTransformation> transformations;

    private Builder() {
    }

    public SwaggerModelTransformer build() {
      return new SwaggerModelTransformer(this.transformations);
    }

    public Builder setTransformations(List<SwaggerModelTransformation> transformations) {
      this.transformations = transformations;
      return this;
    }

    public Builder addTransformation(SwaggerModelTransformation transformation) {
      this.transformations = ListUtils.addToList(this.transformations, transformation);
      return this;
    }
  }

}
