package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.request.v1.GetFactAclRequest;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;

import java.util.List;
import java.util.stream.Collectors;

public class FactGetAclDelegate extends AbstractDelegate {

  public static FactGetAclDelegate create() {
    return new FactGetAclDelegate();
  }

  public ResultSet<AclEntry> handle(GetFactAclRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactEntity fact = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    TiSecurityContext.get().checkReadPermission(fact);
    // Verify that user is allowed to view the Fact's ACL.
    TiSecurityContext.get().checkPermission(TiFunctionConstants.viewFactAccess, fact.getOrganizationID());
    // Fetch ACL for Fact.
    List<AclEntry> acl = TiRequestContext.get().getFactManager()
            .fetchFactAcl(fact.getId())
            .stream()
            .map(TiRequestContext.get().getAclEntryConverter())
            .collect(Collectors.toList());

    return ResultSet.<AclEntry>builder()
            .setCount(acl.size())
            .setLimit(0)
            .setValues(acl)
            .build();
  }

}
