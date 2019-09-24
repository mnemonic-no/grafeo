package no.mnemonic.act.platform.auth.properties.internal;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class AccessControllerStateTest {

  @Test
  public void testGetFunction() {
    Function function = Function.builder().setName("test").build();
    AccessControllerState state = AccessControllerState.builder().addFunction(function).build();
    assertSame(function, state.getFunction(function.getName()));
  }

  @Test
  public void testGetFunctionForUnknownFunction() {
    AccessControllerState state = AccessControllerState.builder().build();
    assertNull(state.getFunction("unknown"));
  }

  @Test
  public void testGetOrganization() {
    Organization organization = Organization.builder()
            .setInternalID(42)
            .setName("name")
            .build();
    AccessControllerState state = AccessControllerState.builder().addOrganization(organization).build();
    assertSame(organization, state.getOrganization(organization.getInternalID()));
    assertSame(organization, state.getOrganization(organization.getName()));
  }

  @Test
  public void testGetOrganizationForUnknownOrganization() {
    AccessControllerState state = AccessControllerState.builder().build();
    assertNull(state.getOrganization(42));
    assertNull(state.getOrganization("name"));
  }

  @Test
  public void testGetSubject() {
    Subject subject = Subject.builder().setInternalID(42).build();
    AccessControllerState state = AccessControllerState.builder().addSubject(subject).build();
    assertSame(subject, state.getSubject(subject.getInternalID()));
  }

  @Test
  public void testGetSubjectForUnknownSubject() {
    AccessControllerState state = AccessControllerState.builder().build();
    assertNull(state.getSubject(42));
  }

  @Test
  public void testGetParentOrganizationsUnknownOrganization() {
    AccessControllerState state = AccessControllerState.builder().build();
    assertEmpty(state.getParentOrganizations(42));
  }

  @Test
  public void testGetParentOrganizationsNoParentsFound() {
    Organization organization = Organization.builder().setInternalID(42).build();
    AccessControllerState state = AccessControllerState.builder().addOrganization(organization).build();
    assertEmpty(state.getParentOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetParentOrganizationsDirectParent() {
    Organization organization = Organization.builder().setInternalID(1).build();
    OrganizationGroup parent = OrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(organization, parent))
            .build();

    assertEquals(SetUtils.set(parent), state.getParentOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetParentOrganizationsRecursively() {
    Organization organization = Organization.builder().setInternalID(1).build();
    OrganizationGroup directParent = OrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    OrganizationGroup indirectParent = OrganizationGroup.builder()
            .setInternalID(11)
            .addMember(directParent.getInternalID())
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(organization, directParent, indirectParent))
            .build();

    assertEquals(SetUtils.set(directParent, indirectParent), state.getParentOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetParentOrganizationsMultiplePaths() {
    Organization organization = Organization.builder().setInternalID(1).build();
    OrganizationGroup directParent1 = OrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    OrganizationGroup directParent2 = OrganizationGroup.builder()
            .setInternalID(11)
            .addMember(organization.getInternalID())
            .build();
    OrganizationGroup indirectParent = OrganizationGroup.builder()
            .setInternalID(12)
            .setMembers(SetUtils.set(directParent1.getInternalID(), directParent2.getInternalID()))
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(organization, directParent1, directParent2, indirectParent))
            .build();

    assertEquals(SetUtils.set(directParent1, directParent2, indirectParent), state.getParentOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetParentOrganizationsSkipOtherOrganizations() {
    Organization organization = Organization.builder().setInternalID(1).build();
    Organization otherOrganization1 = Organization.builder().setInternalID(2).build();
    Organization otherOrganization2 = Organization.builder().setInternalID(3).build();
    OrganizationGroup parent = OrganizationGroup.builder()
            .setInternalID(10)
            .setMembers(SetUtils.set(organization.getInternalID(), otherOrganization1.getInternalID(), otherOrganization2.getInternalID()))
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(organization, otherOrganization1, otherOrganization2, parent))
            .build();

    assertEquals(SetUtils.set(parent), state.getParentOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetParentOrganizationsSkipOtherGroup() {
    Organization organization = Organization.builder().setInternalID(1).build();
    Organization otherOrganization = Organization.builder().setInternalID(2).build();
    OrganizationGroup parent = OrganizationGroup.builder()
            .setInternalID(10)
            .setMembers(SetUtils.set(organization.getInternalID(), otherOrganization.getInternalID()))
            .build();
    OrganizationGroup otherGroup = OrganizationGroup.builder()
            .setInternalID(11)
            .addMember(otherOrganization.getInternalID())
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(organization, otherOrganization, parent, otherGroup))
            .build();

    assertEquals(SetUtils.set(parent), state.getParentOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsUnknownOrganization() {
    AccessControllerState state = AccessControllerState.builder().build();
    assertEmpty(state.getChildOrganizations(42));
  }

  @Test
  public void testGetChildOrganizationsNoGroup() {
    Organization organization = Organization.builder().setInternalID(42).build();
    AccessControllerState state = AccessControllerState.builder().addOrganization(organization).build();
    assertEmpty(state.getChildOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsNoChildrenFound() {
    OrganizationGroup organization = OrganizationGroup.builder().setInternalID(42).build();
    AccessControllerState state = AccessControllerState.builder().addOrganization(organization).build();
    assertEmpty(state.getChildOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsDirectChildren() {
    Organization organization = Organization.builder().setInternalID(1).build();
    OrganizationGroup parent = OrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(organization, parent))
            .build();

    assertEquals(SetUtils.set(organization), state.getChildOrganizations(parent.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsMultipleChildren() {
    Organization child1 = Organization.builder().setInternalID(1).build();
    Organization child2 = Organization.builder().setInternalID(2).build();
    OrganizationGroup parent = OrganizationGroup.builder()
            .setInternalID(10)
            .setMembers(SetUtils.set(child1.getInternalID(), child2.getInternalID()))
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(child1, child2, parent))
            .build();

    assertEquals(SetUtils.set(child1, child2), state.getChildOrganizations(parent.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsRecursively() {
    Organization organization = Organization.builder().setInternalID(1).build();
    OrganizationGroup directParent = OrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    OrganizationGroup indirectParent = OrganizationGroup.builder()
            .setInternalID(11)
            .addMember(directParent.getInternalID())
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(organization, directParent, indirectParent))
            .build();

    assertEquals(SetUtils.set(directParent, organization), state.getChildOrganizations(indirectParent.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsMultiplePaths() {
    Organization organization = Organization.builder().setInternalID(1).build();
    OrganizationGroup directParent1 = OrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    OrganizationGroup directParent2 = OrganizationGroup.builder()
            .setInternalID(11)
            .addMember(organization.getInternalID())
            .build();
    OrganizationGroup indirectParent = OrganizationGroup.builder()
            .setInternalID(12)
            .setMembers(SetUtils.set(directParent1.getInternalID(), directParent2.getInternalID()))
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(organization, directParent1, directParent2, indirectParent))
            .build();

    assertEquals(SetUtils.set(directParent1, directParent2, organization), state.getChildOrganizations(indirectParent.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsSkipParent() {
    Organization organization = Organization.builder().setInternalID(1).build();
    OrganizationGroup directParent = OrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    OrganizationGroup indirectParent = OrganizationGroup.builder()
            .setInternalID(11)
            .addMember(directParent.getInternalID())
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(organization, directParent, indirectParent))
            .build();

    assertEquals(SetUtils.set(organization), state.getChildOrganizations(directParent.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsSkipOtherGroup() {
    Organization child1 = Organization.builder().setInternalID(1).build();
    Organization child2 = Organization.builder().setInternalID(2).build();
    Organization child3 = Organization.builder().setInternalID(3).build();
    OrganizationGroup parent = OrganizationGroup.builder()
            .setInternalID(10)
            .setMembers(SetUtils.set(child1.getInternalID(), child2.getInternalID()))
            .build();
    OrganizationGroup otherGroup = OrganizationGroup.builder()
            .setInternalID(11)
            .setMembers(SetUtils.set(child2.getInternalID(), child3.getInternalID()))
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setOrganizations(SetUtils.set(child1, child2, child3, parent, otherGroup))
            .build();

    assertEquals(SetUtils.set(child1, child2), state.getChildOrganizations(parent.getInternalID()));
  }

  @Test
  public void testGetParentSubjectsUnknownSubject() {
    AccessControllerState state = AccessControllerState.builder().build();
    assertEmpty(state.getParentSubjects(42));
  }

  @Test
  public void testGetParentSubjectsNoParentsFound() {
    Subject subject = Subject.builder().setInternalID(42).build();
    AccessControllerState state = AccessControllerState.builder().addSubject(subject).build();
    assertEmpty(state.getParentSubjects(subject.getInternalID()));
  }

  @Test
  public void testGetParentSubjectsDirectParent() {
    Subject subject = Subject.builder().setInternalID(1).build();
    SubjectGroup parent = SubjectGroup.builder()
            .setInternalID(10)
            .addMember(subject.getInternalID())
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setSubjects(SetUtils.set(subject, parent))
            .build();

    assertEquals(SetUtils.set(parent), state.getParentSubjects(subject.getInternalID()));
  }

  @Test
  public void testGetParentSubjectsRecursively() {
    Subject subject = Subject.builder().setInternalID(1).build();
    SubjectGroup directParent = SubjectGroup.builder()
            .setInternalID(10)
            .addMember(subject.getInternalID())
            .build();
    SubjectGroup indirectParent = SubjectGroup.builder()
            .setInternalID(11)
            .addMember(directParent.getInternalID())
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setSubjects(SetUtils.set(subject, directParent, indirectParent))
            .build();

    assertEquals(SetUtils.set(directParent, indirectParent), state.getParentSubjects(subject.getInternalID()));
  }

  @Test
  public void testGetParentSubjectsMultiplePaths() {
    Subject subject = Subject.builder().setInternalID(1).build();
    SubjectGroup directParent1 = SubjectGroup.builder()
            .setInternalID(10)
            .addMember(subject.getInternalID())
            .build();
    SubjectGroup directParent2 = SubjectGroup.builder()
            .setInternalID(11)
            .addMember(subject.getInternalID())
            .build();
    SubjectGroup indirectParent = SubjectGroup.builder()
            .setInternalID(12)
            .setMembers(SetUtils.set(directParent1.getInternalID(), directParent2.getInternalID()))
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setSubjects(SetUtils.set(subject, directParent1, directParent2, indirectParent))
            .build();

    assertEquals(SetUtils.set(directParent1, directParent2, indirectParent), state.getParentSubjects(subject.getInternalID()));
  }

  @Test
  public void testGetParentSubjectsSkipOtherSubjects() {
    Subject subject = Subject.builder().setInternalID(1).build();
    Subject otherSubject1 = Subject.builder().setInternalID(2).build();
    Subject otherSubject2 = Subject.builder().setInternalID(3).build();
    SubjectGroup parent = SubjectGroup.builder()
            .setInternalID(10)
            .setMembers(SetUtils.set(subject.getInternalID(), otherSubject1.getInternalID(), otherSubject2.getInternalID()))
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setSubjects(SetUtils.set(subject, otherSubject1, otherSubject2, parent))
            .build();

    assertEquals(SetUtils.set(parent), state.getParentSubjects(subject.getInternalID()));
  }

  @Test
  public void testGetParentSubjectsSkipOtherGroup() {
    Subject subject = Subject.builder().setInternalID(1).build();
    Subject otherSubject = Subject.builder().setInternalID(2).build();
    SubjectGroup parent = SubjectGroup.builder()
            .setInternalID(10)
            .setMembers(SetUtils.set(subject.getInternalID(), otherSubject.getInternalID()))
            .build();
    SubjectGroup otherGroup = SubjectGroup.builder()
            .setInternalID(11)
            .addMember(otherSubject.getInternalID())
            .build();
    AccessControllerState state = AccessControllerState.builder()
            .setSubjects(SetUtils.set(subject, otherSubject, parent, otherGroup))
            .build();

    assertEquals(SetUtils.set(parent), state.getParentSubjects(subject.getInternalID()));
  }

  private void assertEmpty(Set<?> collection) {
    assertNotNull(collection);
    assertTrue(collection.isEmpty());
  }

}
