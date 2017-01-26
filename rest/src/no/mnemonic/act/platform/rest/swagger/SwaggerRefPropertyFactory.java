package no.mnemonic.act.platform.rest.swagger;

import io.swagger.models.ModelImpl;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import no.mnemonic.commons.utilities.StringUtils;

public class SwaggerRefPropertyFactory {

  public enum PropertyContainerType {
    NONE("single"),
    LIST("list");

    private final String fieldName;

    PropertyContainerType(String name) {
      fieldName = name;
    }

    @Override
    public String toString() {
      return fieldName;
    }
  }

  public static Property create(PropertyContainerType type, ModelImpl innerModel) {
    switch (type) {
      case NONE:
        return createObjectRefProperty(innerModel);
      case LIST:
        return createArrayRefProperty(innerModel);
      default:
        throw new UnsupportedOperationException("Unsupported PropertyContainerType " + type);
    }
  }

  private static RefProperty createObjectRefProperty(ModelImpl innerModel) {
    return new RefProperty(!StringUtils.isEmpty(innerModel.getReference()) ? innerModel.getReference() : innerModel.getName());
  }

  private static ArrayProperty createArrayRefProperty(ModelImpl innerModel) {
    ArrayProperty arrayProperty = new ArrayProperty();
    RefProperty itemsProperty = createObjectRefProperty(innerModel);
    arrayProperty.setItems(itemsProperty);
    return arrayProperty;
  }

}
