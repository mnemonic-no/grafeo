package no.mnemonic.act.platform.rest.mappings;

import no.mnemonic.act.platform.rest.api.ResultStash;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

  @Override
  public Response toResponse(ConstraintViolationException ex) {
    ResultStash.Builder builder = ResultStash.builder().setStatus(Response.Status.PRECONDITION_FAILED);

    for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
      // Add all violations to the response.
      String value = ObjectUtils.ifNotNull(v.getInvalidValue(), Object::toString, "NULL");
      builder.addFieldError(v.getMessage(), v.getMessageTemplate(), printPropertyPath(v.getPropertyPath()), value);
    }

    return builder.buildResponse();
  }

  private String printPropertyPath(Path path) {
    if (path == null) return "UNKNOWN";

    String propertyPath = "";
    Path.Node parameterNode = null;
    // Construct string representation of property path.
    // This will strip away any other nodes added by RESTEasy (method, parameter, ...).
    for (Path.Node node : path) {
      if (node.getKind() == ElementKind.PARAMETER) {
        parameterNode = node;
      }

      if (node.getKind() == ElementKind.PROPERTY) {
        if (!propertyPath.isEmpty()) {
          propertyPath += ".";
        }

        propertyPath += node;
      }
    }

    if (propertyPath.isEmpty() && parameterNode != null) {
      // No property path constructed, assume this is a validation failure on a request parameter.
      propertyPath = parameterNode.toString();
    }

    return propertyPath;
  }

}
