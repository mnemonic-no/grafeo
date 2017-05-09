package no.mnemonic.act.platform.auth.properties.model;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class SubjectCredentialsTest {

  @Test
  public void testCreateCredentials() {
    SubjectCredentials credentials = SubjectCredentials.builder().setSubjectID(1).build();
    SubjectIdentifier identifier = (SubjectIdentifier) credentials.getUserID();
    assertEquals(1, identifier.getInternalID());
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), identifier.getGlobalID());
  }

}
