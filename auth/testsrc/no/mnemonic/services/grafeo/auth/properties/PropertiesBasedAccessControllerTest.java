package no.mnemonic.services.grafeo.auth.properties;

import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.*;
import no.mnemonic.services.grafeo.api.model.v1.Organization;
import no.mnemonic.services.grafeo.api.model.v1.Subject;
import no.mnemonic.services.grafeo.auth.properties.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PropertiesBasedAccessControllerTest {

  private PropertiesBasedAccessController accessController;
  private Path propertiesFile;

  @AfterEach
  public void cleanUp() throws Exception {
    if (accessController != null) accessController.stopComponent();
    if (propertiesFile != null) Files.deleteIfExists(propertiesFile);
  }

  /* validate(credentials) */

  @Test
  public void testValidateWithValidCredentials() throws Exception {
    setup("subject.1.name = subject");
    SessionDescriptor descriptor = accessController.validate(createCredentials(1));
    assertInstanceOf(SubjectDescriptor.class, descriptor);
    assertEquals(1, ((SubjectDescriptor) descriptor).getIdentifier().getInternalID());
  }

  @Test
  public void testValidateWithNullCredentials() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.validate(null));
  }

  @Test
  public void testValidateWithCredentialsOfWrongType() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.validate(new Credentials() {}));
  }

  @Test
  public void testValidateWithCredentialsForUnknownSubject() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.validate(createCredentials(42)));
  }

  @Test
  public void testValidateWithCredentialsForSubjectGroup() throws Exception {
    String content = """
            subject.1.name = subject
            subject.1.type = group
            """;
    setup(content);
    assertThrows(InvalidCredentialsException.class, () -> accessController.validate(createCredentials(1)));
  }

  /* hasPermission(credentials, function) */

  @Test
  public void testHasPermissionFunctionIdentityWithInvalidCredentials() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(InvalidCredentialsException.class, () -> accessController.hasPermission(createCredentials(42), createFunctionIdentifier("function")));
  }

  @Test
  public void testHasPermissionFunctionIdentityWithFunctionOfWrongType() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), new FunctionIdentity() {}));
  }

  @Test
  public void testHasPermissionFunctionIdentityWithNullFunction() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), (FunctionIdentity) null));
  }

  @Test
  public void testHasPermissionNamedFunctionWithInvalidCredentials() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(InvalidCredentialsException.class, () -> accessController.hasPermission(createCredentials(42), () -> "function"));
  }

  @Test
  public void testHasPermissionNamedFunctionWithNullFunction() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), (NamedFunction) null));
  }

  @Test
  public void testHasPermissionWithoutGrantedPermissions() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("no_grant")));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "no_grant"));
  }

  @Test
  public void testHasPermissionWithoutAccessToFunction() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("no_access")));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "no_access"));
  }

  @Test
  public void testHasPermissionWithoutAccessToParentFunctionGroup() throws Exception {
    String content = """
            function.group1.members = group2
            function.group2.members = function
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = group2
            """;
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group1")));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "group1"));
  }

  @Test
  public void testHasPermissionWithDirectlyGrantedFunction() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function"));
  }

  @Test
  public void testHasPermissionWithDirectlyGrantedFunctionGroup() throws Exception {
    String content = """
            function.group.members = function
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = group
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "group"));
  }

  @Test
  public void testHasPermissionWithIndirectlyGrantedFunction() throws Exception {
    String content = """
            function.group1.members = group2
            function.group2.members = function
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = group1
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function"));
  }

  @Test
  public void testHasPermissionWithIndirectlyGrantedFunctionGroup() throws Exception {
    String content = """
            function.group1.members = group2
            function.group2.members = function
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = group1
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group2")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "group2"));
  }

  @Test
  public void testHasPermissionAcrossMultipleOrganizations() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization1
            organization.2.name = organization2
            subject.1.permission.1 = function1
            subject.1.permission.2 = function2
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function1")));
    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function2")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function1"));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function2"));
  }

  @Test
  public void testHasPermissionInheritedFromSubjectGroup() throws Exception {
    String content = """
            subject.1.name = subject1
            subject.2.name = subject2
            subject.2.type = group
            subject.2.members = 1
            subject.3.name = subject3
            subject.3.type = group
            subject.3.members = 2
            organization.1.name = organization
            subject.3.permission.1 = function
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function"));
  }

  /* hasPermission(credentials, function, organization) */

  @Test
  public void testHasPermissionForOrganizationFunctionIdentityWithInvalidCredentials() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(InvalidCredentialsException.class, () -> accessController.hasPermission(createCredentials(42), createFunctionIdentifier("function"), createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationFunctionIdentityWithFunctionOfWrongType() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), new FunctionIdentity() {}, createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationFunctionIdentityWithNullFunction() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), (FunctionIdentity) null, createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationFunctionIdentityWithOrganizationOfWrongType() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), new OrganizationIdentity() {}));
  }

  @Test
  public void testHasPermissionForOrganizationFunctionIdentityWithNullOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), null));
  }

  @Test
  public void testHasPermissionForOrganizationNamedFunctionWithInvalidCredentials() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(InvalidCredentialsException.class, () -> accessController.hasPermission(createCredentials(42), () -> "function", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationNamedFunctionWithNullFunction() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), (NamedFunction) null, createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationNamedFunctionWithOrganizationOfWrongType() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), () -> "function", new OrganizationIdentity() {}));
  }

  @Test
  public void testHasPermissionForOrganizationNamedFunctionWithNullOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);
    assertThrows(IllegalArgumentException.class, () -> accessController.hasPermission(createCredentials(1), () -> "function", null));
  }

  @Test
  public void testHasPermissionForOrganizationWithoutGrantedPermissions() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("no_grant"), createOrganizationIdentifier(1)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "no_grant", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithoutAccessToFunction() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("no_access"), createOrganizationIdentifier(1)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "no_access", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithoutAccessToParentFunctionGroup() throws Exception {
    String content = """
            function.group1.members = group2
            function.group2.members = function
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = group2
            """;
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group1"), createOrganizationIdentifier(1)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "group1", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithoutAccessToOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization1
            organization.2.name = organization2
            subject.1.permission.1 = function
            """;
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(2)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(2)));
  }

  @Test
  public void testHasPermissionForOrganizationWithoutAccessToParentOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization1
            organization.2.name = organization2
            organization.3.name = organization3
            organization.3.type = group
            organization.3.members = 1,2
            subject.1.permission.1 = function
            """;
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(2)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(2)));
  }

  @Test
  public void testHasPermissionForOrganizationWithDirectlyGrantedFunction() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForFunctionWithDirectlyGrantedFunctionGroup() throws Exception {
    String content = """
            function.group.members = function
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = group
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "group", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithIndirectlyGrantedFunction() throws Exception {
    String content = """
            function.group1.members = group2
            function.group2.members = function
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = group1
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithIndirectlyGrantedFunctionGroup() throws Exception {
    String content = """
            function.group1.members = group2
            function.group2.members = function
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = group1
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group2"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "group2", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithPermissionsInheritedFromSubjectGroup() throws Exception {
    String content = """
            subject.1.name = subject1
            subject.2.name = subject2
            subject.2.type = group
            subject.2.members = 1
            subject.3.name = subject3
            subject.3.type = group
            subject.3.members = 2
            organization.1.name = organization
            subject.3.permission.1 = function
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithPermissionsInheritedFromOrganizationGroup() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization1
            organization.2.name = organization2
            organization.2.type = group
            organization.2.members = 1
            organization.3.name = organization3
            organization.3.type = group
            organization.3.members = 2
            subject.1.permission.3 = function
            """;
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(1)));
  }

  /* getSubjectIdentities(credentials) */

  @Test
  public void testGetSubjectIdentitiesWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.getSubjectIdentities(createCredentials(42)));
  }

  @Test
  public void testGetSubjectIdentitiesReturnsOnlySubject() throws Exception {
    setup("subject.1.name = subject");
    Set<SubjectIdentity> subjects = accessController.getSubjectIdentities(createCredentials(1));
    assertEquals(1, subjects.size());
    assertSubjectID(subjects, 1);
  }

  @Test
  public void testGetSubjectIdentitiesReturnsParents() throws Exception {
    String content = """
            subject.1.name = subject1
            subject.2.name = subject2
            subject.2.type = group
            subject.2.members = 1
            subject.3.name = subject3
            subject.3.type = group
            subject.3.members = 2
            """;
    setup(content);

    Set<SubjectIdentity> subjects = accessController.getSubjectIdentities(createCredentials(1));
    assertEquals(3, subjects.size());
    assertSubjectID(subjects, 1); // subject
    assertSubjectID(subjects, 2); // parent
    assertSubjectID(subjects, 3); // parent
  }

  @Test
  public void testGetSubjectIdentitiesOmitsOtherSubjects() throws Exception {
    String content = """
            subject.1.name = subject1
            subject.2.name = subject2
            subject.3.name = subject3
            subject.3.type = group
            subject.3.members = 1,2
            subject.4.name = subject4
            subject.4.type = group
            subject.4.members = 2
            """;
    setup(content);

    Set<SubjectIdentity> subjects = accessController.getSubjectIdentities(createCredentials(1));
    assertEquals(2, subjects.size());
    assertSubjectID(subjects, 1); // subject
    assertSubjectID(subjects, 3); // parent
  }

  /* getAvailableOrganizations(credentials) */

  @Test
  public void testGetAvailableOrganizationsWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.getAvailableOrganizations(createCredentials(42)));
  }

  @Test
  public void testGetAvailableOrganizationsWithoutPermissions() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    assertEmpty(accessController.getAvailableOrganizations(createCredentials(1)));
  }

  @Test
  public void testGetAvailableOrganizationsDirectlyAccessibleOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(1, organizations.size());
    assertOrganizationID(organizations, 1);
  }

  @Test
  public void testGetAvailableOrganizationsIncludeChildOrganizations() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization1
            organization.2.name = organization2
            organization.2.type = group
            organization.2.members = 1
            organization.3.name = organization3
            organization.3.type = group
            organization.3.members = 2
            subject.1.permission.3 = function
            """;
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(3, organizations.size());
    assertOrganizationID(organizations, 1);
    assertOrganizationID(organizations, 2);
    assertOrganizationID(organizations, 3);
  }

  @Test
  public void testGetAvailableOrganizationsOmitsInaccessibleParent() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization1
            organization.2.name = organization2
            organization.2.type = group
            organization.2.members = 1
            organization.3.name = organization3
            organization.3.type = group
            organization.3.members = 2
            subject.1.permission.2 = function
            """;
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(2, organizations.size());
    assertOrganizationID(organizations, 1);
    assertOrganizationID(organizations, 2);
  }

  @Test
  public void testGetAvailableOrganizationsInheritedFromSubjectGroup() throws Exception {
    String content = """
            subject.1.name = subject1
            subject.2.name = subject2
            subject.2.type = group
            subject.2.members = 1
            subject.3.name = subject3
            subject.3.type = group
            subject.3.members = 2
            organization.1.name = organization
            subject.3.permission.1 = function
            """;
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(1, organizations.size());
    assertOrganizationID(organizations, 1);
  }

  @Test
  public void testGetAvailableOrganizationsOmitsDuplicates() throws Exception {
    String content = """
            subject.1.name = subject1
            subject.2.name = subject2
            subject.2.type = group
            subject.2.members = 1
            subject.3.name = subject3
            subject.3.type = group
            subject.3.members = 2
            organization.1.name = organization
            subject.1.permission.1 = function1
            subject.2.permission.1 = function2
            subject.3.permission.1 = function3
            """;
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(1, organizations.size());
    assertOrganizationID(organizations, 1);
  }

  /* getDescendingOrganizations(credentials, topLevelOrg) */

  @Test
  public void testGetDescendingOrganizationsWithInvalidCredentials() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    assertThrows(InvalidCredentialsException.class, () -> accessController.getDescendingOrganizations(createCredentials(42), createOrganizationIdentifier(1)));
  }

  @Test
  public void testGetDescendingOrganizationsWithOrganizationOfWrongType() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    assertThrows(IllegalArgumentException.class, () -> accessController.getDescendingOrganizations(createCredentials(1), new OrganizationIdentity() {}));
  }

  @Test
  public void testGetDescendingOrganizationsWithNullOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    assertThrows(IllegalArgumentException.class, () -> accessController.getDescendingOrganizations(createCredentials(1), null));
  }

  @Test
  public void testGetDescendingOrganizationsWithoutPermissions() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    assertEmpty(accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(1)));
  }

  @Test
  public void testGetDescendingOrganizationsWithoutAccessToParent() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization1
            organization.2.name = organization2
            organization.2.type = group
            organization.2.members = 1
            subject.1.permission.1 = function
            """;
    setup(content);

    assertEmpty(accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(2)));
  }

  @Test
  public void testGetDescendingOrganizationsDirectlyAccessibleOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            subject.1.permission.1 = function
            """;
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(1));
    assertEquals(1, organizations.size());
    assertOrganizationID(organizations, 1);
  }

  @Test
  public void testGetDescendingOrganizationsIncludeChildOrganizations() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization1
            organization.2.name = organization2
            organization.2.type = group
            organization.2.members = 1
            organization.3.name = organization3
            organization.3.type = group
            organization.3.members = 2
            subject.1.permission.3 = function
            """;
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(3));
    assertEquals(3, organizations.size());
    assertOrganizationID(organizations, 1);
    assertOrganizationID(organizations, 2);
    assertOrganizationID(organizations, 3);
  }

  @Test
  public void testGetDescendingOrganizationsReturnSubTree() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization1
            organization.2.name = organization2
            organization.2.type = group
            organization.2.members = 1
            organization.3.name = organization3
            organization.3.type = group
            organization.3.members = 2
            subject.1.permission.3 = function
            """;
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(2));
    assertEquals(2, organizations.size());
    assertOrganizationID(organizations, 1);
    assertOrganizationID(organizations, 2);
  }

  /* resolveOrganization(credentials, id) */

  @Test
  public void testResolveOrganizationWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.resolveOrganization(createCredentials(42), UUID.randomUUID()));
  }

  @Test
  public void testResolveOrganizationNotExistingOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    assertNull(accessController.resolveOrganization(createCredentials(1), UUID.fromString("00000000-0000-0000-0000-000000000002")));
  }

  @Test
  public void testResolveOrganizationExistingOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Organization organization = accessController.resolveOrganization(createCredentials(1), id);
    assertEquals(id, organization.getId());
    assertEquals("organization", organization.getName());
  }

  /* resolveOrganization(credentials, name) */

  @Test
  public void testResolveOrganizationByNameWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.resolveOrganization(createCredentials(42), "something"));
  }

  @Test
  public void testResolveOrganizationByNameNotExistingOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    assertNull(accessController.resolveOrganization(createCredentials(1), "something"));
  }

  @Test
  public void testResolveOrganizationByNameExistingOrganization() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    Organization organization = accessController.resolveOrganization(createCredentials(1), "organization");
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), organization.getId());
    assertEquals("organization", organization.getName());
  }

  /* resolveCurrentUserAffiliation(credentials) */

  @Test
  public void testResolveCurrentUserAffiliationWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.resolveCurrentUserAffiliation(createCredentials(42)));
  }

  @Test
  public void testResolveCurrentUserAffiliation() throws Exception {
    String content = """
            subject.1.name = subject
            subject.1.affiliation = 1
            organization.1.name = organization
            """;
    setup(content);

    Organization organization = accessController.resolveCurrentUserAffiliation(createCredentials(1));
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), organization.getId());
    assertEquals("organization", organization.getName());
  }

  @Test
  public void testResolveCurrentUserAffiliationSubjectWithoutAffiliation() throws Exception {
    String content = """
            subject.1.name = subject
            organization.1.name = organization
            """;
    setup(content);

    Organization organization = accessController.resolveCurrentUserAffiliation(createCredentials(1));
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), organization.getId());
    assertEquals("N/A", organization.getName());
  }

  /* resolveSubject(credentials, id) */

  @Test
  public void testResolveSubjectWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.resolveSubject(createCredentials(42), UUID.randomUUID()));
  }

  @Test
  public void testResolveSubjectNotExistingSubject() throws Exception {
    setup("subject.1.name = subject");

    assertNull(accessController.resolveSubject(createCredentials(1), UUID.fromString("00000000-0000-0000-0000-000000000002")));
  }

  @Test
  public void testResolveSubjectExistingSubjectWithoutAffiliation() throws Exception {
    setup("subject.1.name = subject");

    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Subject subject = accessController.resolveSubject(createCredentials(1), id);
    assertEquals(id, subject.getId());
    assertEquals("subject", subject.getName());
    assertNull(subject.getOrganization());
  }

  @Test
  public void testResolveSubjectExistingSubjectWithAffiliation() throws Exception {
    String content = """
            subject.1.name = subject
            subject.1.affiliation = 1
            organization.1.name = organization
            """;
    setup(content);

    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Subject subject = accessController.resolveSubject(createCredentials(1), id);
    assertEquals(id, subject.getId());
    assertEquals("subject", subject.getName());
    assertNotNull(subject.getOrganization());
    assertEquals(id, subject.getOrganization().getId());
    assertEquals("organization", subject.getOrganization().getName());
  }

  /* resolveSubject(credentials, name) */

  @Test
  public void testResolveSubjectByNameWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.resolveSubject(createCredentials(42), "something"));
  }

  @Test
  public void testResolveSubjectByNameNotExistingSubject() throws Exception {
    setup("subject.1.name = subject");

    assertNull(accessController.resolveSubject(createCredentials(1), "something"));
  }

  @Test
  public void testResolveSubjectByNameExistingSubjectWithoutAffiliation() throws Exception {
    setup("subject.1.name = subject");

    Subject subject = accessController.resolveSubject(createCredentials(1), "subject");
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), subject.getId());
    assertEquals("subject", subject.getName());
    assertNull(subject.getOrganization());
  }

  @Test
  public void testResolveSubjectByNameExistingSubjectWithAffiliation() throws Exception {
    String content = """
            subject.1.name = subject
            subject.1.affiliation = 1
            organization.1.name = organization
            """;
    setup(content);

    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Subject subject = accessController.resolveSubject(createCredentials(1), "subject");
    assertEquals(id, subject.getId());
    assertEquals("subject", subject.getName());
    assertNotNull(subject.getOrganization());
    assertEquals(id, subject.getOrganization().getId());
    assertEquals("organization", subject.getOrganization().getName());
  }

  /* resolveCurrentUser(credentials) */

  @Test
  public void testResolveCurrentUserWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    assertThrows(InvalidCredentialsException.class, () -> accessController.resolveCurrentUser(createCredentials(42)));
  }

  @Test
  public void testResolveCurrentUserWithoutAffiliation() throws Exception {
    setup("subject.1.name = subject");

    Subject subject = accessController.resolveCurrentUser(createCredentials(1));
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), subject.getId());
    assertEquals("subject", subject.getName());
    assertNull(subject.getOrganization());
  }

  @Test
  public void testResolveCurrentUserWitAffiliation() throws Exception {
    String content = """
            subject.1.name = subject
            subject.1.affiliation = 1
            organization.1.name = organization
            """;
    setup(content);

    Subject subject = accessController.resolveCurrentUser(createCredentials(1));
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), subject.getId());
    assertEquals("subject", subject.getName());
    assertNotNull(subject.getOrganization());
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), subject.getOrganization().getId());
    assertEquals("organization", subject.getOrganization().getName());
  }

  private void setup(String content) throws Exception {
    // Create properties file ...
    propertiesFile = Files.createTempFile(UUID.randomUUID().toString(), ".properties");
    try (FileWriter writer = new FileWriter(propertiesFile.toFile())) {
      writer.write(content);
    }

    // ... and start up access controller.
    accessController = PropertiesBasedAccessController.builder().setPropertiesFile(propertiesFile.toString()).build();
    accessController.startComponent();
  }

  private Credentials createCredentials(long subjectID) {
    return SubjectCredentials.builder().setSubjectID(subjectID).build();
  }

  private FunctionIdentity createFunctionIdentifier(String name) {
    return FunctionIdentifier.builder().setName(name).build();
  }

  private OrganizationIdentity createOrganizationIdentifier(long organizationID) {
    return OrganizationIdentifier.builder().setInternalID(organizationID).build();
  }

  private void assertSubjectID(Set<SubjectIdentity> subjects, long subjectID) {
    assertTrue(subjects.stream().map(SubjectIdentifier.class::cast).anyMatch(s -> s.getInternalID() == subjectID));
  }

  private void assertOrganizationID(Set<OrganizationIdentity> organizations, long organizationID) {
    assertTrue(organizations.stream().map(OrganizationIdentifier.class::cast).anyMatch(s -> s.getInternalID() == organizationID));
  }

  private void assertEmpty(Set<?> collection) {
    assertNotNull(collection);
    assertTrue(collection.isEmpty());
  }

}
