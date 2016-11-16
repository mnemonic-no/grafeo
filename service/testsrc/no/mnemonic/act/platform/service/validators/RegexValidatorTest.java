package no.mnemonic.act.platform.service.validators;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RegexValidatorTest {

  @Test
  public void testValidateMatchesRegex() throws ValidatorConfigurationException {
    Validator validator = new RegexValidator("\\d-\\d");
    assertTrue(validator.validate("1-1"));
  }

  @Test
  public void testValidateDoesNotMatchRegex() throws ValidatorConfigurationException {
    Validator validator = new RegexValidator("\\d-\\d");
    assertFalse(validator.validate("a-a"));
    assertFalse(validator.validate(""));
    assertFalse(validator.validate(null));
  }

  @Test(expected = ValidatorConfigurationException.class)
  public void testRegexValidatorInitializedWithNullThrowsException() throws ValidatorConfigurationException {
    new RegexValidator(null);
  }

  @Test(expected = ValidatorConfigurationException.class)
  public void testRegexValidatorInitializedWithInvalidRegexThrowsException() throws ValidatorConfigurationException {
    new RegexValidator("[a-z");
  }

}
