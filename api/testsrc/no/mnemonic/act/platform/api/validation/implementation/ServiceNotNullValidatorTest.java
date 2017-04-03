package no.mnemonic.act.platform.api.validation.implementation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServiceNotNullValidatorTest {

  @Test
  public void testObjectIsNull() {
    assertFalse(new ServiceNotNullValidator().isValid(null, null));
  }

  @Test
  public void testObjectIsNotNull() {
    assertTrue(new ServiceNotNullValidator().isValid(new Object(), null));
  }

}
