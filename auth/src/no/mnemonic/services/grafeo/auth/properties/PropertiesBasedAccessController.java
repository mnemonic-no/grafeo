package no.mnemonic.services.grafeo.auth.properties;

import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.auth.AccessController;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.*;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.auth.OrganizationSPI;
import no.mnemonic.services.grafeo.auth.SubjectSPI;
import no.mnemonic.services.grafeo.auth.properties.internal.*;
import no.mnemonic.services.grafeo.auth.properties.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * AccessController implementation which is based on a configuration read from a properties file.
 */
public class PropertiesBasedAccessController implements AccessController, OrganizationSPI, SubjectSPI, LifecycleAspect {

  private static final String NOT_AVAILABLE_NAME = "N/A";
  private static final long DEFAULT_READING_INTERVAL = 60_000; // milliseconds

  private final Timer readingTimer = new Timer(PropertiesBasedAccessController.class.getSimpleName() + " - readingTimer");
  private final AtomicReference<AccessControllerState> state = new AtomicReference<>();
  private final String propertiesFile;
  private final long readingInterval;

  private PropertiesBasedAccessController(String propertiesFile, long readingInterval) {
    this.propertiesFile = propertiesFile;
    this.readingInterval = readingInterval != 0 ? readingInterval : DEFAULT_READING_INTERVAL;
  }

  @Override
  public SessionDescriptor validate(Credentials credentials) throws InvalidCredentialsException {
    if (!(credentials instanceof SubjectCredentials)) {
      throw new InvalidCredentialsException("Cannot handle the type of provided credentials.");
    }

    PropertiesSubject subject = state.get().getSubject(SubjectCredentials.class.cast(credentials).getSubjectID());
    if (subject == null || subject.isGroup()) {
      throw new InvalidCredentialsException("The presented credentials are not valid.");
    }

    return SubjectDescriptor.builder()
            .setIdentifier(SubjectIdentifier.builder().setInternalID(subject.getInternalID()).build())
            .build();
  }

  @Override
  public boolean hasPermission(Credentials credentials, FunctionIdentity function) throws InvalidCredentialsException {
    SessionDescriptor descriptor = validate(credentials);
    return hasPermission(getSubject(descriptor), getFunctionName(function));
  }

  @Override
  public boolean hasPermission(Credentials credentials, NamedFunction function) throws InvalidCredentialsException {
    SessionDescriptor descriptor = validate(credentials);
    return hasPermission(getSubject(descriptor), getFunctionName(function));
  }

  @Override
  public boolean hasPermission(Credentials credentials, FunctionIdentity function, OrganizationIdentity organization) throws InvalidCredentialsException {
    SessionDescriptor descriptor = validate(credentials);
    return hasPermission(getSubject(descriptor), getFunctionName(function), getOrganizationID(organization));
  }

  @Override
  public boolean hasPermission(Credentials credentials, NamedFunction function, OrganizationIdentity organization) throws InvalidCredentialsException {
    SessionDescriptor descriptor = validate(credentials);
    return hasPermission(getSubject(descriptor), getFunctionName(function), getOrganizationID(organization));
  }

  @Override
  public Set<SubjectIdentity> getSubjectIdentities(Credentials credentials) throws InvalidCredentialsException {
    SessionDescriptor descriptor = validate(credentials);

    PropertiesSubject subject = getSubject(descriptor);
    Set<PropertiesSubject> parents = state.get().getParentSubjects(subject.getInternalID());

    // Return subject itself and its parents.
    return SetUtils.addToSet(parents, subject)
            .stream()
            .map(s -> SubjectIdentifier.builder().setInternalID(s.getInternalID()).build())
            .collect(Collectors.toSet());
  }

  @Override
  public Set<OrganizationIdentity> getAvailableOrganizations(Credentials credentials) throws InvalidCredentialsException {
    SessionDescriptor descriptor = validate(credentials);
    return resolveAvailableOrganizations(getSubject(descriptor))
            .stream()
            .map(id -> OrganizationIdentifier.builder().setInternalID(id).build())
            .collect(Collectors.toSet());
  }

  @Override
  public Set<OrganizationIdentity> getDescendingOrganizations(Credentials credentials, OrganizationIdentity topLevelOrg) throws InvalidCredentialsException {
    SessionDescriptor descriptor = validate(credentials);

    PropertiesSubject subject = getSubject(descriptor);
    long organizationID = getOrganizationID(topLevelOrg);

    // Subject has no access to organization.
    if (!resolveAvailableOrganizations(subject).contains(organizationID)) {
      return SetUtils.set();
    }

    // Resolve children for organization.
    Set<Long> children = SetUtils.set(state.get().getChildOrganizations(organizationID), PropertiesOrganization::getInternalID);

    // Return organization itself and its children.
    return SetUtils.addToSet(children, organizationID)
            .stream()
            .map(id -> OrganizationIdentifier.builder().setInternalID(id).build())
            .collect(Collectors.toSet());
  }

