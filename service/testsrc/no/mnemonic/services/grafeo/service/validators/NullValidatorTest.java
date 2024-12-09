package no.mnemonic.services.grafeo.service.validators;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NullValidatorTest {

  @Test
  public void testValidateOnlyAllowsNull() {
    Validator validator = new NullValidator();
    assertTrue(validator.validate(null));
    assertFalse(validator.validate(""));
    assertFalse(validator.validate(" "));
    assertFalse(validator.validate("test"));
  }

}
