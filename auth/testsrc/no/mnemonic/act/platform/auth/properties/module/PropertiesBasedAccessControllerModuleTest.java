package no.mnemonic.act.platform.auth.properties.module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import no.mnemonic.act.platform.auth.OrganizationResolver;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.services.common.auth.AccessController;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class PropertiesBasedAccessControllerModuleTest {

  @Test
  public void testCreatingAccessControllerReturnsSingletonInstance() {
    Injector injector = Guice.createInjector(new TestModule());
    AccessController accessController = injector.getInstance(AccessController.class);
    OrganizationResolver organizationResolver = injector.getInstance(OrganizationResolver.class);
    SubjectResolver subjectResolver = injector.getInstance(SubjectResolver.class);

    assertSame(accessController, organizationResolver);
    assertSame(accessController, subjectResolver);
  }

  private class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      install(new PropertiesBasedAccessControllerModule());
      bind(String.class).annotatedWith(Names.named("act.access.controller.properties.configuration.file")).toInstance("test.properties");
      bind(String.class).annotatedWith(Names.named("act.access.controller.properties.reload.interval")).toInstance("60000");
    }
  }

}
