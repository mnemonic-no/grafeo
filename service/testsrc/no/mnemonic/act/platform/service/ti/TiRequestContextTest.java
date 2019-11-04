package no.mnemonic.act.platform.service.ti;

import org.junit.Test;

public class TiRequestContextTest {

  @Test(expected = RuntimeException.class)
  public void testObjectManagerNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getObjectManager();
  }

  @Test(expected = RuntimeException.class)
  public void testFactManagerNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getFactManager();
  }

  @Test(expected = RuntimeException.class)
  public void testValidatorFactoryNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getValidatorFactory();
  }

}
