package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.request.v1.GetFactAclRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.services.common.api.ResultSet;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FactGetAclDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final FactManager factManager;
  private final Function<FactAclEntity, AclEntry> aclEntryConverter;

  @Inject
  public FactGetAclDelegate(TiSecurityContext securityContext,
                            FactManager factManager,
                            Function<FactAclEntity, AclEntry> aclEntryConverter) {
    this.securityContext = securityContext;
    this.factManager = factManager;
    this.aclEntryConverter = aclEntryConverter;
  }

  public ResultSet<AclEntry> handle(GetFactAclRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactEntity fact = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    securityContext.checkReadPermission(fact);
    // Verify that user is allowed to view the Fact's ACL.
    securityContext.checkPermission(TiFunctionConstants.viewFactAccess, fact.getOrganizationID());
    // Fetch ACL for Fact.
    List<AclEntry> acl = factManager
            .fetchFactAcl(fact.getId())
            .stream()
            .map(aclEntryConverter)
            .collect(Collectors.toList());

    return StreamingResultSet.<AclEntry>builder()
            .setCount(acl.size())
            .setLimit(0)
            .setValues(acl)
            .build();
  }
}
