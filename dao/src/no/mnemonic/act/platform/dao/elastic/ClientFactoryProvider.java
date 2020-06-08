package no.mnemonic.act.platform.dao.elastic;

import no.mnemonic.commons.utilities.collections.SetUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 * Provider class for instantiating a configured {@link ClientFactory}.
 */
public class ClientFactoryProvider implements Provider<ClientFactory> {

  @Inject
  @Named("act.elasticsearch.port")
  private String port;
  @Inject
  @Named("act.elasticsearch.contact.points")
  private String contactPoints;

  @Override
  public ClientFactory get() {
    return ClientFactory.builder()
            .setPort(Integer.parseInt(port))
            .setContactPoints(SetUtils.set(contactPoints.split(",")))
            .build();
  }

}
