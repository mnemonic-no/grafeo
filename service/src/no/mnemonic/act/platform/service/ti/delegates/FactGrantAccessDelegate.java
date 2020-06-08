package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.GrantFactAccessRequest;
import no.mnemonic.act.platform.auth.SubjectResolver;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactAclEntryRecord;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.AclEntryResponseConverter;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactRequestResolver;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.inject.Inject;
import java.util.UUID;

public class FactGrantAccessDelegate implements Delegate {

  private final TiSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final FactRequestResolver factRequestResolver;
  private final SubjectResolver subjectResolver;
  private final AclEntryResponseConverter aclEntryResponseConverter;

  @Inject
  public FactGrantAccessDelegate(TiSecurityContext securityContext,
                                 ObjectFactDao objectFactDao,
                                 FactRequestResolver factRequestResolver,
                                 SubjectResolver subjectResolver,
                                 AclEntryResponseConverter aclEntryResponseConverter) {
    this.securityContext = securityContext;
    this.objectFactDao = objectFactDao;
    this.factRequestResolver = factRequestResolver;
    this.subjectResolver = subjectResolver;
    this.aclEntryResponseConverter = aclEntryResponseConverter;
  }

  public AclEntry handle(GrantFactAccessRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    // Fetch Fact and verify that it exists.
    FactRecord fact = factRequestResolver.resolveFact(request.getFact());
    // Verify that user is allowed to access the Fact.
    securityContext.checkReadPermission(fact);
    // Verify that user is allowed to grant further access to the Fact.
    securityContext.checkPermission(TiFunctionConstants.grantThreatIntelFactAccess, fact.getOrganizationID());
    // It doesn't make sense to grant explicit access to a public Fact.
    if (fact.getAccessMode() == FactRecord.AccessMode.Public) {
      throw new InvalidArgumentException()
              .addValidationError("Cannot grant explicit access to a public Fact.", "fact.is.public", "fact", request.getFact().toString());
    }

    UUID subjectID = resolveSubjectID(request.getSubject());
    // Return an existing ACL entry or create a new entry for requested Subject.
    FactAclEntryRecord aclEntry = ObjectUtils.ifNull(findExistingAclEntry(fact, subjectID), () -> saveNewAclEntry(fact, subjectID));

    return aclEntryResponseConverter.apply(aclEntry);
  }

  private UUID resolveSubjectID(String idOrName) throws InvalidArgumentException {
    Subject subject;
    if (StringUtils.isUUID(idOrName)) {
      subject = subjectResolver.resolveSubject(UUID.fromString(idOrName));
    } else {
      subject = subjectResolver.resolveSubject(idOrName);
    }

    if (subject != null) return subject.getId();

    throw new InvalidArgumentException()
            .addValidationError("Subject does not exist.", "subject.not.exist", "subject", idOrName);
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
