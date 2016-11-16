package no.mnemonic.act.platform.entity.handlers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IdentityHandlerTest {

  @Test
  public void testEncode() {
    EntityHandler handler = new IdentityHandler();
    assertEquals("test", handler.encode("test"));
  }

  @Test
  public void testDecode() {
    EntityHandler handler = new IdentityHandler();
    assertEquals("test", handler.decode("test"));
  }

  @Test
  public void testEncodeDecodeReturnsOriginalValue() {
    EntityHandler handler = new IdentityHandler();
    assertEquals("test", handler.decode(handler.encode("test")));
  }

}
