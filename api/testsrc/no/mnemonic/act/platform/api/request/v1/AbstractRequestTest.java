package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;

import javax.validation.*;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public abstract class AbstractRequestTest {

  private static ObjectMapper mapper;
  private static Validator validator;

  @BeforeClass
  public static void setUp() {
    // Initialize object mapper for testing.
    mapper = new ObjectMapper();
    // With this configuration we save a lot of quotes and escaping when creating JSON strings.
    mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    // Initialize validator for testing.
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  protected ObjectMapper getMapper() {
    return mapper;
  }

  protected Validator getValidator() {
    return validator;
  }

  protected <T> void assertPropertyInvalid(Set<ConstraintViolation<T>> violations, String property) {
    boolean invalid = false;
    for (ConstraintViolation v : violations) {
      for (Path.Node n : v.getPropertyPath()) {
        if (n.getKind() == ElementKind.PROPERTY && n.getName().equals(property)) {
          invalid = true;
          break;
        }
      }
    }

    assertTrue(invalid);
  }

}
