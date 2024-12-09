package no.mnemonic.services.grafeo.service.validators;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultValidatorFactoryTest {

  @Test
  public void testGetReturnsTrueValidator() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    assertInstanceOf(TrueValidator.class, factory.get("TrueValidator", null));
  }

  @Test
  public void testGetReturnsRegexValidator() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    assertInstanceOf(RegexValidator.class, factory.get("RegexValidator", "pattern"));
  }

  @Test
  public void testGetReturnsNullValidator() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    assertInstanceOf(NullValidator.class, factory.get("NullValidator", null));
  }

  @Test
  public void testGetRegexValidatorWithInvalidPatternThrowsException() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    assertThrows(ValidatorConfigurationException.class, () -> factory.get("RegexValidator", "[a-z"));
  }

  @Test
  public void testGetWithInvalidValidatorThrowsException() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    assertThrows(ValidatorConfigurationException.class, () -> factory.get(null, "ignored"));
    assertThrows(ValidatorConfigurationException.class, () -> factory.get("", "ignored"));
    assertThrows(ValidatorConfigurationException.class, () -> factory.get(" ", "ignored"));
    assertThrows(ValidatorConfigurationException.class, () -> factory.get("Unknown", "ignored"));
  }

  @Test
  public void testExecuteGetTwiceReturnsSameInstance() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    Validator first = factory.get("RegexValidator", "pattern");
    Validator second = factory.get("RegexValidator", "pattern");
    assertSame(first, second);
  }

}
