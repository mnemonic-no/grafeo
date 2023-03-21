package no.mnemonic.services.grafeo.service.validators;

import org.junit.Test;

import static org.junit.Assert.*;

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

  @Test
  public void testRegexValidatorInitializedWithoutParameterThrowsException() throws ValidatorConfigurationException {
    assertThrows(ValidatorConfigurationException.class, () -> new RegexValidator(null));
    assertThrows(ValidatorConfigurationException.class, () -> new RegexValidator(""));
    assertThrows(ValidatorConfigurationException.class, () -> new RegexValidator(" "));
  }

  @Test
  public void testRegexValidatorInitializedWithInvalidRegexThrowsException() throws ValidatorConfigurationException {
    assertThrows(ValidatorConfigurationException.class, () -> new RegexValidator("[a-z"));
  }

}
