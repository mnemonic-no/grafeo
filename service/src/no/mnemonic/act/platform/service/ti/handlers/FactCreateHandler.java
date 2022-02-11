package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.auth.OrganizationSPI;
import no.mnemonic.act.platform.auth.SubjectSPI;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.OriginEntity;
import no.mnemonic.act.platform.dao.facade.helpers.FactRecordHasher;
import no.mnemonic.act.platform.service.providers.LockProvider;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.converters.response.FactResponseConverter;
import no.mnemonic.act.platform.service.validators.Validator;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.auth.InvalidCredentialsException;

import javax.inject.Inject;
import java.util.*;

import static no.mnemonic.act.platform.service.ti.ThreatIntelligenceServiceImpl.GLOBAL_NAMESPACE;
import static no.mnemonic.act.platform.service.ti.helpers.FactHelper.withAcl;
import static no.mnemonic.act.platform.service.ti.helpers.FactHelper.withComment;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;

public class FactCreateHandler {

  private static final String LOCK_REGION = FactCreateHandler.class.getSimpleName();
  private static final float ORIGIN_DEFAULT_TRUST = 0.8f;
  private static final Map<FactRecord.AccessMode, Integer> ACCESS_MODE_ORDER = MapUtils.map(
          T(FactRecord.AccessMode.Public, 0),
          T(FactRecord.AccessMode.RoleBased, 1),
          T(FactRecord.AccessMode.Explicit, 2)
  );

  private final TiSecurityContext securityContext;
  private final SubjectSPI subjectResolver;
  private final OrganizationSPI organizationResolver;
  private final OriginManager originManager;
  private final ValidatorFactory validatorFactory;
  private final ObjectFactDao objectFactDao;
  private final FactResponseConverter factResponseConverter;
  private final LockProvider lockProvider;

  @Inject
  public FactCreateHandler(TiSecurityContext securityContext,
                           SubjectSPI subjectResolver,
                           OrganizationSPI organizationResolver,
                           OriginManager originManager,
                           ValidatorFactory validatorFactory,
                           ObjectFactDao objectFactDao,
                           FactResponseConverter factResponseConverter,
                           LockProvider lockProvider) {
    this.securityContext = securityContext;
    this.subjectResolver = subjectResolver;
    this.organizationResolver = organizationResolver;
    this.originManager = originManager;
    this.validatorFactory = validatorFactory;
    this.objectFactDao = objectFactDao;
    this.factResponseConverter = factResponseConverter;
    this.lockProvider = lockProvider;
  }

  /**
   * Resolve an Organization by ID or name. Falls back to the Organization of the given Origin if no 'idOrName' is provided.
   * If neither 'idOrName' is provided nor the given Origin is linked to an Organization the current user's Organization
   * will be returned.
   *
   * @param idOrName ID or name of Organization (can be null)
   * @param fallback Origin used as fallback (can be null)
   * @return Resolved Organization (will never be null)
   * @throws AccessDeniedException         Thrown if the current user does not have access to the resolved Organization
   * @throws AuthenticationFailedException Thrown if the current user could not be authenticated
   * @throws InvalidArgumentException      Thrown if an Organization cannot be resolved
   */
  public Organization resolveOrganization(String idOrName, OriginEntity fallback)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    if (idOrName != null) {
      // If an Organization was provided in the request fetch it.
      Organization organization = fetchOrganization(idOrName);
      // Make sure that user has access to the resolved Organization.
      securityContext.checkReadPermission(organization);
      return organization;
    }

    if (fallback != null && fallback.getOrganizationID() != null) {
      // If an Organization wasn't provided in the request fall back to the Origin's Organization. If the user has access to
      // the Origin (which must be checked by the caller of this method!) then one also has access to the Origin's Organization.
      return fetchOrganization(fallback.getOrganizationID().toString());
    }

