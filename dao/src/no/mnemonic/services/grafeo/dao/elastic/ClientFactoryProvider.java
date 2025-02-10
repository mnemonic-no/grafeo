package no.mnemonic.services.grafeo.dao.elastic;

import no.mnemonic.commons.utilities.collections.SetUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

/**
 * Provider class for instantiating a configured {@link ClientFactory}.
 */
public class ClientFactoryProvider implements Provider<ClientFactory> {

  @Inject
  @Named("grafeo.elasticsearch.port")
  private String port;
  @Inject
  @Named("grafeo.elasticsearch.contact.points")
  private String contactPoints;

  @Override
  public ClientFactory get() {
    return ClientFactory.builder()
            .setPort(Integer.parseInt(port))
            .setContactPoints(SetUtils.set(contactPoints.split(",")))
            .build();
  }

}
