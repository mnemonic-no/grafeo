package no.mnemonic.act.platform.service.validators;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
