package no.mnemonic.act.platform.rest.swagger;

import io.swagger.models.Operation;
import io.swagger.models.Swagger;

/**
 * Transformation to transform a Swagger model. See SwaggerModelTransformer.
 */
public interface SwaggerModelTransformation {

  /**
   * Called in the beginning of a transformation process. Can be used to perform some initial set-up.
   * <p>
   * Does nothing by default.
   *
   * @param swagger Swagger model to transform
   */
  default void beforeHook(Swagger swagger) {
    // NOOP
  }

  /**
   * Called for each API operation defined in a Swagger model. Performs the actual transformation.
   * <p>
   * Does nothing by default.
   *
   * @param swagger   Swagger model to transform
   * @param operation API operation to transform
   */
  default void transformOperation(Swagger swagger, Operation operation) {
    // NOOP
  }

  /**
   * Called at the very end of a transformation process. Can be used to perform some final clean-up.
   * <p>
   * Does nothing by default.
   *
   * @param swagger Swagger model to transform
   */
  default void afterHook(Swagger swagger) {
    // NOOP
  }

}
