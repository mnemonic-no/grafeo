package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.request.v1.GetFactCommentsRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;

import java.util.List;
import java.util.stream.Collectors;

public class FactGetCommentsDelegate extends AbstractDelegate {

  public static FactGetCommentsDelegate create() {
    return new FactGetCommentsDelegate();
  }

  public ResultSet<FactComment> handle(GetFactCommentsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactEntity fact = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    TiSecurityContext.get().checkReadPermission(fact);
    // Fetch comments for Fact and filter by 'before' and 'after' timestamps.
    List<FactComment> comments = TiRequestContext.get().getFactManager()
            .fetchFactComments(fact.getId())
            .stream()
            .filter(comment -> request.getBefore() == null || comment.getTimestamp() < request.getBefore())
            .filter(comment -> request.getAfter() == null || comment.getTimestamp() > request.getAfter())
            .map(TiRequestContext.get().getFactCommentConverter())
            .collect(Collectors.toList());

    return ResultSet.<FactComment>builder()
            .setCount(comments.size())
            .setLimit(0)
            .setValues(comments)
            .build();
  }

}
