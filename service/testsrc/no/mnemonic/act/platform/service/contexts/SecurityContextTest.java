package no.mnemonic.act.platform.service.contexts;

import org.junit.Test;

import static org.junit.Assert.*;

public class SecurityContextTest {

  @Test(expected = IllegalStateException.class)
  public void testGetContextNotSet() {
    SecurityContext.get();
  }

  @Test(expected = IllegalStateException.class)
  public void testSetContextTwice() throws Exception {
    try (SecurityContext ignored = SecurityContext.set(new SecurityContext())) {
      SecurityContext.set(new SecurityContext());
    }
  }

  @Test
  public void testSetAndGetContext() throws Exception {
    assertFalse(SecurityContext.isSet());

    try (SecurityContext ctx = SecurityContext.set(new SecurityContext())) {
      assertTrue(SecurityContext.isSet());
      assertSame(ctx, SecurityContext.get());
    }

    assertFalse(SecurityContext.isSet());
  }

}
