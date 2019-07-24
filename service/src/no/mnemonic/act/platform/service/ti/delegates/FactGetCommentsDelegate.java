package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.request.v1.GetFactCommentsRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactCommentEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FactGetCommentsDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactManager factManager;
  private final Function<FactCommentEntity, FactComment> factCommentConverter;

  @Inject
  public FactGetCommentsDelegate(TiSecurityContext securityContext,
                                 FactManager factManager,
                                 Function<FactCommentEntity, FactComment> factCommentConverter) {
    this.securityContext = securityContext;
    this.factManager = factManager;
    this.factCommentConverter = factCommentConverter;
  }

  public ResultSet<FactComment> handle(GetFactCommentsRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactEntity fact = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    securityContext.checkReadPermission(fact);
    // Verify that user is allowed to view the Fact's comments.
    securityContext.checkPermission(TiFunctionConstants.viewFactComments, fact.getOrganizationID());
    // Fetch comments for Fact and filter by 'before' and 'after' timestamps.
    List<FactComment> comments = factManager
            .fetchFactComments(fact.getId())
            .stream()
            .filter(comment -> request.getBefore() == null || comment.getTimestamp() < request.getBefore())
            .filter(comment -> request.getAfter() == null || comment.getTimestamp() > request.getAfter())
            .map(factCommentConverter)
            .collect(Collectors.toList());

    return StreamingResultSet.<FactComment>builder()
            .setCount(comments.size())
            .setLimit(0)
            .setValues(comments)
            .build();
  }
}
