package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.request.v1.GrantFactAccessRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.AccessMode;
import no.mnemonic.act.platform.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.UUID;

public class FactGrantAccessDelegate extends AbstractDelegate {

  public static FactGrantAccessDelegate create() {
    return new FactGrantAccessDelegate();
  }

  public AclEntry handle(GrantFactAccessRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactEntity fact = fetchExistingFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    TiSecurityContext.get().checkReadPermission(fact);
    // Verify that user is allowed to grant further access to the Fact.
    TiSecurityContext.get().checkPermission(TiFunctionConstants.grantFactAccess, fact.getOrganizationID());
    // It doesn't make sense to grant explicit access to a public Fact.
    if (fact.getAccessMode() == AccessMode.Public) {
      throw new InvalidArgumentException()
              .addValidationError("Cannot grant explicit access to a public Fact.", "fact.is.public", "fact", request.getFact().toString());
    }

    // Return an existing ACL entry or create a new entry for requested Subject.
    FactAclEntity aclEntry = ObjectUtils.ifNull(findExistingAclEntry(fact, request.getSubject()), () -> {
      FactAclEntity entry = saveNewAclEntry(fact, request.getSubject());
      // Also add entry to ElasticSearch to allow searching for Fact.
      reindexExistingFact(fact.getId(), d -> d.addAclEntry(entry.getSubjectID()));
      return entry;
    });

    return TiRequestContext.get().getAclEntryConverter().apply(aclEntry);
  }

  private FactAclEntity findExistingAclEntry(FactEntity fact, UUID subject) {
    return TiRequestContext.get().getFactManager().fetchFactAcl(fact.getId())
            .stream()
            .filter(entry -> entry.getSubjectID().equals(subject))
            .findFirst()
            .orElse(null);
  }

  private FactAclEntity saveNewAclEntry(FactEntity fact, UUID subject) {
    // TODO: Verify that subjects exist.
    FactAclEntity entry = new FactAclEntity()
            .setId(UUID.randomUUID()) // Need to provide client-generated ID.
            .setFactID(fact.getId())
            .setSourceID(TiSecurityContext.get().getCurrentUserID())
            .setSubjectID(subject)
            .setTimestamp(System.currentTimeMillis());

    return TiRequestContext.get().getFactManager().saveFactAclEntry(entry);
  }

}
