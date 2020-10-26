package no.mnemonic.act.platform.auth.properties;

import no.mnemonic.act.platform.auth.ServiceAccountSPI;
import no.mnemonic.act.platform.auth.properties.model.SubjectCredentials;
import no.mnemonic.services.common.auth.model.Credentials;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * {@link ServiceAccountSPI} implementation for the PropertiesBasedAccessController which identifies the
 * service account by reading the user ID from a configuration file.
 */
public class PropertiesBasedServiceAccountCredentialsResolver implements ServiceAccountSPI {

  private final long serviceAccountUserID;

  @Inject
  public PropertiesBasedServiceAccountCredentialsResolver(
          @Named("act.access.controller.properties.service.account.user.id") long serviceAccountUserID) {
    this.serviceAccountUserID = serviceAccountUserID;
  }

  @Override
  public Credentials get() {
    return SubjectCredentials.builder()
            .setSubjectID(serviceAccountUserID)
            .build();
  }
}
