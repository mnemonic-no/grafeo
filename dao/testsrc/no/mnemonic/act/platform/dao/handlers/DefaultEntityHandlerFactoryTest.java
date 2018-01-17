package no.mnemonic.act.platform.dao.handlers;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DefaultEntityHandlerFactoryTest {

  @Test
  public void testGetReturnsIdentityHandler() {
    DefaultEntityHandlerFactory factory = new DefaultEntityHandlerFactory();
    assertTrue(factory.get("IdentityHandler", "ignored") instanceof IdentityHandler);
    assertTrue(factory.get("IdentityHandler", "") instanceof IdentityHandler);
    assertTrue(factory.get("IdentityHandler", null) instanceof IdentityHandler);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWithNullHandlerThrowsException() {
    DefaultEntityHandlerFactory factory = new DefaultEntityHandlerFactory();
    factory.get(null, "ignored");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWithEmptyHandlerThrowsException() {
    DefaultEntityHandlerFactory factory = new DefaultEntityHandlerFactory();
    factory.get("", "ignored");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWithUnknownHandlerThrowsException() {
    DefaultEntityHandlerFactory factory = new DefaultEntityHandlerFactory();
    factory.get("Unknown", "ignored");
  }

  @Test
  public void testExecuteGetTwiceReturnsSameInstance() {
    DefaultEntityHandlerFactory factory = new DefaultEntityHandlerFactory();
    EntityHandler first = factory.get("IdentityHandler", "ignored");
    EntityHandler second = factory.get("IdentityHandler", "ignored");
    assertSame(first, second);
  }

}