  @Override
  public Organization resolveOrganization(Credentials credentials, UUID id) throws InvalidCredentialsException {
    validate(credentials);

    PropertiesOrganization organization = state.get().getOrganization(IdMapper.toInternalID(id));
    return ObjectUtils.ifNotNull(organization, o -> createOrganization(id, o.getName()));
  }

  @Override
  public Organization resolveOrganization(Credentials credentials, String name) throws InvalidCredentialsException {
    validate(credentials);

    PropertiesOrganization organization = state.get().getOrganization(name);
    return ObjectUtils.ifNotNull(organization, o -> createOrganization(IdMapper.toGlobalID(o.getInternalID()), o.getName()));
  }

  @Override
  public Organization resolveCurrentUserAffiliation(Credentials credentials) throws InvalidCredentialsException {
    SessionDescriptor descriptor = validate(credentials);
    long affiliationID = getSubject(descriptor).getAffiliation();

    PropertiesOrganization organization = state.get().getOrganization(affiliationID);
    return ObjectUtils.ifNotNull(
            organization,
            o -> createOrganization(IdMapper.toGlobalID(affiliationID), o.getName()),
            createOrganization(IdMapper.toGlobalID(affiliationID), NOT_AVAILABLE_NAME)
    );
  }

  @Override
  public Subject resolveSubject(Credentials credentials, UUID id) throws InvalidCredentialsException {
    validate(credentials);

    PropertiesSubject subject = state.get().getSubject(IdMapper.toInternalID(id));
    return ObjectUtils.ifNotNull(subject, s -> createSubject(id, s.getName(), state.get().getOrganization(s.getAffiliation())));
  }

  @Override
  public Subject resolveSubject(Credentials credentials, String name) throws InvalidCredentialsException {
    validate(credentials);

    PropertiesSubject subject = state.get().getSubject(name);
    return ObjectUtils.ifNotNull(subject, s -> createSubject(IdMapper.toGlobalID(s.getInternalID()), s.getName(), state.get().getOrganization(s.getAffiliation())));
  }

  @Override
  public Subject resolveCurrentUser(Credentials credentials) throws InvalidCredentialsException {
    SessionDescriptor descriptor = validate(credentials);
    long internalID = getSubject(descriptor).getInternalID();

    PropertiesSubject subject = state.get().getSubject(internalID);
    return ObjectUtils.ifNotNull(subject, s -> createSubject(IdMapper.toGlobalID(internalID), s.getName(), state.get().getOrganization(s.getAffiliation())));
  }

  @Override
  public void startComponent() {
    // Read properties file once before starting the timer.
    readPropertiesFile();
    startTimer();
  }

  @Override
  public void stopComponent() {
    readingTimer.cancel();
  }

  public static Builder builder() {
    return new Builder();
  }

