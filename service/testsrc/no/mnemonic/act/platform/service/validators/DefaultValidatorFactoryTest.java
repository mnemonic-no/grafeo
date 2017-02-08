package no.mnemonic.act.platform.service.validators;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

  @Test(expected = IllegalArgumentException.class)
  public void testGetRegexValidatorWithInvalidPatternThrowsException() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    factory.get("RegexValidator", "[a-z");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWithNullValidatorThrowsException() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    factory.get(null, "ignored");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWithEmptyValidatorThrowsException() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    factory.get("", "ignored");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWithUnknownValidatorThrowsException() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    factory.get("Unknown", "ignored");
  }

  @Test
  public void testExecuteGetTwiceReturnsSameInstance() {
    DefaultValidatorFactory factory = new DefaultValidatorFactory();
    Validator first = factory.get("RegexValidator", "pattern");
    Validator second = factory.get("RegexValidator", "pattern");
    assertSame(first, second);
  }

}
