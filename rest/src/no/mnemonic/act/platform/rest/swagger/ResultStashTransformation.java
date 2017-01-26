package no.mnemonic.act.platform.rest.swagger;

import io.swagger.converter.ModelConverters;
import io.swagger.models.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.util.Json;
import no.mnemonic.commons.utilities.collections.MapUtils;

import java.util.Map;

public class ResultStashTransformation implements SwaggerModelTransformation {

  private final Class resultStashClass;
  private final String resultStashName;
  private final String resultStashDataProperty;

  public ResultStashTransformation(Class resultStashClass, String resultStashDataProperty) {
    this.resultStashClass = resultStashClass;
    this.resultStashName = resultStashClass.getSimpleName();
    this.resultStashDataProperty = resultStashDataProperty;
  }

  @Override
  public void beforeHook(Swagger swagger) {
    // Read ResultStash and inject its model definition into swagger.
    Map<String, Model> resultStashModels = ModelConverters.getInstance().read(Json.mapper().constructType(resultStashClass));
    swagger.setDefinitions(MapUtils.concatenate(swagger.getDefinitions(), resultStashModels));
  }

  @Override
  public void transformOperation(Swagger swagger, Operation operation) {
    for (Response response : operation.getResponses().values()) {
      if (response == null || response.getSchema() == null) {
        continue;
      }
      response.setSchema(evaluateSchema(swagger, response.getSchema()));
    }
  }

  private Property evaluateSchema(Swagger swagger, Property schema) {
    Property newSchemaModel = null;
    if (schema instanceof RefProperty) {
      // Only response type defined in @ApiOperation:
      // e.g. response = UserInfo.class
      newSchemaModel = wrapModelWithResultStashModel(swagger, (RefProperty) schema, SwaggerRefPropertyFactory.PropertyContainerType.NONE);
    } else if (schema instanceof ArrayProperty) {
      // Both response type and container "list" defined in @ApiOperation:
      // e.g. response = UserInfo.class, responseContainer = list
      Property itemsProperty = ((ArrayProperty) schema).getItems();
      if (itemsProperty instanceof RefProperty) {
        newSchemaModel = wrapModelWithResultStashModel(swagger, (RefProperty) itemsProperty, SwaggerRefPropertyFactory.PropertyContainerType.LIST);
      }
    }
    return newSchemaModel != null ? newSchemaModel : schema;
  }

  private Property wrapModelWithResultStashModel(Swagger swagger, RefProperty schema, SwaggerRefPropertyFactory.PropertyContainerType containerType) {
    Model model = swagger.getDefinitions().get(schema.getSimpleRef());
    if (model == null || !(model instanceof ModelImpl)) {
      return null;
    }

    ModelImpl newSchemaModel = injectResultStashModel(swagger, (ModelImpl) model, containerType);
    return newSchemaModel != null ? new RefProperty(newSchemaModel.getName()) : null;
  }

  private ModelImpl injectResultStashModel(Swagger swagger, ModelImpl innerModel, SwaggerRefPropertyFactory.PropertyContainerType containerType) {
    // Only inject ResultStash model once.
    if (innerModel.getName().startsWith(resultStashName)) {
      return null;
    }

    // Create ResultStash model instance based on ResultStash definition.
    Model model = swagger.getDefinitions().get(resultStashName);
    if (model == null || !(model instanceof ModelImpl)) {
      return null;
    }
    ModelImpl resultStashModel = (ModelImpl) model.clone();

    // Set unique name of ResultStash model per container type to avoid overwriting documentation.
    resultStashModel.setName(resultStashName + "-" + innerModel.getName() + "-" + containerType);
    // Inject innerModel into 'data' property.
    Property dataProperty = SwaggerRefPropertyFactory.create(containerType, innerModel);
    dataProperty.setRequired(true);
    dataProperty.setDescription(createDataPropertyDescription(containerType));
    resultStashModel.getProperties().put(resultStashDataProperty, dataProperty);
    // Put ResultStash model with injected innerModel into Swagger.
    swagger.addDefinition(resultStashModel.getName(), resultStashModel);

    return resultStashModel;
  }

  private String createDataPropertyDescription(SwaggerRefPropertyFactory.PropertyContainerType containerType) {
    switch (containerType) {
      case NONE:
        return "Contains a single result";
      case LIST:
        return "Contains an array of results";
      default:
        return "";
    }
  }

}
