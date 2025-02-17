package no.mnemonic.services.grafeo.rest.api.auth;

import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.common.auth.model.Credentials;
import no.mnemonic.services.grafeo.auth.properties.model.SubjectCredentials;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * A {@link CredentialsResolver} implementation which identifies a user based on the "Grafeo-User-ID" HTTP header.
 */
public class SubjectCredentialsResolver implements CredentialsResolver {

  // The "ACT-User-ID" header is deprecated in favour of the "Grafeo-User-ID" header.
  // For now both are supported to not break existing clients. This will be removed in the future!
  private static final String ACT_USER_ID_HEADER = "ACT-User-ID";
  private static final String GRAFEO_USER_ID_HEADER = "Grafeo-User-ID";

  private final HttpHeaders headers;

  @Inject
  public SubjectCredentialsResolver(HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  public Credentials getCredentials() {
    // Prioritize "Grafeo-User-ID" header.
    String grafeoHeader = headers.getHeaderString(GRAFEO_USER_ID_HEADER);
    if (isHeaderSet(grafeoHeader)) return createCredentials(grafeoHeader);

    // Still support "ACT-User-ID" header for backwards compatibility.
    String actHeader = headers.getHeaderString(ACT_USER_ID_HEADER);
    if (isHeaderSet(actHeader)) return createCredentials(actHeader);

    // Rely on service to validate credentials and reject unauthenticated requests.
    return null;
  }

  private SubjectCredentials createCredentials(String header) {
    return SubjectCredentials.builder()
            .setSubjectID(parseUserID(header))
            .build();
  }

  private boolean isHeaderSet(String header) {
    return !StringUtils.isBlank(header) && parseUserID(header) != -1;
  }

  private long parseUserID(String userID) {
    try {
      return Long.parseUnsignedLong(userID);
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }
}
