package no.mnemonic.act.platform.auth.properties;

import no.mnemonic.act.platform.api.model.v1.Organization;
import no.mnemonic.act.platform.api.model.v1.Subject;
import no.mnemonic.act.platform.auth.properties.model.FunctionIdentifier;
import no.mnemonic.act.platform.auth.properties.model.OrganizationIdentifier;
import no.mnemonic.act.platform.auth.properties.model.SubjectCredentials;
import no.mnemonic.act.platform.auth.properties.model.SubjectIdentifier;
import no.mnemonic.services.common.auth.InvalidCredentialsException;
import no.mnemonic.services.common.auth.model.*;
import org.junit.After;
import org.junit.Test;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

public class PropertiesBasedAccessControllerTest {

  private PropertiesBasedAccessController accessController;
  private Path propertiesFile;

  @After
  public void cleanUp() throws Exception {
    if (accessController != null) accessController.stopComponent();
    if (propertiesFile != null) Files.deleteIfExists(propertiesFile);
  }

  /* validate(credentials) */

  @Test
  public void testValidateWithValidCredentials() throws Exception {
    setup("subject.1.name = subject");
    accessController.validate(createCredentials(1));
  }

  @Test(expected = InvalidCredentialsException.class)
  public void testValidateWithNullCredentials() throws Exception {
    setup("subject.1.name = subject");
    accessController.validate(null);
  }

  @Test(expected = InvalidCredentialsException.class)
  public void testValidateWithCredentialsOfWrongType() throws Exception {
    setup("subject.1.name = subject");
    accessController.validate(new Credentials() {
      @Override
      public SubjectIdentity getUserID() {
        return null;
      }

      @Override
      public SecurityLevel getSecurityLevel() {
        return null;
      }
    });
  }

  @Test(expected = InvalidCredentialsException.class)
  public void testValidateWithCredentialsForUnknownSubject() throws Exception {
    setup("subject.1.name = subject");
    accessController.validate(createCredentials(42));
  }

  @Test(expected = InvalidCredentialsException.class)
  public void testValidateWithCredentialsForSubjectGroup() throws Exception {
    setup("subject.1.name = subject\n" +
            "subject.1.type = group");
    accessController.validate(createCredentials(1));
  }

  /* hasPermission(credentials, function) */