  private void startTimer() {
    // Keep it simple and just re-read the properties file in regular intervals.
    readingTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        readPropertiesFile();
      }
    }, readingInterval, readingInterval);
  }

  private void readPropertiesFile() {
    PropertiesFileParser parser = new PropertiesFileParser();
    parser.parse(propertiesFile);

    AccessControllerState newState = AccessControllerState.builder()
            .setFunctions(parser.getFunctions())
            .setOrganizations(parser.getOrganizations())
            .setSubjects(parser.getSubjects())
            .build();

    state.set(newState);
  }

  private PropertiesSubject getSubject(SessionDescriptor descriptor) {
    if (!(descriptor instanceof SubjectDescriptor)) {
      throw new IllegalArgumentException("Cannot handle the type of provided descriptor.");
    }

    return state.get().getSubject(SubjectDescriptor.class.cast(descriptor).getIdentifier().getInternalID());
  }

  private long getOrganizationID(OrganizationIdentity organization) {
    if (!(organization instanceof OrganizationIdentifier)) {
      throw new IllegalArgumentException("Cannot handle the type of provided organization.");
    }

    return OrganizationIdentifier.class.cast(organization).getInternalID();
  }

  private String getFunctionName(FunctionIdentity function) {
    if (!(function instanceof FunctionIdentifier)) {
      throw new IllegalArgumentException("Cannot handle the type of provided function.");
    }

    return FunctionIdentifier.class.cast(function).getName();
  }

  private String getFunctionName(NamedFunction function) {
    if (function == null) {
      throw new IllegalArgumentException("Given function is null.");
    }

    return function.getName();
  }

  private boolean hasPermission(PropertiesSubject subject, String requestedFunction) {
    boolean found = false;

    // Check permissions directly granted to requesting subject regardless of organization.
    Set<String> grantedFunctions = subject.getPermissions()
            .values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    if (checkFunctionTree(grantedFunctions, requestedFunction)) {
      found = true;
    }

    if (!found) {
      // Check if permission was granted to any parent subject group of the requesting subject.
      for (PropertiesSubject parent : state.get().getParentSubjects(subject.getInternalID())) {
        if (hasPermission(parent, requestedFunction)) {
          found = true;
          break;
        }
      }
    }

    return found;
  }

  private boolean hasPermission(PropertiesSubject subject, String requestedFunction, long requestedOrganizationID) {
    boolean found = false;

    // Check permissions directly granted to requesting subject for the requested organization.
    if (checkFunctionTree(subject.getPermissions().get(requestedOrganizationID), requestedFunction)) {
      found = true;
    }

    if (!found) {
      // Check if permission was granted to any parent organization group of the requested organization.
      for (PropertiesOrganization parent : state.get().getParentOrganizations(requestedOrganizationID)) {
        if (hasPermission(subject, requestedFunction, parent.getInternalID())) {
          found = true;
          break;
        }
      }
    }

    if (!found) {
      // Check if permission was granted to any parent subject group of the requesting subject.
      for (PropertiesSubject parent : state.get().getParentSubjects(subject.getInternalID())) {
        if (hasPermission(parent, requestedFunction, requestedOrganizationID)) {
          found = true;
          break;
        }
      }
    }

    return found;
  }

  private boolean checkFunctionTree(Set<String> grantedFunctions, String requestedFunction) {
    boolean found = false;

    for (String grantedFunction : SetUtils.set(grantedFunctions)) {
      if (grantedFunction.equals(requestedFunction)) {
        // Found a function or function group granted to subject directly.
        found = true;
        break;
      }

      // If 'grantedFunction' is a group check child functions recursively.
      // If 'group' isn't defined it will just be skipped (i.e. it's not a group but a single function).
      PropertiesFunction group = state.get().getFunction(grantedFunction);
      if (group != null && group.isGroup() && checkFunctionTree(PropertiesFunctionGroup.class.cast(group).getMembers(), requestedFunction)) {
        // Found a function or function group granted to subject indirectly via resolved function tree.
        found = true;
        break;
      }
    }

    return found;
  }

  private Set<Long> resolveAvailableOrganizations(PropertiesSubject subject) {
    // All organizations the subject has direct access to.
    Set<Long> directAccessibleOrganizations = SetUtils.set(subject.getPermissions().keySet());

    // All child organizations for the directly accessible organizations.
    Set<Long> childOrganizations = SetUtils.set();
    for (Long id : directAccessibleOrganizations) {
      childOrganizations = SetUtils.union(childOrganizations, SetUtils.set(state.get().getChildOrganizations(id), PropertiesOrganization::getInternalID));
    }

    // Also resolve all organizations for all parent subject groups.
    Set<Long> parentSubjectsOrganizations = SetUtils.set();
    for (PropertiesSubject parent : state.get().getParentSubjects(subject.getInternalID())) {
      parentSubjectsOrganizations = SetUtils.union(parentSubjectsOrganizations, resolveAvailableOrganizations(parent));
    }

    return SetUtils.union(directAccessibleOrganizations, childOrganizations, parentSubjectsOrganizations);
  }

  private Organization createOrganization(UUID id, String name) {
    return Organization.builder()
            .setId(id)
            .setName(name)
            .build();
  }

  private Subject createSubject(UUID id, String name, PropertiesOrganization organization) {
    return Subject.builder()
            .setId(id)
            .setName(name)
            .setOrganization(ObjectUtils.ifNotNull(
                    organization,
                    o -> createOrganization(IdMapper.toGlobalID(o.getInternalID()), o.getName()).toInfo())
            ).build();
  }

  public static class Builder {
    private String propertiesFile;
    private long readingInterval;

    private Builder() {
    }

    public PropertiesBasedAccessController build() {
      return new PropertiesBasedAccessController(propertiesFile, readingInterval);
    }

    public Builder setPropertiesFile(String propertiesFile) {
      this.propertiesFile = propertiesFile;
      return this;
    }

    public Builder setReadingInterval(long readingInterval) {
      this.readingInterval = readingInterval;
      return this;
    }
  }

}
