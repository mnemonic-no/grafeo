package no.mnemonic.act.platform.api.model.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class SubjectTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testEncodeSubject() {
    Subject subject = createSubject();
    JsonNode root = mapper.valueToTree(subject);
    assertEquals(subject.getId().toString(), root.get("id").textValue());
    assertEquals(subject.getName(), root.get("name").textValue());
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
            .build();
  }

}
