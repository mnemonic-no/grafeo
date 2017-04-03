package no.mnemonic.act.platform.rest.mappings;

import com.fasterxml.jackson.databind.JsonMappingException;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

class MapperUtils {

  /**
   * Helper function to construct a string representation of a property path from a JsonMappingException
   * (similar to a property path from a ConstraintViolationException).
   *
   * @param exception JsonMappingException
   * @return String representation of property path
   */
  static String printPropertyPath(JsonMappingException exception) {
    if (CollectionUtils.isEmpty(exception.getPath())) return "UNKNOWN";

    String propertyPath = "";
    for (JsonMappingException.Reference ref : exception.getPath()) {
      if (ref.getFieldName() != null) {
        if (!propertyPath.isEmpty()) {
          propertyPath += ".";
        }

        propertyPath += ref.getFieldName();
      } else {
        propertyPath += String.format("[%d]", ref.getIndex());
      }
    }

    return propertyPath;
  }

}
