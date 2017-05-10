package no.mnemonic.act.platform.auth.properties.model;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class OrganizationIdentifierTest {

  @Test
  public void testCreateIdentifierWithGlobalIdSetsInternalId() {
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    OrganizationIdentifier identifier = OrganizationIdentifier.builder().setGlobalID(id).build();
    assertEquals(id, identifier.getGlobalID());
    assertEquals(1, identifier.getInternalID());
  }

  @Test
  public void testCreateIdentifierWithInternalIdSetsGlobalId() {
    OrganizationIdentifier identifier = OrganizationIdentifier.builder().setInternalID(1).build();
    assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), identifier.getGlobalID());
    assertEquals(1, identifier.getInternalID());
  }

}
