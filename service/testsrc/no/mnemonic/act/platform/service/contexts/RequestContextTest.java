package no.mnemonic.act.platform.service.contexts;

import org.junit.Test;

import static org.junit.Assert.*;

public class RequestContextTest {

  @Test(expected = IllegalStateException.class)
  public void testGetContextNotSet() {
    RequestContext.get();
  }

  @Test(expected = IllegalStateException.class)
  public void testSetContextTwice() throws Exception {
    try (RequestContext ignored = RequestContext.set(new RequestContext())) {
      RequestContext.set(new RequestContext());
    }
  }

  @Test
  public void testSetAndGetContext() throws Exception {
    assertFalse(RequestContext.isSet());

    try (RequestContext ctx = RequestContext.set(new RequestContext())) {
      assertTrue(RequestContext.isSet());
      assertSame(ctx, RequestContext.get());
    }

    assertFalse(RequestContext.isSet());
  }

  @Test
  public void testClearExistingContext() {
    RequestContext ctx = RequestContext.set(new RequestContext());
    assertTrue(RequestContext.isSet());
    RequestContext oldCtx = RequestContext.clear();
    assertFalse(RequestContext.isSet());
    assertSame(ctx, oldCtx);
  }

  @Test
  public void testClearNonExistingContext() {
    assertNull(RequestContext.clear());
    assertFalse(RequestContext.isSet());
  }

}
