package no.mnemonic.services.grafeo.auth.properties.module;

import no.mnemonic.services.grafeo.auth.properties.PropertiesBasedAccessController;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@Singleton
public class PropertiesBasedAccessControllerProvider implements Provider<PropertiesBasedAccessController> {

  @Inject
  @Named("grafeo.access.controller.properties.configuration.file")
  private String propertiesFile;
  @Inject
  @Named("grafeo.access.controller.properties.reload.interval")
  private String readInterval;

  // There should only be one instance of this access controller. Ever.
  // This is because the same instance will be bound to multiple interfaces (AccessController, OrganizationResolver, SubjectResolver).
  // This is also the reason why this provider is marked with @Singleton.
  private PropertiesBasedAccessController accessController;

  @Override
  public PropertiesBasedAccessController get() {
    if (accessController == null) {
      accessController = PropertiesBasedAccessController.builder()
              .setPropertiesFile(propertiesFile)
              .setReadingInterval(Long.parseLong(readInterval))
              .build();
    }

    return accessController;
  }

}
