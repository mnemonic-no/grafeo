package no.mnemonic.services.grafeo.service.scopes;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceRequestScopeImplTest {

  @Mock
  private Provider<Object> unscopedProvider;

  private final ServiceRequestScopeImpl scope = new ServiceRequestScopeImpl();
  private final Key<Object> key = Key.get(Object.class);
  private final Object seed = new Object();

  @Test
  public void testEnterAndExitScope() {
    assertFalse(scope.isInsideScope());
    scope.enter();
    assertTrue(scope.isInsideScope());
    scope.exit();
    assertFalse(scope.isInsideScope());
  }

  @Test
  public void testEnterScopeTwice() {
    assertThrows(IllegalStateException.class, () -> executeInsideScope(scope::enter));
  }

  @Test
  public void testExitNotEnteredScope() {
    assertThrows(IllegalStateException.class, scope::exit);
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

  @Test
  public void testProviderNotEnteredScope() {
    assertThrows(OutOfScopeException.class, () -> scope.scope(key, unscopedProvider).get());
  }

  @Test
  public void testSeedNotEnteredScope() {
    assertThrows(OutOfScopeException.class, () -> scope.seed(key, seed));
  }

  @Test
  public void testSeedObjectTwice() {
    assertThrows(IllegalStateException.class, () -> executeInsideScope(() -> {
      scope.seed(key, seed);
      scope.seed(key, seed);
    }));
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