    // As a last resort fall back to the current user's Organization.
    return resolveCurrentUserAffiliation();
  }

  /**
   * Resolve an Origin by ID or name. Falls back to the current user if no 'idOrName' is provided.
   * This will create a new Origin for the current user if no Origin exists yet.
   *
   * @param idOrName ID or name of Origin (can be null)
   * @return Resolved Origin (will never be null)
   * @throws AccessDeniedException         Thrown if the current user does not have access to the resolved Origin
   * @throws AuthenticationFailedException Thrown if the current user could not be authenticated
   * @throws InvalidArgumentException      Thrown if 'idOrName' is provided and the Origin does not exist, or the resolved Origin is deleted
   */
  public OriginEntity resolveOrigin(String idOrName)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    if (idOrName != null) {
      // If an Origin was provided in the request fetch it or throw an exception.
      OriginEntity origin = fetchOrigin(idOrName);
      if (origin != null) {
        // Make sure that user has access to the resolved Origin.
        securityContext.checkReadPermission(origin);
        return origin;
      }

      throw new InvalidArgumentException()
              .addValidationError("Origin does not exist.", "origin.not.exist", "origin", idOrName);
    }

    // If an Origin wasn't provided in the request fall back to the current user as Origin.
    // Note that the current user always has access to its own Origin.
    OriginEntity origin = fetchOrigin(securityContext.getCurrentUserID().toString());
    if (origin != null) return origin;

    // Create an Origin for the current user if it doesn't exist yet.
    Subject currentUser = wrapInvalidCredentialsException(() -> subjectResolver.resolveCurrentUser(securityContext.getCredentials()));
    return originManager.saveOrigin(new OriginEntity()
            .setId(securityContext.getCurrentUserID())
            .setNamespaceID(GLOBAL_NAMESPACE) // For now everything will just be part of the global namespace.
            .setOrganizationID(ObjectUtils.ifNotNull(currentUser.getOrganization(), Organization.Info::getId))
            .setName(createOriginName(currentUser))
            .setTrust(ORIGIN_DEFAULT_TRUST)
            .setType(OriginEntity.Type.User)
    );
  }

  /**
   * Resolve Subjects which should be added to a new Fact's ACL. Subjects will be identified by ID or name.
   * If one or more Subjects cannot be resolved an InvalidArgumentException is thrown.
   *
   * @param acl ID or name of Subjects (can be null)
   * @return Resolved Subjects (will never be null)
   * @throws AuthenticationFailedException Thrown if the current user could not be authenticated
   * @throws InvalidArgumentException      Thrown if one or more Subjects cannot be resolved
   */
  public List<Subject> resolveSubjects(List<String> acl) throws AuthenticationFailedException, InvalidArgumentException {
    if (CollectionUtils.isEmpty(acl)) return Collections.emptyList();

    List<Subject> result = new ArrayList<>();
    InvalidArgumentException unresolved = new InvalidArgumentException();

    // Loop through all entries and fetch Subjects by id or name.
    for (int i = 0; i < acl.size(); i++) {
      String idOrName = acl.get(i);
      Subject subject = wrapInvalidCredentialsException(() -> {
        if (StringUtils.isUUID(idOrName)) {
          return subjectResolver.resolveSubject(securityContext.getCredentials(), UUID.fromString(idOrName));
        } else {
          return subjectResolver.resolveSubject(securityContext.getCredentials(), idOrName);
        }
      });

      if (subject != null) {
        result.add(subject);
      } else {
        // Collect all validation errors instead of failing on the first.
        unresolved.addValidationError("Subject does not exist.", "subject.not.exist", String.format("acl[%d]", i), idOrName);
      }
    }

    // Fail the the request if one or more Subjects couldn't be resolved.
    if (unresolved.hasErrors()) throw unresolved;
    return result;
  }

  /**
   * Resolve AccessMode from a request and verify that it is not less restrictive than the AccessMode from another Fact.
   * Falls back to AccessMode from referenced Fact if requested AccessMode is not given.
   *
   * @param referencedFact      Fact to validate AccessMode against
   * @param requestedAccessMode Requested AccessMode (can be null)
   * @return Resolved AccessMode
   * @throws InvalidArgumentException Thrown if requested AccessMode is less restrictive than AccessMode of referenced Fact
   */
  public FactRecord.AccessMode resolveAccessMode(FactRecord referencedFact, AccessMode requestedAccessMode) throws InvalidArgumentException {
    if (referencedFact == null || referencedFact.getAccessMode() == null) return null;

    // If no AccessMode provided fall back to the AccessMode from the referenced Fact.
    FactRecord.AccessMode mode = ObjectUtils.ifNotNull(requestedAccessMode, m -> FactRecord.AccessMode.valueOf(m.name()), referencedFact.getAccessMode());

    // The requested AccessMode of a new Fact should not be less restrictive than the AccessMode of the referenced Fact.
    if (ACCESS_MODE_ORDER.get(mode) < ACCESS_MODE_ORDER.get(referencedFact.getAccessMode())) {
      throw new InvalidArgumentException()
              .addValidationError(String.format("Requested AccessMode cannot be less restrictive than AccessMode of Fact with id = %s.", referencedFact.getId()),
                      "access.mode.too.wide", "accessMode", mode.name());
    }

    return mode;
  }

  /**
   * Saves a fact into permanent storage. If the fact exists already, the fact is refreshed.
   *
   * @param fact       The fact to save
   * @param comment    A comment to the record
   * @param subjectIds List of subject ids for the record
   * @return The fact that was stored
   */
  public Fact saveFact(FactRecord fact, String comment, List<UUID> subjectIds) {
    FactRecord effectiveFact;

    // Synchronize storing new Facts and refreshing existing Facts using the Fact's unique hash value. If two
    // simultaneous requests try to add the same Fact one request will be delayed and will just refresh the Fact
    // added by the other request.
    try (LockProvider.Lock ignored = lockProvider.acquireLock(LOCK_REGION, FactRecordHasher.toHash(fact))) {
      FactRecord existingFact = resolveExistingFact(fact);

      effectiveFact = existingFact != null ? existingFact : fact;
      effectiveFact = withAcl(effectiveFact, securityContext.getCurrentUserID(), subjectIds);
      effectiveFact = withComment(effectiveFact, comment);

      if (existingFact != null) {
        // Refresh existing Fact.
        effectiveFact = objectFactDao.refreshFact(effectiveFact);
      } else {
        // Or create a new Fact.
        effectiveFact = objectFactDao.storeFact(effectiveFact);
      }
    }

    return factResponseConverter.apply(effectiveFact);
  }

  /**
   * Assert that a Fact value is valid according to a FactType's validator.
   *
   * @param type  FactType to validate against
   * @param value Value to validate
   * @throws InvalidArgumentException Thrown if value is not valid for the given FactType
   */
  public void assertValidFactValue(FactTypeEntity type, String value) throws InvalidArgumentException {
    Validator validator = validatorFactory.get(type.getValidator(), type.getValidatorParameter());

    if (!validator.validate(value)) {
      throw new InvalidArgumentException()
              .addValidationError("Fact did not pass validation against FactType.", "fact.not.valid", "value", value);
    }
  }

  private FactRecord resolveExistingFact(FactRecord newFact) {
    // Fetch any Fact which is logically the same as the Fact to create,
    // apply permission check and return existing Fact if accessible.
    return objectFactDao.retrieveExistingFact(newFact)
            .filter(securityContext::hasReadPermission)
            .orElse(null);
  }

  private Organization fetchOrganization(String idOrName) throws AuthenticationFailedException, InvalidArgumentException {
    Organization organization = wrapInvalidCredentialsException(() -> {
      if (StringUtils.isUUID(idOrName)) {
        return organizationResolver.resolveOrganization(securityContext.getCredentials(), UUID.fromString(idOrName));
      } else {
        return organizationResolver.resolveOrganization(securityContext.getCredentials(), idOrName);
      }
    });

    if (organization != null) return organization;

    throw new InvalidArgumentException()
            .addValidationError("Organization does not exist.", "organization.not.exist", "organization", idOrName);
  }

  private OriginEntity fetchOrigin(String idOrName) throws InvalidArgumentException {
    OriginEntity origin;
    if (StringUtils.isUUID(idOrName)) {
      origin = originManager.getOrigin(UUID.fromString(idOrName));
    } else {
      origin = originManager.getOrigin(idOrName);
    }

    // It's not allowed to use a deleted Origin for a new Fact.
    if (origin != null && SetUtils.set(origin.getFlags()).contains(OriginEntity.Flag.Deleted)) {
      throw new InvalidArgumentException()
              .addValidationError("Not allowed to create a Fact using a deleted Origin.", "invalid.origin.deleted", "origin", idOrName);
    }

    return origin;
  }

  private Organization resolveCurrentUserAffiliation() throws AuthenticationFailedException, InvalidArgumentException {
    Organization affiliation = wrapInvalidCredentialsException(() -> organizationResolver.resolveCurrentUserAffiliation(securityContext.getCredentials()));
    if (affiliation != null) return affiliation;

    // That the current user doesn't have an affiliation is an unlikely edge case, but better handle it, just in case.
    throw new InvalidArgumentException()
            .addValidationError("Unable to determine Organization for current user. Please specify Organization.",
                    "current.user.organization.not.exist", "organization", "N/A");
  }

  private String createOriginName(Subject currentUser) {
    // There can't exist two Origins with the same name. Avoid collisions by generating a unique name if necessary.
    OriginEntity collision = originManager.getOrigin(currentUser.getName());
    return collision == null ? currentUser.getName() : String.format("%s (%s)", currentUser.getName(), UUID.randomUUID().toString().substring(0, 8));
  }

  private <T> T wrapInvalidCredentialsException(WithInvalidCredentialsException<T> wrappedMethod) throws AuthenticationFailedException {
    try {
      return wrappedMethod.call();
    } catch (InvalidCredentialsException ex) {
      throw new AuthenticationFailedException("Could not authenticate user: " + ex.getMessage());
    }
  }

  private interface WithInvalidCredentialsException<T> {
    T call() throws InvalidCredentialsException;
  }
}
