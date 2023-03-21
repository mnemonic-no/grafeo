package no.mnemonic.services.grafeo.auth.properties.internal;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class AccessControllerStateTest {

  @Test
  public void testGetFunction() {
    PropertiesFunction function = PropertiesFunction.builder().setName("test").build();
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
    PropertiesOrganization organization = PropertiesOrganization.builder()
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
    PropertiesSubject subject = PropertiesSubject.builder()
            .setInternalID(42)
            .setName("name")
            .build();
    AccessControllerState state = AccessControllerState.builder().addSubject(subject).build();
    assertSame(subject, state.getSubject(subject.getInternalID()));
    assertSame(subject, state.getSubject(subject.getName()));
  }

  @Test
  public void testGetSubjectForUnknownSubject() {
    AccessControllerState state = AccessControllerState.builder().build();
    assertNull(state.getSubject(42));
    assertNull(state.getSubject("name"));
  }

  @Test
  public void testGetParentOrganizationsUnknownOrganization() {
    AccessControllerState state = AccessControllerState.builder().build();
    assertEmpty(state.getParentOrganizations(42));
  }

  @Test
  public void testGetParentOrganizationsNoParentsFound() {
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(42).build();
    AccessControllerState state = AccessControllerState.builder().addOrganization(organization).build();
    assertEmpty(state.getParentOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetParentOrganizationsDirectParent() {
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganizationGroup parent = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganizationGroup directParent = PropertiesOrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    PropertiesOrganizationGroup indirectParent = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganizationGroup directParent1 = PropertiesOrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    PropertiesOrganizationGroup directParent2 = PropertiesOrganizationGroup.builder()
            .setInternalID(11)
            .addMember(organization.getInternalID())
            .build();
    PropertiesOrganizationGroup indirectParent = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganization otherOrganization1 = PropertiesOrganization.builder().setInternalID(2).build();
    PropertiesOrganization otherOrganization2 = PropertiesOrganization.builder().setInternalID(3).build();
    PropertiesOrganizationGroup parent = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganization otherOrganization = PropertiesOrganization.builder().setInternalID(2).build();
    PropertiesOrganizationGroup parent = PropertiesOrganizationGroup.builder()
            .setInternalID(10)
            .setMembers(SetUtils.set(organization.getInternalID(), otherOrganization.getInternalID()))
            .build();
    PropertiesOrganizationGroup otherGroup = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(42).build();
    AccessControllerState state = AccessControllerState.builder().addOrganization(organization).build();
    assertEmpty(state.getChildOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsNoChildrenFound() {
    PropertiesOrganizationGroup organization = PropertiesOrganizationGroup.builder().setInternalID(42).build();
    AccessControllerState state = AccessControllerState.builder().addOrganization(organization).build();
    assertEmpty(state.getChildOrganizations(organization.getInternalID()));
  }

  @Test
  public void testGetChildOrganizationsDirectChildren() {
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganizationGroup parent = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization child1 = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganization child2 = PropertiesOrganization.builder().setInternalID(2).build();
    PropertiesOrganizationGroup parent = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganizationGroup directParent = PropertiesOrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    PropertiesOrganizationGroup indirectParent = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganizationGroup directParent1 = PropertiesOrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    PropertiesOrganizationGroup directParent2 = PropertiesOrganizationGroup.builder()
            .setInternalID(11)
            .addMember(organization.getInternalID())
            .build();
    PropertiesOrganizationGroup indirectParent = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization organization = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganizationGroup directParent = PropertiesOrganizationGroup.builder()
            .setInternalID(10)
            .addMember(organization.getInternalID())
            .build();
    PropertiesOrganizationGroup indirectParent = PropertiesOrganizationGroup.builder()
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
    PropertiesOrganization child1 = PropertiesOrganization.builder().setInternalID(1).build();
    PropertiesOrganization child2 = PropertiesOrganization.builder().setInternalID(2).build();
    PropertiesOrganization child3 = PropertiesOrganization.builder().setInternalID(3).build();
    PropertiesOrganizationGroup parent = PropertiesOrganizationGroup.builder()
            .setInternalID(10)
            .setMembers(SetUtils.set(child1.getInternalID(), child2.getInternalID()))
            .build();
    PropertiesOrganizationGroup otherGroup = PropertiesOrganizationGroup.builder()
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
    PropertiesSubject subject = PropertiesSubject.builder().setInternalID(42).build();
    AccessControllerState state = AccessControllerState.builder().addSubject(subject).build();
    assertEmpty(state.getParentSubjects(subject.getInternalID()));
  }

  @Test
  public void testGetParentSubjectsDirectParent() {
    PropertiesSubject subject = PropertiesSubject.builder().setInternalID(1).build();
    PropertiesSubjectGroup parent = PropertiesSubjectGroup.builder()
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
    PropertiesSubject subject = PropertiesSubject.builder().setInternalID(1).build();
    PropertiesSubjectGroup directParent = PropertiesSubjectGroup.builder()
            .setInternalID(10)
            .addMember(subject.getInternalID())
            .build();
    PropertiesSubjectGroup indirectParent = PropertiesSubjectGroup.builder()
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
    PropertiesSubject subject = PropertiesSubject.builder().setInternalID(1).build();
    PropertiesSubjectGroup directParent1 = PropertiesSubjectGroup.builder()
            .setInternalID(10)
            .addMember(subject.getInternalID())
            .build();
    PropertiesSubjectGroup directParent2 = PropertiesSubjectGroup.builder()
            .setInternalID(11)
            .addMember(subject.getInternalID())
            .build();
    PropertiesSubjectGroup indirectParent = PropertiesSubjectGroup.builder()
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
    PropertiesSubject subject = PropertiesSubject.builder().setInternalID(1).build();
    PropertiesSubject otherSubject1 = PropertiesSubject.builder().setInternalID(2).build();
    PropertiesSubject otherSubject2 = PropertiesSubject.builder().setInternalID(3).build();
    PropertiesSubjectGroup parent = PropertiesSubjectGroup.builder()
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
    PropertiesSubject subject = PropertiesSubject.builder().setInternalID(1).build();
    PropertiesSubject otherSubject = PropertiesSubject.builder().setInternalID(2).build();
    PropertiesSubjectGroup parent = PropertiesSubjectGroup.builder()
            .setInternalID(10)
            .setMembers(SetUtils.set(subject.getInternalID(), otherSubject.getInternalID()))
            .build();
    PropertiesSubjectGroup otherGroup = PropertiesSubjectGroup.builder()
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