  @Test(expected = InvalidCredentialsException.class)
  public void testHasPermissionFunctionIdentityWithInvalidCredentials() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(42), createFunctionIdentifier("function"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionFunctionIdentityWithFunctionOfWrongType() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), new FunctionIdentity() {
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionFunctionIdentityWithNullFunction() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), (FunctionIdentity) null);
  }

  @Test(expected = InvalidCredentialsException.class)
  public void testHasPermissionNamedFunctionWithInvalidCredentials() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(42), () -> "function");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionNamedFunctionWithNullFunction() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), (NamedFunction) null);
  }

  @Test
  public void testHasPermissionWithoutGrantedPermissions() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization" +
            "";
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("no_grant")));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "no_grant"));
  }

  @Test
  public void testHasPermissionWithoutAccessToFunction() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("no_access")));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "no_access"));
  }

  @Test
  public void testHasPermissionWithoutAccessToParentFunctionGroup() throws Exception {
    String content = "" +
            "function.group1.members = group2\n" +
            "function.group2.members = function\n" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = group2" +
            "";
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group1")));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "group1"));
  }

  @Test
  public void testHasPermissionWithDirectlyGrantedFunction() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function"));
  }

  @Test
  public void testHasPermissionWithDirectlyGrantedFunctionGroup() throws Exception {
    String content = "" +
            "function.group.members = function\n" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = group" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "group"));
  }

  @Test
  public void testHasPermissionWithIndirectlyGrantedFunction() throws Exception {
    String content = "" +
            "function.group1.members = group2\n" +
            "function.group2.members = function\n" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = group1" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function"));
  }

  @Test
  public void testHasPermissionWithIndirectlyGrantedFunctionGroup() throws Exception {
    String content = "" +
            "function.group1.members = group2\n" +
            "function.group2.members = function\n" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = group1" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group2")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "group2"));
  }

  @Test
  public void testHasPermissionAcrossMultipleOrganizations() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization1\n" +
            "organization.2.name = organization2\n" +
            "subject.1.permission.1 = function1\n" +
            "subject.1.permission.2 = function2" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function1")));
    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function2")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function1"));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function2"));
  }

  @Test
  public void testHasPermissionInheritedFromSubjectGroup() throws Exception {
    String content = "" +
            "subject.1.name = subject1\n" +
            "subject.2.name = subject2\n" +
            "subject.2.type = group\n" +
            "subject.2.members = 1\n" +
            "subject.3.name = subject3\n" +
            "subject.3.type = group\n" +
            "subject.3.members = 2\n" +
            "organization.1.name = organization\n" +
            "subject.3.permission.1 = function" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function")));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function"));
  }

  /* hasPermission(credentials, function, organization) */

  @Test(expected = InvalidCredentialsException.class)
  public void testHasPermissionForOrganizationFunctionIdentityWithInvalidCredentials() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(42), createFunctionIdentifier("function"), createOrganizationIdentifier(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionForOrganizationFunctionIdentityWithFunctionOfWrongType() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), new FunctionIdentity() {
    }, createOrganizationIdentifier(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionForOrganizationFunctionIdentityWithNullFunction() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), (FunctionIdentity) null, createOrganizationIdentifier(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionForOrganizationFunctionIdentityWithOrganizationOfWrongType() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), new OrganizationIdentity() {
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionForOrganizationFunctionIdentityWithNullOrganization() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), null);
  }

  @Test(expected = InvalidCredentialsException.class)
  public void testHasPermissionForOrganizationNamedFunctionWithInvalidCredentials() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(42), () -> "function", createOrganizationIdentifier(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionForOrganizationNamedFunctionWithNullFunction() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), (NamedFunction) null, createOrganizationIdentifier(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionForOrganizationNamedFunctionWithOrganizationOfWrongType() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), () -> "function", new OrganizationIdentity() {
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHasPermissionForOrganizationNamedFunctionWithNullOrganization() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);
    accessController.hasPermission(createCredentials(1), () -> "function", null);
  }

  @Test
  public void testHasPermissionForOrganizationWithoutGrantedPermissions() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization" +
            "";
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("no_grant"), createOrganizationIdentifier(1)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "no_grant", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithoutAccessToFunction() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("no_access"), createOrganizationIdentifier(1)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "no_access", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithoutAccessToParentFunctionGroup() throws Exception {
    String content = "" +
            "function.group1.members = group2\n" +
            "function.group2.members = function\n" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = group2" +
            "";
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group1"), createOrganizationIdentifier(1)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "group1", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithoutAccessToOrganization() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization1\n" +
            "organization.2.name = organization2\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(2)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(2)));
  }

  @Test
  public void testHasPermissionForOrganizationWithoutAccessToParentOrganization() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization1\n" +
            "organization.2.name = organization2\n" +
            "organization.3.name = organization3\n" +
            "organization.3.type = group\n" +
            "organization.3.members = 1,2\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);

    assertFalse(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(2)));
    assertFalse(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(2)));
  }

  @Test
  public void testHasPermissionForOrganizationWithDirectlyGrantedFunction() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForFunctionWithDirectlyGrantedFunctionGroup() throws Exception {
    String content = "" +
            "function.group.members = function\n" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = group" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "group", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithIndirectlyGrantedFunction() throws Exception {
    String content = "" +
            "function.group1.members = group2\n" +
            "function.group2.members = function\n" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = group1" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithIndirectlyGrantedFunctionGroup() throws Exception {
    String content = "" +
            "function.group1.members = group2\n" +
            "function.group2.members = function\n" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = group1" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("group2"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "group2", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithPermissionsInheritedFromSubjectGroup() throws Exception {
    String content = "" +
            "subject.1.name = subject1\n" +
            "subject.2.name = subject2\n" +
            "subject.2.type = group\n" +
            "subject.2.members = 1\n" +
            "subject.3.name = subject3\n" +
            "subject.3.type = group\n" +
            "subject.3.members = 2\n" +
            "organization.1.name = organization\n" +
            "subject.3.permission.1 = function" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(1)));
  }

  @Test
  public void testHasPermissionForOrganizationWithPermissionsInheritedFromOrganizationGroup() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization1\n" +
            "organization.2.name = organization2\n" +
            "organization.2.type = group\n" +
            "organization.2.members = 1\n" +
            "organization.3.name = organization3\n" +
            "organization.3.type = group\n" +
            "organization.3.members = 2\n" +
            "subject.1.permission.3 = function" +
            "";
    setup(content);

    assertTrue(accessController.hasPermission(createCredentials(1), createFunctionIdentifier("function"), createOrganizationIdentifier(1)));
    assertTrue(accessController.hasPermission(createCredentials(1), () -> "function", createOrganizationIdentifier(1)));
  }

  /* getSubjectIdentities(credentials) */

  @Test(expected = InvalidCredentialsException.class)
  public void testGetSubjectIdentitiesWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    accessController.getSubjectIdentities(createCredentials(42));
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
    String content = "" +
            "subject.1.name = subject1\n" +
            "subject.2.name = subject2\n" +
            "subject.2.type = group\n" +
            "subject.2.members = 1\n" +
            "subject.3.name = subject3\n" +
            "subject.3.type = group\n" +
            "subject.3.members = 2" +
            "";
    setup(content);

    Set<SubjectIdentity> subjects = accessController.getSubjectIdentities(createCredentials(1));
    assertEquals(3, subjects.size());
    assertSubjectID(subjects, 1); // subject
    assertSubjectID(subjects, 2); // parent
    assertSubjectID(subjects, 3); // parent
  }

  @Test
  public void testGetSubjectIdentitiesOmitsOtherSubjects() throws Exception {
    String content = "" +
            "subject.1.name = subject1\n" +
            "subject.2.name = subject2\n" +
            "subject.3.name = subject3\n" +
            "subject.3.type = group\n" +
            "subject.3.members = 1,2\n" +
            "subject.4.name = subject4\n" +
            "subject.4.type = group\n" +
            "subject.4.members = 2" +
            "";
    setup(content);

    Set<SubjectIdentity> subjects = accessController.getSubjectIdentities(createCredentials(1));
    assertEquals(2, subjects.size());
    assertSubjectID(subjects, 1); // subject
    assertSubjectID(subjects, 3); // parent
  }

  /* getAvailableOrganizations(credentials) */

  @Test(expected = InvalidCredentialsException.class)
  public void testGetAvailableOrganizationsWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    accessController.getAvailableOrganizations(createCredentials(42));
  }

  @Test
  public void testGetAvailableOrganizationsWithoutPermissions() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization" +
            "";
    setup(content);

    assertEmpty(accessController.getAvailableOrganizations(createCredentials(1)));
  }

  @Test
  public void testGetAvailableOrganizationsDirectlyAccessibleOrganization() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(1, organizations.size());
    assertOrganizationID(organizations, 1);
  }

  @Test
  public void testGetAvailableOrganizationsIncludeChildOrganizations() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization1\n" +
            "organization.2.name = organization2\n" +
            "organization.2.type = group\n" +
            "organization.2.members = 1\n" +
            "organization.3.name = organization3\n" +
            "organization.3.type = group\n" +
            "organization.3.members = 2\n" +
            "subject.1.permission.3 = function" +
            "";
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(3, organizations.size());
    assertOrganizationID(organizations, 1);
    assertOrganizationID(organizations, 2);
    assertOrganizationID(organizations, 3);
  }

  @Test
  public void testGetAvailableOrganizationsOmitsInaccessibleParent() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization1\n" +
            "organization.2.name = organization2\n" +
            "organization.2.type = group\n" +
            "organization.2.members = 1\n" +
            "organization.3.name = organization3\n" +
            "organization.3.type = group\n" +
            "organization.3.members = 2\n" +
            "subject.1.permission.2 = function" +
            "";
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(2, organizations.size());
    assertOrganizationID(organizations, 1);
    assertOrganizationID(organizations, 2);
  }

  @Test
  public void testGetAvailableOrganizationsInheritedFromSubjectGroup() throws Exception {
    String content = "" +
            "subject.1.name = subject1\n" +
            "subject.2.name = subject2\n" +
            "subject.2.type = group\n" +
            "subject.2.members = 1\n" +
            "subject.3.name = subject3\n" +
            "subject.3.type = group\n" +
            "subject.3.members = 2\n" +
            "organization.1.name = organization\n" +
            "subject.3.permission.1 = function" +
            "";
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(1, organizations.size());
    assertOrganizationID(organizations, 1);
  }

  @Test
  public void testGetAvailableOrganizationsOmitsDuplicates() throws Exception {
    String content = "" +
            "subject.1.name = subject1\n" +
            "subject.2.name = subject2\n" +
            "subject.2.type = group\n" +
            "subject.2.members = 1\n" +
            "subject.3.name = subject3\n" +
            "subject.3.type = group\n" +
            "subject.3.members = 2\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function1\n" +
            "subject.2.permission.1 = function2\n" +
            "subject.3.permission.1 = function3" +
            "";
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getAvailableOrganizations(createCredentials(1));
    assertEquals(1, organizations.size());
    assertOrganizationID(organizations, 1);
  }

  /* getDescendingOrganizations(credentials, topLevelOrg) */

  @Test(expected = InvalidCredentialsException.class)
  public void testGetDescendingOrganizationsWithInvalidCredentials() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization" +
            "";
    setup(content);

    accessController.getDescendingOrganizations(createCredentials(42), createOrganizationIdentifier(1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetDescendingOrganizationsWithOrganizationOfWrongType() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization" +
            "";
    setup(content);

    accessController.getDescendingOrganizations(createCredentials(1), new OrganizationIdentity() {
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetDescendingOrganizationsWithNullOrganization() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization" +
            "";
    setup(content);

    accessController.getDescendingOrganizations(createCredentials(1), null);
  }

  @Test
  public void testGetDescendingOrganizationsWithoutPermissions() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization" +
            "";
    setup(content);

    assertEmpty(accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(1)));
  }

  @Test
  public void testGetDescendingOrganizationsWithoutAccessToParent() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization1\n" +
            "organization.2.name = organization2\n" +
            "organization.2.type = group\n" +
            "organization.2.members = 1\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);

    assertEmpty(accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(2)));
  }

  @Test
  public void testGetDescendingOrganizationsDirectlyAccessibleOrganization() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "subject.1.permission.1 = function" +
            "";
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(1));
    assertEquals(1, organizations.size());
    assertOrganizationID(organizations, 1);
  }

  @Test
  public void testGetDescendingOrganizationsIncludeChildOrganizations() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization1\n" +
            "organization.2.name = organization2\n" +
            "organization.2.type = group\n" +
            "organization.2.members = 1\n" +
            "organization.3.name = organization3\n" +
            "organization.3.type = group\n" +
            "organization.3.members = 2\n" +
            "subject.1.permission.3 = function" +
            "";
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(3));
    assertEquals(3, organizations.size());
    assertOrganizationID(organizations, 1);
    assertOrganizationID(organizations, 2);
    assertOrganizationID(organizations, 3);
  }

  @Test
  public void testGetDescendingOrganizationsReturnSubTree() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization1\n" +
            "organization.2.name = organization2\n" +
            "organization.2.type = group\n" +
            "organization.2.members = 1\n" +
            "organization.3.name = organization3\n" +
            "organization.3.type = group\n" +
            "organization.3.members = 2\n" +
            "subject.1.permission.3 = function" +
            "";
    setup(content);

    Set<OrganizationIdentity> organizations = accessController.getDescendingOrganizations(createCredentials(1), createOrganizationIdentifier(2));
    assertEquals(2, organizations.size());
    assertOrganizationID(organizations, 1);
    assertOrganizationID(organizations, 2);
  }

  /* resolveOrganization(id) */

  @Test
  public void testResolveOrganizationNotExistingOrganization() throws Exception {
    setup("organization.1.name = organization");

    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
    Organization organization = accessController.resolveOrganization(id);
    assertEquals(id, organization.getId());
    assertEquals("N/A", organization.getName());
  }

  @Test
  public void testResolveOrganizationExistingOrganization() throws Exception {
    setup("organization.1.name = organization");

    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Organization organization = accessController.resolveOrganization(id);
    assertEquals(id, organization.getId());
    assertEquals("organization", organization.getName());
  }

  /* resolveCurrentUserAffiliation(credentials) */

  @Test(expected = InvalidCredentialsException.class)
  public void testResolveCurrentUserAffiliationWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    accessController.resolveCurrentUserAffiliation(createCredentials(42));
  }

  @Test
  public void testResolveCurrentUserAffiliation() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "subject.1.affiliation = 1\n" +
            "organization.1.name = organization\n" +
            "";
    setup(content);

    Organization organization = accessController.resolveCurrentUserAffiliation(createCredentials(1));
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), organization.getId());
    assertEquals("organization", organization.getName());
  }

  @Test
  public void testResolveCurrentUserAffiliationSubjectWithoutAffiliation() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "organization.1.name = organization\n" +
            "";
    setup(content);

    Organization organization = accessController.resolveCurrentUserAffiliation(createCredentials(1));
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), organization.getId());
    assertEquals("N/A", organization.getName());
  }

  /* resolveSubject(id) */

  @Test
  public void testResolveSubjectNotExistingSubject() throws Exception {
    setup("subject.1.name = subject");

    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
    Subject subject = accessController.resolveSubject(id);
    assertEquals(id, subject.getId());
    assertEquals("N/A", subject.getName());
    assertNull(subject.getOrganization());
  }

  @Test
  public void testResolveSubjectExistingSubjectWithoutAffiliation() throws Exception {
    setup("subject.1.name = subject");

    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Subject subject = accessController.resolveSubject(id);
    assertEquals(id, subject.getId());
    assertEquals("subject", subject.getName());
    assertNull(subject.getOrganization());
  }

  @Test
  public void testResolveSubjectExistingSubjectWithAffiliation() throws Exception {
    String content = "" +
            "subject.1.name = subject\n" +
            "subject.1.affiliation = 1\n" +
            "organization.1.name = organization\n" +
            "";
    setup(content);

    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Subject subject = accessController.resolveSubject(id);
    assertEquals(id, subject.getId());
    assertEquals("subject", subject.getName());
    assertNotNull(subject.getOrganization());
    assertEquals(id, subject.getOrganization().getId());
    assertEquals("organization", subject.getOrganization().getName());
  }

  /* resolveCurrentUser(credentials) */

  @Test(expected = InvalidCredentialsException.class)
  public void testResolveCurrentUserWithInvalidCredentials() throws Exception {
    setup("subject.1.name = subject");
    accessController.resolveCurrentUser(createCredentials(42));
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
    String content = "" +
            "subject.1.name = subject\n" +
            "subject.1.affiliation = 1\n" +
            "organization.1.name = organization\n" +
            "";
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
