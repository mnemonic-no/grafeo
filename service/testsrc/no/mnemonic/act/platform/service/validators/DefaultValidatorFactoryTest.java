package no.mnemonic.act.platform.service.validators;

import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultValidatorFactoryTest {

  @Test
  public void testGetReturnsTrueValidator() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    assertTrue(factory.get("TrueValidator", null) instanceof TrueValidator);
  }

  @Test
  public void testGetReturnsRegexValidator() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    assertTrue(factory.get("RegexValidator", "pattern") instanceof RegexValidator);
  }

  @Test
  public void testGetReturnsNullValidator() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    assertTrue(factory.get("NullValidator", null) instanceof NullValidator);
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
