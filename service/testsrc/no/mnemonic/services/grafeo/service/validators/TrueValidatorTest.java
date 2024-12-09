package no.mnemonic.services.grafeo.service.validators;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrueValidatorTest {

  @Test
  public void testValidateAlwaysReturnsTrue() {
    Validator validator = new TrueValidator();
    assertTrue(validator.validate("test"));
    assertTrue(validator.validate(""));
    assertTrue(validator.validate(null));
  }

}
