package no.mnemonic.act.platform.rest.swagger;

import io.swagger.models.ModelImpl;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SwaggerRefPropertyFactoryTest {

  @Test
  public void testCreateObjectRefProperty() {
    ModelImpl innerModel = new ModelImpl();
    innerModel.setName("Test");

    Property property = SwaggerRefPropertyFactory.create(SwaggerRefPropertyFactory.PropertyContainerType.NONE, innerModel);
    assertTrue(property instanceof RefProperty);
    RefProperty refProperty = (RefProperty) property;
    assertEquals(innerModel.getName(), refProperty.getSimpleRef());
  }

  @Test
  public void testCreateArrayRefProperty() {
    ModelImpl innerModel = new ModelImpl();
    innerModel.setName("Test");

    Property property = SwaggerRefPropertyFactory.create(SwaggerRefPropertyFactory.PropertyContainerType.LIST, innerModel);
    assertTrue(property instanceof ArrayProperty);
    ArrayProperty arrayProperty = (ArrayProperty) property;
    RefProperty refProperty = (RefProperty) arrayProperty.getItems();
    assertEquals(innerModel.getName(), refProperty.getSimpleRef());
  }

}
