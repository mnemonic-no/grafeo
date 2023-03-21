package no.mnemonic.services.grafeo.auth.properties.module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.grafeo.auth.OrganizationSPI;
import no.mnemonic.services.grafeo.auth.SubjectSPI;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class PropertiesBasedAccessControllerModuleTest {

  @Test
  public void testCreatingAccessControllerReturnsSingletonInstance() {
    Injector injector = Guice.createInjector(new TestModule());
    AccessController accessController = injector.getInstance(AccessController.class);
    OrganizationSPI organizationResolver = injector.getInstance(OrganizationSPI.class);
    SubjectSPI subjectResolver = injector.getInstance(SubjectSPI.class);

    assertSame(accessController, organizationResolver);
    assertSame(accessController, subjectResolver);
  }

  private static class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      install(new PropertiesBasedAccessControllerModule());
      bind(String.class).annotatedWith(Names.named("act.access.controller.properties.configuration.file")).toInstance("test.properties");
      bind(String.class).annotatedWith(Names.named("act.access.controller.properties.reload.interval")).toInstance("60000");
      bind(String.class).annotatedWith(Names.named("act.access.controller.properties.service.account.user.id")).toInstance("1");
    }
  }

}
