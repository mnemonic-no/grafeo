package no.mnemonic.services.grafeo.service.implementation.delegates;

import com.google.inject.Guice;
import com.google.inject.Module;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class DelegateProviderTest {

  private final DelegateProvider provider = new DelegateProvider(Guice.createInjector((Module) binder -> {
  }));

  @Test
  public void testGetDelegateInstance() {
    TestDelegate delegate1 = provider.get(TestDelegate.class);
    TestDelegate delegate2 = provider.get(TestDelegate.class);
    assertNotNull(delegate1);
    assertNotNull(delegate2);
    assertNotSame(delegate1, delegate2);
  }

  private static class TestDelegate implements Delegate {
  }
}
