package no.mnemonic.act.platform.rest.swagger;

import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

public class SwaggerModelTransformerTest {

  @Test
  public void testTransform() {
    Operation operation = new Operation();
    Swagger swagger = createSwagger(operation);

    SwaggerModelTransformation transformation = mock(SwaggerModelTransformation.class);
    SwaggerModelTransformer transformer = SwaggerModelTransformer.builder()
            .addTransformation(transformation)
            .build();

    transformer.transform(swagger);

    InOrder inOrder = inOrder(transformation);
    inOrder.verify(transformation, times(1)).beforeHook(swagger);
    inOrder.verify(transformation, times(3)).transformOperation(swagger, operation);
    inOrder.verify(transformation, times(1)).afterHook(swagger);
  }

  private Swagger createSwagger(Operation operation) {
    Path path = new Path();
    path.setGet(operation);
    path.setPost(operation);
    path.setPut(operation);

    Swagger swagger = new Swagger();
    swagger.path("/test", path);

    return swagger;
  }

}
