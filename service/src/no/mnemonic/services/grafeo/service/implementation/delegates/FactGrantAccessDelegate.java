package no.mnemonic.services.grafeo.service.implementation.delegates;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.grafeo.api.exceptions.AccessDeniedException;
import no.mnemonic.services.grafeo.api.exceptions.AuthenticationFailedException;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.api.exceptions.ObjectNotFoundException;
import no.mnemonic.services.grafeo.api.model.v1.AclEntry;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.api.request.v1.GrantFactAccessRequest;
import no.mnemonic.services.grafeo.auth.SubjectSPI;
import no.mnemonic.services.grafeo.dao.api.ObjectFactDao;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.service.implementation.FunctionConstants;
import no.mnemonic.services.grafeo.service.implementation.GrafeoSecurityContext;
import no.mnemonic.services.grafeo.service.implementation.converters.response.AclEntryResponseConverter;
import no.mnemonic.services.grafeo.service.implementation.resolvers.request.FactRequestResolver;

import javax.inject.Inject;
import java.util.UUID;

public class FactGrantAccessDelegate implements Delegate {

  private final GrafeoSecurityContext securityContext;
  private final ObjectFactDao objectFactDao;
  private final FactRequestResolver factRequestResolver;
  private final SubjectSPI subjectResolver;
  private final AclEntryResponseConverter aclEntryResponseConverter;

  @Inject
  public FactGrantAccessDelegate(GrafeoSecurityContext securityContext,
                                 ObjectFactDao objectFactDao,
                                 FactRequestResolver factRequestResolver,
                                 SubjectSPI subjectResolver,
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
    securityContext.checkPermission(FunctionConstants.grantThreatIntelFactAccess, fact.getOrganizationID());
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

  private UUID resolveSubjectID(String idOrName) throws AuthenticationFailedException, InvalidArgumentException {
    Subject subject;
    try {
      if (StringUtils.isUUID(idOrName)) {
        subject = subjectResolver.resolveSubject(securityContext.getCredentials(), UUID.fromString(idOrName));
      } else {
        subject = subjectResolver.resolveSubject(securityContext.getCredentials(), idOrName);
      }
    } catch (InvalidCredentialsException ex) {
      throw new AuthenticationFailedException("Could not authenticate user: " + ex.getMessage());
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
