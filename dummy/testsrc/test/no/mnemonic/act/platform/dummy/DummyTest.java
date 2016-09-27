package test.no.mnemonic.act.platform.dummy;

import no.mnemonic.act.platform.dummy.Dummy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DummyTest {

  @Test
  public void testDoSomething() {
    assertEquals("Hello World!", Dummy.doSomething());
  }

}
