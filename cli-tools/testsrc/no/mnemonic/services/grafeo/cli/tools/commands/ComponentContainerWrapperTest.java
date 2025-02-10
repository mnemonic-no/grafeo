package no.mnemonic.services.grafeo.cli.tools.commands;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import no.mnemonic.commons.component.LifecycleAspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ComponentContainerWrapperTest {

  @Mock
  private LifecycleAspect lifecycleAspect;

  private ComponentContainerWrapper wrapper;
  private Executable executable;

  @BeforeEach
  public void setUp() {
    wrapper = new ComponentContainerWrapper(new ExecutableModule());
    executable = wrapper.getBean(Executable.class);
  }

  @Test
  public void testOkIsCalled() {
    assertFalse(executable.isCalled());
    assertDoesNotThrow(() -> wrapper.execute(executable::ok));
    assertTrue(executable.isCalled());
  }

  @Test
  public void testErrorIsCalled() {
    assertThrows(IllegalStateException.class, () -> wrapper.execute(executable::error));
  }

  @Test
  public void testLifecycleAspectIsCalled() {
    assertDoesNotThrow(() -> wrapper.execute(executable::ok));
    verify(lifecycleAspect).startComponent();
    verify(lifecycleAspect).stopComponent();
  }

  @Test
  public void testLifecycleAspectIsCalledOnError() {
    assertThrows(IllegalStateException.class, () -> wrapper.execute(executable::error));
    verify(lifecycleAspect).startComponent();
    verify(lifecycleAspect).stopComponent();
  }

  private class ExecutableModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(LifecycleAspect.class).toInstance(lifecycleAspect);
      bind(Executable.class).in(Scopes.SINGLETON);
    }
  }

  @Singleton
  private static class Executable implements LifecycleAspect {

    private final AtomicBoolean called = new AtomicBoolean(false);
    private final LifecycleAspect lifecycleAspect;

    @Inject
    public Executable(LifecycleAspect lifecycleAspect) {
      this.lifecycleAspect = lifecycleAspect;
    }

    @Override
    public void startComponent() {
      lifecycleAspect.startComponent();
    }

    @Override
    public void stopComponent() {
      lifecycleAspect.stopComponent();
    }

    public void ok() {
      called.set(true);
    }

    public void error() {
      throw new IllegalStateException();
    }

    public boolean isCalled() {
      return called.get();
    }
  }
}
