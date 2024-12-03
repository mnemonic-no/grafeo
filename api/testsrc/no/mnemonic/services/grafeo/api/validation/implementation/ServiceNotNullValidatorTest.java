package no.mnemonic.services.grafeo.api.validation.implementation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
