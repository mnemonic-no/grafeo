package no.mnemonic.act.platform.rest;

import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.ws.rs.core.Response;
import java.util.Collection;

public abstract class AbstractEndpoint {

  protected RequestHeader getHeader() {
    // Just return an empty header until we can do something meaningful here.
    return new RequestHeader();
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
            .setSize(ObjectUtils.ifNotNull(result.getValues(), Collection::size, 0))
            .setData(result.getValues())
            .buildResponse();
  }

}
