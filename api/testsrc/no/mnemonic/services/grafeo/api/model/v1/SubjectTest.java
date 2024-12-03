package no.mnemonic.services.grafeo.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubjectTest {

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  @Test
  public void testEncodeSubject() {
    Subject subject = createSubject();
    JsonNode root = mapper.valueToTree(subject);
    assertEquals(subject.getId().toString(), root.get("id").textValue());
    assertEquals(subject.getName(), root.get("name").textValue());
    assertTrue(root.get("organization").isObject());
  }

  @Test
  public void testEncodeSubjectInfo() {
    Subject.Info subject = createSubject().toInfo();
    JsonNode root = mapper.valueToTree(subject);
    assertEquals(subject.getId().toString(), root.get("id").textValue());
    assertEquals(subject.getName(), root.get("name").textValue());
  }

  private Subject createSubject() {
    return Subject.builder()
            .setId(UUID.randomUUID())
            .setName("subject")
            .setOrganization(Organization.builder().setId(UUID.randomUUID()).setName("organization").build().toInfo())
            .build();
  }

}
