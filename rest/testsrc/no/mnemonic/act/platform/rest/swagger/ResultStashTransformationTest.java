package no.mnemonic.act.platform.rest.swagger;

import io.swagger.models.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.commons.utilities.collections.MapUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResultStashTransformationTest {

  private final ResultStashTransformation transformation = new ResultStashTransformation(ResultStash.class, "data");

  @Test
  public void testBeforeHookInjectsResultStashModel() {
    Swagger swagger = new Swagger();

    transformation.beforeHook(swagger);

    assertFalse(MapUtils.isEmpty(swagger.getDefinitions()));
    assertNotNull(swagger.getDefinitions().get("ResultStash"));
  }

  @Test
  public void testTransformOperationForSingleObject() {
    Operation operation = createOperation(createRefProperty());
    Swagger swagger = createSwagger(operation);

    transformation.beforeHook(swagger);
    transformation.transformOperation(swagger, operation);

    // Test that the schema points to the injected ResultStash model definition.
    RefProperty resultStashRefProperty = (RefProperty) swagger.getPath("/test").getGet().getResponses().get("200").getSchema();
    assertEquals("ResultStash-TestObject-single", resultStashRefProperty.getSimpleRef());

    // Test that the injected ResultStash model definition exists.
    Model refModelDefinition = swagger.getDefinitions().get("ResultStash-TestObject-single");
    assertNotNull(refModelDefinition);

    // Test that the 'data' property points to the correct model definition.
    RefProperty dataRefProperty = (RefProperty) refModelDefinition.getProperties().get("data");
    assertEquals("TestObject", dataRefProperty.getSimpleRef());
  }

  @Test
  public void testTransformOperationForObjectList() {
    Operation operation = createOperation(createArrayProperty());
    Swagger swagger = createSwagger(operation);

    transformation.beforeHook(swagger);
    transformation.transformOperation(swagger, operation);

    // Test that the schema points to the injected ResultStash model definition.
    RefProperty resultStashRefProperty = (RefProperty) swagger.getPath("/test").getGet().getResponses().get("200").getSchema();
    assertEquals("ResultStash-TestObject-list", resultStashRefProperty.getSimpleRef());

    // Test that the injected ResultStash model definition exists.
    Model refModelDefinition = swagger.getDefinitions().get("ResultStash-TestObject-list");
    assertNotNull(refModelDefinition);

    // Test that the 'data' property points to the correct model definition.
    ArrayProperty dataArrayProperty = (ArrayProperty) refModelDefinition.getProperties().get("data");
    assertEquals("TestObject", ((RefProperty) dataArrayProperty.getItems()).getSimpleRef());
  }

  private Swagger createSwagger(Operation operation) {
    ModelImpl model = new ModelImpl();
    model.setName("TestObject");

    Path path = new Path();
    path.setGet(operation);

    Swagger swagger = new Swagger();
    swagger.path("/test", path);
    swagger.addDefinition("TestObject", model);

    return swagger;
  }

  private Operation createOperation(Property schema) {
    Response response = new Response();
    response.schema(schema);

    Operation operation = new Operation();
    operation.addResponse("200", response);

    return operation;
  }

  private Property createRefProperty() {
    RefProperty property = new RefProperty();
    property.set$ref("#/definitions/TestObject");

    return property;
  }

  private Property createArrayProperty() {
    ArrayProperty property = new ArrayProperty();
    property.setItems(createRefProperty());

    return property;
  }

}
