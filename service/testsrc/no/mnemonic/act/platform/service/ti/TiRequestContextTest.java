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
  public void testFactSearchManagerNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getFactSearchManager();
  }

  @Test(expected = RuntimeException.class)
  public void testRetractionHandlerNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getRetractionHandler();
  }

  @Test(expected = RuntimeException.class)
  public void testValidatorFactoryNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getValidatorFactory();
  }

  @Test(expected = RuntimeException.class)
  public void testObjectTypeConverterNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getObjectTypeConverter();
  }

  @Test(expected = RuntimeException.class)
  public void testFactTypeConverterNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getFactTypeConverter();
  }

  @Test(expected = RuntimeException.class)
  public void testObjectConverterNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getObjectConverter();
  }

  @Test(expected = RuntimeException.class)
  public void testFactConverterNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getFactConverter();
  }

  @Test(expected = RuntimeException.class)
  public void testAclEntryConverterNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getAclEntryConverter();
  }

  @Test(expected = RuntimeException.class)
  public void testFactCommentConverterNotSetInContextThrowsException() {
    TiRequestContext.builder().build().getFactCommentConverter();
  }

}
