package no.mnemonic.act.platform.api.request.v1;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.BeforeClass;

import javax.validation.*;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public abstract class AbstractRequestTest {

  private static final String VALIDATION_MAPPINGS = "ValidationMappings.xml";

  private static ObjectMapper mapper;
  private static Validator validator;

  @BeforeClass
  public static void setUp() {
    // Initialize object mapper for testing. With this configuration we save a lot of quotes and escaping when creating JSON strings.
    mapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();
    // Initialize validator for testing including custom mappings.
    validator = Validation.byDefaultProvider()
            .configure()
            .addMapping(AbstractRequestTest.class.getClassLoader().getResourceAsStream(VALIDATION_MAPPINGS))
            .buildValidatorFactory()
            .getValidator();
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
