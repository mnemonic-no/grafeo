package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class OrganizationTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void testEncodeOrganization() {
    Organization organization = createOrganization();
    JsonNode root = mapper.valueToTree(organization);
    assertEquals(organization.getId().toString(), root.get("id").textValue());
    assertEquals(organization.getName(), root.get("name").textValue());
  }

  @Test
  public void testEncodeOrganizationInfo() {
    Organization.Info organization = createOrganization().toInfo();
    JsonNode root = mapper.valueToTree(organization);
    assertEquals(organization.getId().toString(), root.get("id").textValue());
    assertEquals(organization.getName(), root.get("name").textValue());
  }

  private Organization createOrganization() {
    return Organization.builder()
            .setId(UUID.randomUUID())
            .setName("organization")
            .build();
  }

}
