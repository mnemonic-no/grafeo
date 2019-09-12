package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.act.platform.api.request.v1.AccessMode;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.dao.cassandra.entity.*;
import no.mnemonic.services.triggers.action.TriggerAction;
import no.mnemonic.services.triggers.action.exceptions.ParameterException;
import no.mnemonic.services.triggers.action.exceptions.TriggerExecutionException;
import no.mnemonic.services.triggers.action.exceptions.TriggerInitializationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class FactIT extends AbstractIT {

  @Mock
  private static TriggerAction action;

  @Before
  public void setup() {
    super.setup();
    initMocks(this);
  }

  @Test
  public void testFetchFact() throws Exception {
    // Create a Fact in the database ...
    FactEntity entity = createFact();

    // ... and check that it can be received via the REST API.
    fetchAndAssertSingle("/v1/fact/uuid/" + entity.getId(), entity.getId());
  }

  @Test
  public void testSearchFacts() throws Exception {
    // Create a Fact in the database ...
    FactEntity entity = createFact();

    // ... and check that it can be found via the REST API.
    fetchAndAssertList("/v1/fact/search", new SearchFactRequest(), entity.getId());
  }

  @Test
  public void testSearchFactsWithFiltering() throws Exception {
    // Create multiple Facts in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectEntity object = createObject(objectType.getId());
    FactEntity fact = createFact(object, factType, f -> f.setValue("fact1"));
    createFact(object, factType, f -> f.setValue("fact2"));

    // ... and check that only one Fact after filtering is found via the REST API.
    fetchAndAssertList("/v1/fact/search", new SearchFactRequest().addFactValue(fact.getValue()), fact.getId());
  }

  @Test
  public void testCreateFact() throws Exception {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());

    // Create a Fact via the REST API ...
    CreateFactRequest request = createCreateFactRequest(objectType, factType);
    Response response = request("/v1/fact").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that both Fact and Object end up in the database.
    UUID id = getIdFromModel(getPayload(response));
    assertNotNull(getFactManager().getFact(id));
    assertNotNull(getFactSearchManager().getFact(id));
    assertNotNull(getObjectManager().getObject(objectType.getName(), "objectValue"));
  }

  @Test
  public void testCreateFactTwice() throws Exception {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());

    // Create the same Fact twice ...
    CreateFactRequest request = createCreateFactRequest(objectType, factType);
    Response response1 = request("/v1/fact").post(Entity.json(request));
    assertEquals(201, response1.getStatus());
    Response response2 = request("/v1/fact").post(Entity.json(request));
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
    Response response = request("/v1/fact").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that both ACL and the comment end up in the database.
    UUID id = getIdFromModel(getPayload(response));
    assertEquals(request.getAcl().size(), getFactManager().fetchFactAcl(id).size());
    assertEquals(1, getFactManager().fetchFactComments(id).size());
  }

  @Test
  public void testCreateFactTriggersAction() throws Exception {
    // Create a Fact via the REST API ...
    CreateFactRequest request = createCreateFactRequest();
    Response response = request("/v1/fact", 1).post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID factID = getIdFromModel(getPayload(response));

    // ... and verify that the action was called with the correct parameters.
    verify(action).trigger(argThat(parameters -> parameters.containsKey("addedFact")
            && Objects.equals(parameters.get("addedFact"), factID.toString())));
  }

  @Test
  public void testFetchMetaFacts() throws Exception {
    // Create a Fact and a related meta Fact in the database ...
    FactEntity referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());
    FactEntity metaFact = createMetaFact(referencedFact, metaFactType, f -> f);

    // ... and check that the meta Fact can be found via the REST API.
    fetchAndAssertList("/v1/fact/uuid/" + referencedFact.getId() + "/meta", metaFact.getId());
  }

  @Test
  public void testFetchMetaFactsWithFiltering() throws Exception {
    // Create a Fact and multiple related meta Facts in the database ...
    FactEntity referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());
    createMetaFact(referencedFact, metaFactType, f -> f.setTimestamp(111111111));
    FactEntity metaFact = createMetaFact(referencedFact, metaFactType, f -> f.setTimestamp(333333333));

    // ... and check that only one meta Fact after filtering is found via the REST API.
    fetchAndAssertList("/v1/fact/uuid/" + referencedFact.getId() + "/meta?after=" + Instant.ofEpochMilli(222222222), metaFact.getId());
  }

  @Test
  public void testCreateMetaFact() throws Exception {
    FactEntity referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());

    // Create a meta Fact via the REST API ...
    CreateMetaFactRequest request = createCreateMetaFactRequest(metaFactType);
    Response response = request("/v1/fact/uuid/" + referencedFact.getId() + "/meta").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that meta Fact ends up in the database.
    FactEntity metaFact = getFactManager().getFact(getIdFromModel(getPayload(response)));
    assertNotNull(metaFact);
    assertNotNull(getFactSearchManager().getFact(metaFact.getId()));
    assertEquals(referencedFact.getId(), metaFact.getInReferenceToID());
  }

  @Test
  public void testCreateMetaFactTwice() throws Exception {
    FactEntity referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());

    // Create the same meta Fact twice ...
    CreateMetaFactRequest request = createCreateMetaFactRequest(metaFactType);
    Response response1 = request("/v1/fact/uuid/" + referencedFact.getId() + "/meta").post(Entity.json(request));
    assertEquals(201, response1.getStatus());
    Response response2 = request("/v1/fact/uuid/" + referencedFact.getId() + "/meta").post(Entity.json(request));
    assertEquals(201, response2.getStatus());

    // ... and check that the Fact was only refreshed.
    JsonNode payload1 = getPayload(response1);
    JsonNode payload2 = getPayload(response2);
    assertEquals(getIdFromModel(payload1), getIdFromModel(payload2));
    assertEquals(payload1.get("timestamp").textValue(), payload2.get("timestamp").textValue());
    assertTrue(Instant.parse(payload1.get("lastSeenTimestamp").textValue()).isBefore(Instant.parse(payload2.get("lastSeenTimestamp").textValue())));
  }

  @Test
  public void testCreateMetaFactWithAclAndComment() throws Exception {
    FactEntity referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());

    // Create a meta Fact with ACL and a comment via the REST API ...
    CreateMetaFactRequest request = createCreateMetaFactRequest(metaFactType)
            .addAcl(UUID.randomUUID())
            .addAcl(UUID.randomUUID())
            .addAcl(UUID.randomUUID())
            .setComment("Hello World!");
    Response response = request("/v1/fact/uuid/" + referencedFact.getId() + "/meta").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that both ACL and the comment end up in the database.
    UUID id = getIdFromModel(getPayload(response));
    assertEquals(request.getAcl().size(), getFactManager().fetchFactAcl(id).size());
    assertEquals(1, getFactManager().fetchFactComments(id).size());
  }

  @Test
  public void testCreateMetaFactTriggersAction() throws Exception {
    FactEntity referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());

    // Create a meta Fact via the REST API ...
    CreateMetaFactRequest request = createCreateMetaFactRequest(metaFactType);
    Response response = request("/v1/fact/uuid/" + referencedFact.getId() + "/meta").post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID factID = getIdFromModel(getPayload(response));

    // ... and verify that the action was called with the correct parameters.
    verify(action).trigger(argThat(parameters -> parameters.containsKey("addedFact")
            && Objects.equals(parameters.get("addedFact"), factID.toString())));
  }

  @Test
  public void testRetractFact() throws Exception {
    // Create a Fact in the database ...
    FactEntity factToRetract = createFact();

    // ... retract it via the REST API ...
    Response response = request("/v1/fact/uuid/" + factToRetract.getId() + "/retract").post(Entity.json(new RetractFactRequest()));
    assertEquals(201, response.getStatus());

    // ... and check that retraction Fact was created correctly.
    FactEntity retractionFact = getFactManager().getFact(getIdFromModel(getPayload(response)));
    assertNotNull(retractionFact);
    assertNotNull(getFactSearchManager().getFact(retractionFact.getId()));
    assertEquals(factToRetract.getId(), retractionFact.getInReferenceToID());
  }

  @Test
  public void testRetractFactTriggersAction() throws Exception {
    // Create a Fact in the database ...
    FactEntity factToRetract = createFact();

    // ... retract it via the REST API ...
    Response response = request("/v1/fact/uuid/" + factToRetract.getId() + "/retract").post(Entity.json(new RetractFactRequest()));
    assertEquals(201, response.getStatus());
    UUID retractionFactID = getIdFromModel(getPayload(response));

    // ... and verify that the action was called with the correct parameters.
    verify(action).trigger(argThat(parameters -> parameters.containsKey("retractionFact")
            && Objects.equals(parameters.get("retractionFact"), retractionFactID.toString())
            && parameters.containsKey("retractedFact")
            && Objects.equals(parameters.get("retractedFact"), factToRetract.getId().toString())
    ));
  }

  @Test
  public void testFetchAcl() throws Exception {
    // Create a Fact with ACL in the database ...
    FactEntity fact = createFact();
    FactAclEntity entry = createAclEntry(fact);

    // ... and check that the ACL can be received via the REST API.
    fetchAndAssertList("/v1/fact/uuid/" + fact.getId() + "/access", entry.getId());
  }

  @Test
  public void testGrantAccess() {
    // Create a Fact in the database ...
    FactEntity fact = createFact();

    // ... grant access to it via the REST API ...
    UUID subject = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Response response = request("/v1/fact/uuid/" + fact.getId() + "/access/" + subject).post(Entity.json(new GrantFactAccessRequest()));
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
    fetchAndAssertList("/v1/fact/uuid/" + fact.getId() + "/comments", comment.getId());
  }

  @Test
  public void testCreateComment() {
    // Create a Fact in the database ...
    FactEntity fact = createFact();

    // ... create a comment via the REST API ...
    CreateFactCommentRequest request = new CreateFactCommentRequest().setComment("Hello World!");
    Response response = request("/v1/fact/uuid/" + fact.getId() + "/comments").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that the comment ends up in the database.
    List<FactCommentEntity> comments = getFactManager().fetchFactComments(fact.getId());
    assertEquals(1, comments.size());
    assertEquals(request.getComment(), comments.get(0).getComment());
  }

  @Test
  public void testFactAccessWithPublicAccess() throws Exception {
    // Create a Fact via the REST API ...
    CreateFactRequest request = createCreateFactRequest()
            .setAccessMode(AccessMode.Public);
    Response response = request("/v1/fact", 1).post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID factID = getIdFromModel(getPayload(response));

    // ... and check who has access to the created Fact. All subjects have access to public Facts.
    assertEquals(200, request("/v1/fact/uuid/" + factID, 1).get().getStatus());
    assertEquals(200, request("/v1/fact/uuid/" + factID, 2).get().getStatus());
    assertEquals(200, request("/v1/fact/uuid/" + factID, 3).get().getStatus());
  }

  @Test
  public void testFactAccessWithRoleBasedAccess() throws Exception {
    // Create a Fact via the REST API ...
    CreateFactRequest request = createCreateFactRequest()
            .setAccessMode(AccessMode.RoleBased);
    Response response = request("/v1/fact", 1).post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID factID = getIdFromModel(getPayload(response));

    // ... and check who has access to the created Fact.
    assertEquals(200, request("/v1/fact/uuid/" + factID, 1).get().getStatus()); // Creator of the Fact.
    assertEquals(403, request("/v1/fact/uuid/" + factID, 2).get().getStatus()); // No access to Organization.
    assertEquals(200, request("/v1/fact/uuid/" + factID, 3).get().getStatus()); // Access via parent Organization.
  }

  @Test
  public void testFactAccessWithExplicitAccess() throws Exception {
    // Create a Fact via the REST API ...
    CreateFactRequest request = createCreateFactRequest()
            .setAccessMode(AccessMode.Explicit)
            .addAcl(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    Response response = request("/v1/fact", 1).post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID factID = getIdFromModel(getPayload(response));

    // ... and check who has access to the created Fact.
    assertEquals(200, request("/v1/fact/uuid/" + factID, 1).get().getStatus()); // Creator of the Fact.
    assertEquals(200, request("/v1/fact/uuid/" + factID, 2).get().getStatus()); // Explicitly granted access.
    assertEquals(403, request("/v1/fact/uuid/" + factID, 3).get().getStatus()); // Not in ACL.
  }

  @Test
  public void testFetchFactSanitizeInReferenceToFact() throws Exception {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectEntity object = createObject(objectType.getId());
    // Create a referenced Fact with explicit access ...
    FactEntity referencedFact = createFact(object, factType, f -> f.setAccessMode(no.mnemonic.act.platform.dao.cassandra.entity.AccessMode.Explicit));
    // ... and reference this Fact from another Fact ...
    FactEntity referencingFact = createFact(object, factType, f -> f.setInReferenceToID(referencedFact.getId()));

    // ... and check that the referenced Fact is not returned by the REST API.
    Response response = request("/v1/fact/uuid/" + referencingFact.getId()).get();
    assertEquals(200, response.getStatus());
    assertTrue(getPayload(response).get("inReferenceTo").isNull());
  }

  private FactAclEntity createAclEntry(FactEntity fact) {
    FactAclEntity entry = new FactAclEntity()
            .setId(UUID.randomUUID())
            .setFactID(fact.getId())
            .setSubjectID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789);

    return getFactManager().saveFactAclEntry(entry);
  }

  private FactCommentEntity createComment(FactEntity fact) {
    FactCommentEntity comment = new FactCommentEntity()
            .setId(UUID.randomUUID())
            .setFactID(fact.getId())
            .setComment("Hello World!")
            .setReplyToID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789);

    return getFactManager().saveFactComment(comment);
  }

  private CreateFactRequest createCreateFactRequest() {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    return createCreateFactRequest(objectType, factType);
  }

  private CreateFactRequest createCreateFactRequest(ObjectTypeEntity objectType, FactTypeEntity factType) {
    return new CreateFactRequest()
            .setType(factType.getName())
            .setValue("factValue")
            .setSourceObject(String.format("%s/%s", objectType.getName(), "objectValue"))
            .setBidirectionalBinding(true);
  }

  private CreateMetaFactRequest createCreateMetaFactRequest(FactTypeEntity factType) {
    return new CreateMetaFactRequest()
            .setType(factType.getName())
            .setValue("factValue");
  }

  public static class TestTriggerAction implements TriggerAction {
    @Override
    public void init(Map<String, String> initParameters) throws ParameterException, TriggerInitializationException {
      action.init(initParameters);
    }

    @Override
    public void trigger(Map<String, String> triggerParameters) throws ParameterException, TriggerExecutionException {
      action.trigger(triggerParameters);
    }
  }

}
