package no.mnemonic.act.platform.rest.api.auth;

import no.mnemonic.act.platform.auth.properties.model.SubjectCredentials;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.common.auth.model.Credentials;
import org.jboss.resteasy.plugins.guice.RequestScoped;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

/**
 * A {@link CredentialsResolver} implementation which identifies a user based on the "ACT-User-ID" HTTP header.
 */
@RequestScoped
public class SubjectCredentialsResolver implements CredentialsResolver {

  private static final String ACT_USER_ID_HEADER = "ACT-User-ID";

  // Injectable by RESTEasy in a @RequestScoped context.
  private final HttpHeaders headers;

  @Inject
  public SubjectCredentialsResolver(HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  public Credentials getCredentials() {
    String header = headers.getHeaderString(ACT_USER_ID_HEADER);
    if (StringUtils.isBlank(header) || parseUserID(header) == -1) {
      // Rely on service to validate credentials and reject unauthenticated requests.
      return null;
    }

    return SubjectCredentials.builder()
            .setSubjectID(parseUserID(header))
            .build();
  }

  private long parseUserID(String userID) {
    try {
      return Long.parseUnsignedLong(userID);
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }
}
