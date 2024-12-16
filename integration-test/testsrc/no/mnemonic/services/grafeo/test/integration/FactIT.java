package no.mnemonic.services.grafeo.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.request.v1.*;
import no.mnemonic.services.grafeo.dao.api.record.FactAclEntryRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactCommentRecord;
import no.mnemonic.services.grafeo.dao.api.record.FactRecord;
import no.mnemonic.services.grafeo.dao.api.record.ObjectRecord;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactAclEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.services.grafeo.dao.cassandra.entity.OriginEntity;
import no.mnemonic.services.triggers.action.TriggerAction;
import no.mnemonic.services.triggers.action.exceptions.ParameterException;
import no.mnemonic.services.triggers.action.exceptions.TriggerExecutionException;
import no.mnemonic.services.triggers.action.exceptions.TriggerInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FactIT extends AbstractIT {

  @Mock
  private static TriggerAction action;

  private static CompletableFuture<Boolean> actionTriggered;

  @BeforeEach
  public void setup() {
    super.setup();

    actionTriggered = new CompletableFuture<>();
  }

  @Test
  public void testFetchFact() throws Exception {
    // Create a Fact in the database ...
    FactRecord fact = createFact();

    // ... and check that it can be received via the REST API.
    fetchAndAssertSingle("/v1/fact/uuid/" + fact.getId(), fact.getId());
  }

  @Test
  public void testSearchFactsWithFiltering() throws Exception {
    // Create multiple Facts in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectRecord object = createObject(objectType.getId());
    FactRecord fact = createFact(object, factType, f -> f.setValue("fact1"));
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
    assertNotNull(getObjectFactDao().getFact(getIdFromModel(getPayload(response))));
    assertNotNull(getObjectFactDao().getObject(objectType.getName(), "objectValue"));
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
  public void testCreateFactTwiceWithExplicitAccess() throws Exception {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    OriginEntity origin = createOrigin();
    CreateFactRequest request = new CreateFactRequest()
            .setType(factType.getName())
            .setValue("factValue")
            .setOrganization(UUID.fromString("00000000-0000-0000-0000-000000000001").toString())
            .setOrigin(origin.getName())
            .setConfidence(0.75f)
            .setAccessMode(AccessMode.Explicit)
            .setSourceObject(String.format("%s/%s", objectType.getName(), "objectValue"))
            .setBidirectionalBinding(true);

    // Create the same Fact twice with different users ...
    Response response1 = request("/v1/fact", 1).post(Entity.json(request));
    assertEquals(201, response1.getStatus());
    Response response2 = request("/v1/fact", 3).post(Entity.json(request));
    assertEquals(201, response2.getStatus());

    // ... and check that the Fact was only refreshed.
    JsonNode payload1 = getPayload(response1);
    JsonNode payload2 = getPayload(response2);
    assertEquals(getIdFromModel(payload1), getIdFromModel(payload2));
    assertEquals(payload1.get("timestamp").textValue(), payload2.get("timestamp").textValue());
    assertTrue(Instant.parse(payload1.get("lastSeenTimestamp").textValue()).isBefore(Instant.parse(payload2.get("lastSeenTimestamp").textValue())));

    // Also verify that both users are in the Fact's ACL.
    assertEquals(
            SetUtils.set(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000003")),
            SetUtils.set(getFactManager().fetchFactAcl(getIdFromModel(payload1)), FactAclEntity::getSubjectID)
    );
  }

  @Test
  public void testCreateFactWithAclAndComment() throws Exception {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());

    // Create a Fact with ACL and a comment via the REST API ...
    CreateFactRequest request = createCreateFactRequest(objectType, factType)
            .addAcl("Max Mustermann")
            .setComment("Hello World!");
    Response response = request("/v1/fact").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that both ACL and the comment end up in the database.
    FactRecord fact = getObjectFactDao().getFact(getIdFromModel(getPayload(response)));
    assertEquals(request.getAcl().size(), fact.getAcl().size());
    assertEquals(1, fact.getComments().size());
  }

  @Test
  public void testCreateFactTriggersAction() throws Exception {
    // Create a Fact via the REST API ...
    CreateFactRequest request = createCreateFactRequest();
    Response response = request("/v1/fact", 1).post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID factID = getIdFromModel(getPayload(response));

    // ... and verify that the action was called with the correct parameters.
    assertTrue(actionTriggered.get(5, TimeUnit.SECONDS));
    verify(action).trigger(argThat(parameters -> parameters.containsKey("addedFact")
            && Objects.equals(parameters.get("addedFact"), factID.toString())));
  }

  @Test
  public void testFetchMetaFacts() throws Exception {
    // Create a Fact and a related meta Fact in the database ...
    FactRecord referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());
    FactRecord metaFact = createMetaFact(referencedFact, metaFactType, f -> f);

    // ... and check that the meta Fact can be found via the REST API.
    fetchAndAssertList("/v1/fact/uuid/" + referencedFact.getId() + "/meta", metaFact.getId());
  }

  @Test
  public void testFetchMetaFactsWithFiltering() throws Exception {
    long now = System.currentTimeMillis();

    // Create a Fact and multiple related meta Facts in the database ...
    FactRecord referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());
    createMetaFact(referencedFact, metaFactType, f -> f.setLastSeenTimestamp(now - 2000));
    FactRecord metaFact = createMetaFact(referencedFact, metaFactType, f -> f.setLastSeenTimestamp(now));

    // ... and check that only one meta Fact after filtering is found via the REST API.
    fetchAndAssertList("/v1/fact/uuid/" + referencedFact.getId() + "/meta?after=" + Instant.ofEpochMilli(now - 1000), metaFact.getId());
  }

  @Test
  public void testCreateMetaFact() throws Exception {
    FactRecord referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());

    // Create a meta Fact via the REST API ...
    CreateMetaFactRequest request = createCreateMetaFactRequest(metaFactType);
    Response response = request("/v1/fact/uuid/" + referencedFact.getId() + "/meta").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that meta Fact ends up in the database.
    FactRecord metaFact = getObjectFactDao().getFact(getIdFromModel(getPayload(response)));
    assertNotNull(metaFact);
    assertEquals(referencedFact.getId(), metaFact.getInReferenceToID());
  }

  @Test
  public void testCreateMetaFactTwice() throws Exception {
    FactRecord referencedFact = createFact();
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
    FactRecord referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());

    // Create a meta Fact with ACL and a comment via the REST API ...
    CreateMetaFactRequest request = createCreateMetaFactRequest(metaFactType)
            .addAcl("00000000-0000-0000-0000-000000000002")
            .setComment("Hello World!");
    Response response = request("/v1/fact/uuid/" + referencedFact.getId() + "/meta").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that both ACL and the comment end up in the database.
    FactRecord metaFact = getObjectFactDao().getFact(getIdFromModel(getPayload(response)));
    assertEquals(request.getAcl().size(), metaFact.getAcl().size());
    assertEquals(1, metaFact.getComments().size());
  }

  @Test
  public void testCreateMetaFactTriggersAction() throws Exception {
    FactRecord referencedFact = createFact();
    FactTypeEntity metaFactType = createMetaFactType(referencedFact.getTypeID());

    // Create a meta Fact via the REST API ...
    CreateMetaFactRequest request = createCreateMetaFactRequest(metaFactType);
    Response response = request("/v1/fact/uuid/" + referencedFact.getId() + "/meta").post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID factID = getIdFromModel(getPayload(response));

    // ... and verify that the action was called with the correct parameters.
    assertTrue(actionTriggered.get(5, TimeUnit.SECONDS));
    verify(action).trigger(argThat(parameters -> parameters.containsKey("addedFact")
            && Objects.equals(parameters.get("addedFact"), factID.toString())));
  }

  @Test
  public void testRetractFact() throws Exception {
    // Create a Fact in the database ...
    FactRecord factToRetract = createFact();

    // ... retract it via the REST API ...
    Response response = request("/v1/fact/uuid/" + factToRetract.getId() + "/retract").post(Entity.json(new RetractFactRequest()));
    assertEquals(201, response.getStatus());

    // ... and check that retraction Fact was created correctly.
    FactRecord retractionFact = getObjectFactDao().getFact(getIdFromModel(getPayload(response)));
    assertNotNull(retractionFact);
    assertEquals(factToRetract.getId(), retractionFact.getInReferenceToID());
  }

  @Test
  public void testRetractFactTwice() throws Exception {
    // Create a Fact in the database ...
    FactRecord factToRetract = createFact();

    // ... retract it via the REST API twice ...
    Response response1 = request("/v1/fact/uuid/" + factToRetract.getId() + "/retract").post(Entity.json(new RetractFactRequest()));
    assertEquals(201, response1.getStatus());
    Response response2 = request("/v1/fact/uuid/" + factToRetract.getId() + "/retract").post(Entity.json(new RetractFactRequest()));
    assertEquals(201, response2.getStatus());

    // ... and check that the retraction was refreshed.
    JsonNode payload1 = getPayload(response1);
    JsonNode payload2 = getPayload(response2);
    assertEquals(getIdFromModel(payload1), getIdFromModel(payload2));
    assertEquals(payload1.get("timestamp").textValue(), payload2.get("timestamp").textValue());
    assertTrue(Instant.parse(payload1.get("lastSeenTimestamp").textValue()).isBefore(Instant.parse(payload2.get("lastSeenTimestamp").textValue())));
  }

  @Test
  public void testRetractFactTriggersAction() throws Exception {
    // Create a Fact in the database ...
    FactRecord factToRetract = createFact();

    // ... retract it via the REST API ...
    Response response = request("/v1/fact/uuid/" + factToRetract.getId() + "/retract").post(Entity.json(new RetractFactRequest()));
    assertEquals(201, response.getStatus());
    UUID retractionFactID = getIdFromModel(getPayload(response));

    // ... and verify that the action was called with the correct parameters.
    assertTrue(actionTriggered.get(5, TimeUnit.SECONDS));
    verify(action).trigger(argThat(parameters -> parameters.containsKey("retractionFact")
            && Objects.equals(parameters.get("retractionFact"), retractionFactID.toString())
            && parameters.containsKey("retractedFact")
            && Objects.equals(parameters.get("retractedFact"), factToRetract.getId().toString())
    ));
  }

  @Test
  public void testFetchAcl() throws Exception {
    // Create a Fact with ACL in the database ...
    FactRecord fact = createFact();
    FactAclEntryRecord entry = createAclEntry(fact);

    // ... and check that the ACL can be received via the REST API.
    fetchAndAssertList("/v1/fact/uuid/" + fact.getId() + "/access", entry.getId());
  }

  @Test
  public void testGrantAccess() {
    // Create a Fact in the database ...
    FactRecord fact = createFact();

    // ... grant access to it via the REST API ...
    UUID subject = UUID.fromString("00000000-0000-0000-0000-000000000001");
    Response response = request("/v1/fact/uuid/" + fact.getId() + "/access/" + subject).post(Entity.json(new GrantFactAccessRequest()));
    assertEquals(201, response.getStatus());

    // ... and check that the ACL entry ends up in the database.
    List<FactAclEntryRecord> acl = getObjectFactDao().getFact(fact.getId()).getAcl();
    assertEquals(1, acl.size());
    assertEquals(subject, acl.get(0).getSubjectID());
  }

  @Test
  public void testFetchComments() throws Exception {
    // Create a Fact with a comment in the database ...
    FactRecord fact = createFact();
    FactCommentRecord comment = createComment(fact);

    // ... and check that the comment can be received via the REST API.
    fetchAndAssertList("/v1/fact/uuid/" + fact.getId() + "/comments", comment.getId());
  }

  @Test
  public void testCreateComment() {
    // Create a Fact in the database ...
    FactRecord fact = createFact();

    // ... create a comment via the REST API ...
    CreateFactCommentRequest request = new CreateFactCommentRequest().setComment("Hello World!");
    Response response = request("/v1/fact/uuid/" + fact.getId() + "/comments").post(Entity.json(request));
    assertEquals(201, response.getStatus());

    // ... and check that the comment ends up in the database.
    List<FactCommentRecord> comments = getObjectFactDao().getFact(fact.getId()).getComments();
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
            .addAcl("00000000-0000-0000-0000-000000000002");
    Response response = request("/v1/fact", 1).post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID factID = getIdFromModel(getPayload(response));

    // ... and check who has access to the created Fact.
    assertEquals(200, request("/v1/fact/uuid/" + factID, 1).get().getStatus()); // Creator of the Fact.
    assertEquals(200, request("/v1/fact/uuid/" + factID, 2).get().getStatus()); // Explicitly granted access.
    assertEquals(403, request("/v1/fact/uuid/" + factID, 3).get().getStatus()); // Not in ACL.
  }

  @Test
  public void testFactAccessWithExplicitAccessViaGroup() throws Exception {
    // Create a Fact via the REST API ...
    CreateFactRequest request = createCreateFactRequest()
            .setAccessMode(AccessMode.Explicit)
            .addAcl("00000000-0000-0000-0000-000000000004");
    Response response = request("/v1/fact", 1).post(Entity.json(request));
    assertEquals(201, response.getStatus());
    UUID factID = getIdFromModel(getPayload(response));

    // ... and check who has access to the created Fact.
    assertEquals(200, request("/v1/fact/uuid/" + factID, 1).get().getStatus()); // Creator of the Fact.
    assertEquals(200, request("/v1/fact/uuid/" + factID, 2).get().getStatus()); // Explicitly granted access via group.
    assertEquals(403, request("/v1/fact/uuid/" + factID, 3).get().getStatus()); // Not in ACL.
  }

  @Test
  public void testFetchFactSanitizeInReferenceToFact() throws Exception {
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectRecord object = createObject(objectType.getId());
    // Create a referenced Fact with explicit access ...
    FactRecord referencedFact = createFact(object, factType, f -> f.setAccessMode(FactRecord.AccessMode.Explicit));
    // ... and reference this Fact from another Fact ...
    FactRecord referencingFact = createFact(object, factType, f -> f.setInReferenceToID(referencedFact.getId()));

    // ... and check that the referenced Fact is not returned by the REST API.
    Response response = request("/v1/fact/uuid/" + referencingFact.getId()).get();
    assertEquals(200, response.getStatus());
    assertTrue(getPayload(response).get("inReferenceTo").isNull());
  }

  @Test
  public void testFetchFactWithRetractedFlag() throws Exception {
    // Create a Fact in the database ...
    FactRecord factToRetract = createFact();

    // ... retract it via the REST API using a more restrictive access mode ...
    RetractFactRequest request = new RetractFactRequest()
            .setAccessMode(AccessMode.Explicit);
    assertEquals(201, request("/v1/fact/uuid/" + factToRetract.getId() + "/retract").post(Entity.json(request)).getStatus());

    // ... and check that the 'Retracted' flag is calculated based on access to the retraction Fact.
    JsonNode payload1 = getPayload(request("/v1/fact/uuid/" + factToRetract.getId(), 1).get());
    assertEquals(SetUtils.set("Retracted"), SetUtils.set(payload1.get("flags").iterator(), JsonNode::asText));
    JsonNode payload2 = getPayload(request("/v1/fact/uuid/" + factToRetract.getId(), 3).get());
    assertTrue(payload2.get("flags").isEmpty());
  }

  private FactAclEntryRecord createAclEntry(FactRecord fact) {
    FactAclEntryRecord entry = new FactAclEntryRecord()
            .setId(UUID.randomUUID())
            .setSubjectID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789);

    return getObjectFactDao().storeFactAclEntry(fact, entry);
  }

  private FactCommentRecord createComment(FactRecord fact) {
    FactCommentRecord comment = new FactCommentRecord()
            .setId(UUID.randomUUID())
            .setComment("Hello World!")
            .setReplyToID(UUID.randomUUID())
            .setOriginID(UUID.randomUUID())
            .setTimestamp(123456789);

    return getObjectFactDao().storeFactComment(fact, comment);
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
      actionTriggered.complete(true);
    }
  }

}
