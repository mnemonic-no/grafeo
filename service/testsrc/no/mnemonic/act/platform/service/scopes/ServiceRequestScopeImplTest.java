package no.mnemonic.act.platform.service.scopes;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ServiceRequestScopeImplTest {

  @Mock
  private Provider<Object> unscopedProvider;

  private final ServiceRequestScopeImpl scope = new ServiceRequestScopeImpl();
  private final Key<Object> key = Key.get(Object.class);
  private final Object seed = new Object();

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testEnterAndExitScope() {
    assertFalse(scope.isInsideScope());
    scope.enter();
    assertTrue(scope.isInsideScope());
    scope.exit();
    assertFalse(scope.isInsideScope());
  }

  @Test(expected = IllegalStateException.class)
  public void testEnterScopeTwice() {
    executeInsideScope(scope::enter);
  }

  @Test(expected = IllegalStateException.class)
  public void testExitNotEnteredScope() {
    scope.exit();
  }

  @Test
  public void testProviderWithSeededObject() {
    executeInsideScope(() -> {
      scope.seed(key, seed);
      assertSame(seed, scope.scope(key, unscopedProvider).get());
      verifyNoInteractions(unscopedProvider);
    });
  }

  @Test
  public void testProviderWithUnseededObject() {
    when(unscopedProvider.get()).thenReturn(seed);

    executeInsideScope(() -> {
      assertSame(seed, scope.scope(key, unscopedProvider).get());
      verify(unscopedProvider, times(1)).get();
    });
  }

  @Test
  public void testProviderWithUnseededObjectCachesResolvedObject() {
    when(unscopedProvider.get()).thenReturn(seed);

    executeInsideScope(() -> {
      assertSame(scope.scope(key, unscopedProvider).get(), scope.scope(key, unscopedProvider).get());
      verify(unscopedProvider, times(1)).get();
    });
  }

  @Test(expected = OutOfScopeException.class)
  public void testProviderNotEnteredScope() {
    scope.scope(key, unscopedProvider).get();
  }

  @Test(expected = OutOfScopeException.class)
  public void testSeedNotEnteredScope() {
    scope.seed(key, seed);
  }

  @Test(expected = IllegalStateException.class)
  public void testSeedObjectTwice() {
    executeInsideScope(() -> {
      scope.seed(key, seed);
      scope.seed(key, seed);
    });
  }

  private void executeInsideScope(Runnable test) {
    try {
      scope.enter();
      test.run();
    } finally {
      scope.exit();
    }
  }
}
