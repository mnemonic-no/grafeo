package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.request.v1.Direction;
import no.mnemonic.act.platform.entity.cassandra.*;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class FactIT extends AbstractIT {

  @Test
  public void testFetchFact() throws Exception {
    // Create a Fact in the database ...
    FactEntity entity = createFact();

    // ... and check that it can be received via the REST API.
    Response response = target("/v1/fact/uuid/" + entity.getId()).request().get();
    assertEquals(200, response.getStatus());
    assertEquals(entity.getId(), getIdFromModel(getPayload(response)));
  }

  @Test
  public void testCreateFact() throws Exception {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());

    // Create a Fact via the REST API ...
    CreateFactRequest request = createCreateFactRequest(objectType, factType);
    Response response = target("/v1/fact").request().post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that both Fact and Object end up in the database.
    assertNotNull(getFactManager().getFact(getIdFromModel(getPayload(response))));
    assertNotNull(getObjectManager().getObject(objectType.getName(), request.getBindings().get(0).getObjectValue()));
  }

  @Test
  public void testCreateFactTwice() throws Exception {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());

    // Create the same Fact twice ...
    CreateFactRequest request = createCreateFactRequest(objectType, factType);
    Response response1 = target("/v1/fact").request().post(Entity.json(request));
    assertEquals(201, response1.getStatus());
    Response response2 = target("/v1/fact").request().post(Entity.json(request));
    assertEquals(201, response2.getStatus());

    // ... and check that the Fact was only refreshed.
    JsonNode payload1 = getPayload(response1);
    JsonNode payload2 = getPayload(response2);
    assertEquals(getIdFromModel(payload1), getIdFromModel(payload2));
    assertEquals(payload1.get("timestamp").textValue(), payload2.get("timestamp").textValue());
    assertTrue(Instant.parse(payload1.get("lastSeenTimestamp").textValue()).isBefore(Instant.parse(payload2.get("lastSeenTimestamp").textValue())));
  }

  @Test
  public void testCreateFactWithAclAndComment() throws Exception {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());

    // Create a Fact with ACL and a comment via the REST API ...
    CreateFactRequest request = createCreateFactRequest(objectType, factType)
            .addAcl(UUID.randomUUID())
            .addAcl(UUID.randomUUID())
            .addAcl(UUID.randomUUID())
            .setComment("Hello World!");
    Response response = target("/v1/fact").request().post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that both ACL and the comment end up in the database.
    UUID id = getIdFromModel(getPayload(response));
    assertEquals(request.getAcl().size(), getFactManager().fetchFactAcl(id).size());
    assertEquals(1, getFactManager().fetchFactComments(id).size());
  }

  @Test
  public void testRetractFact() throws Exception {
    // Create a Fact in the database ...
    FactEntity factToRetract = createFact();

    // ... retract it via the REST API ...
    Response response = target("/v1/fact/uuid/" + factToRetract.getId() + "/retract").request().post(Entity.json(new RetractFactRequest()));
    assertEquals(201, response.getStatus());

    // ... and check that retraction Fact was created correctly.
    FactEntity retractionFact = getFactManager().getFact(getIdFromModel(getPayload(response)));
    assertNotNull(retractionFact);
    assertEquals(factToRetract.getId(), retractionFact.getInReferenceToID());
  }

  @Test
  public void testFetchAcl() throws Exception {
    // Create a Fact with ACL in the database ...
    FactEntity fact = createFact();
    FactAclEntity entry = createAclEntry(fact);

    // ... and check that the ACL can be received via the REST API.
    Response response = target("/v1/fact/uuid/" + fact.getId() + "/access").request().get();
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertEquals(entry.getId(), getIdFromModel(data.get(0)));
  }

  @Test
  public void testGrantAccess() throws Exception {
    // Create a Fact in the database ...
    FactEntity fact = createFact();

    // ... grant access to it via the REST API ...
    UUID subject = UUID.randomUUID();
    Response response = target("/v1/fact/uuid/" + fact.getId() + "/access/" + subject).request().post(Entity.json(new GrantFactAccessRequest()));
    assertEquals(201, response.getStatus());

    // ... and check that the ACL entry ends up in the database.
    List<FactAclEntity> acl = getFactManager().fetchFactAcl(fact.getId());
    assertEquals(1, acl.size());
    assertEquals(subject, acl.get(0).getSubjectID());
  }

  @Test
  public void testFetchComments() throws Exception {
    // Create a Fact with a comment in the database ...
    FactEntity fact = createFact();
    FactCommentEntity comment = createComment(fact);

    // ... and check that the comment can be received via the REST API.
    Response response = target("/v1/fact/uuid/" + fact.getId() + "/comments").request().get();
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertEquals(comment.getId(), getIdFromModel(data.get(0)));
  }

  @Test
  public void testCreateComment() throws Exception {
    // Create a Fact in the database ...
    FactEntity fact = createFact();

    // ... create a comment via the REST API ...
    CreateFactCommentRequest request = new CreateFactCommentRequest().setComment("Hello World!");
    Response response = target("/v1/fact/uuid/" + fact.getId() + "/comments").request().post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that the comment ends up in the database.
    List<FactCommentEntity> comments = getFactManager().fetchFactComments(fact.getId());
    assertEquals(1, comments.size());
    assertEquals(request.getComment(), comments.get(0).getComment());
  }

  private FactAclEntity createAclEntry(FactEntity fact) {
    FactAclEntity entry = new FactAclEntity()
            .setId(UUID.randomUUID())
            .setFactID(fact.getId())
            .setSubjectID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setTimestamp(123456789);

    return getFactManager().saveFactAclEntry(entry);
  }

  private FactCommentEntity createComment(FactEntity fact) {
    FactCommentEntity comment = new FactCommentEntity()
            .setId(UUID.randomUUID())
            .setFactID(fact.getId())
            .setComment("Hello World!")
            .setReplyToID(UUID.randomUUID())
            .setSourceID(UUID.randomUUID())
            .setTimestamp(123456789);

    return getFactManager().saveFactComment(comment);
  }

  private CreateFactRequest createCreateFactRequest(ObjectTypeEntity objectType, FactTypeEntity factType) {
    return new CreateFactRequest()
            .setType(factType.getName())
            .setValue("factValue")
            .addBinding(new CreateFactRequest.FactObjectBinding()
                    .setObjectType(objectType.getName())
                    .setObjectValue("objectValue")
                    .setDirection(Direction.BiDirectional)
            );
  }

}
