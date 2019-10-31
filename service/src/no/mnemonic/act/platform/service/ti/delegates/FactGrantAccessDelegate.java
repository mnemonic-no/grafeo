package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.request.v1.GrantFactAccessRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.AclEntryConverter;
import no.mnemonic.act.platform.service.ti.resolvers.FactResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.inject.Inject;
import java.util.UUID;

public class FactGrantAccessDelegate extends AbstractDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final FactResolver factResolver;
  private final AclEntryConverter aclEntryConverter;

  @Inject
  public FactGrantAccessDelegate(TiSecurityContext securityContext,
                                 ObjectFactDao objectFactDao,
                                 FactResolver factResolver,
                                 AclEntryConverter aclEntryConverter) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.factResolver = factResolver;
    this.aclEntryConverter = aclEntryConverter;
  }

  public AclEntry handle(GrantFactAccessRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactRecord fact = factResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    securityContext.checkReadPermission(fact);
    // Verify that user is allowed to grant further access to the Fact.
    securityContext.checkPermission(TiFunctionConstants.grantFactAccess, fact.getOrganizationID());
    // It doesn't make sense to grant explicit access to a public Fact.
    if (fact.getAccessMode() == FactRecord.AccessMode.Public) {
      throw new InvalidArgumentException()
              .addValidationError("Cannot grant explicit access to a public Fact.", "fact.is.public", "fact", request.getFact().toString());
    }

    // Return an existing ACL entry or create a new entry for requested Subject.
    FactAclEntryRecord aclEntry = ObjectUtils.ifNull(findExistingAclEntry(fact, request.getSubject()), () -> saveNewAclEntry(fact, request.getSubject()));

    return aclEntryConverter.apply(aclEntry);
  }

  private FactAclEntryRecord findExistingAclEntry(FactRecord fact, UUID subject) {
    return ListUtils.list(fact.getAcl())
            .stream()
            .filter(entry -> entry.getSubjectID().equals(subject))
            .findFirst()
            .orElse(null);
  }

  private FactAclEntryRecord saveNewAclEntry(FactRecord fact, UUID subject) {
    FactAclEntryRecord entry = new FactAclEntryRecord()
            .setId(UUID.randomUUID())
            .setOriginID(securityContext.getCurrentUserID())
            .setSubjectID(subject)
            .setTimestamp(System.currentTimeMillis());

    return objectFactDao.storeFactAclEntry(fact, entry);
  }
}
