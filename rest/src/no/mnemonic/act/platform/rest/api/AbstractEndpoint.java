package no.mnemonic.act.platform.rest.api;

import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.auth.properties.model.SubjectCredentials;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.services.common.api.ResultSet;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

public abstract class AbstractEndpoint {

  private static final String ACT_USER_ID_HEADER = "ACT-User-ID";

  @Context
  private HttpHeaders headers;

  protected RequestHeader getHeader() {
    return RequestHeader.builder()
            .setCredentials(resolveSubjectCredentials())
            .build();
  }

  protected <T> Response buildResponse(T model) {
    return ResultStash.builder()
            .setData(model)
            .buildResponse();
  }

  protected <T> Response buildResponse(ResultSet<T> result) {
    return ResultStash.builder()
            .setLimit(result.getLimit())
            .setCount(result.getCount())
            .setData(result)
            .buildResponse();
  }

  private SubjectCredentials resolveSubjectCredentials() {
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
