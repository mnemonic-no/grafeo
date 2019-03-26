package no.mnemonic.act.platform.rest.api.v1;

import com.fasterxml.jackson.databind.JsonNode;
import no.mnemonic.act.platform.api.model.v1.AclEntry;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.FactComment;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.rest.AbstractEndpointTest;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

public class FactEndpointTest extends AbstractEndpointTest {

  @Test
  public void testGetFactById() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().getFact(any(), isA(GetFactByIdRequest.class))).then(i -> {
      assertEquals(id, i.<GetFactByIdRequest>getArgument(1).getId());
      return Fact.builder().setId(id).build();
    });

    Response response = target(String.format("/v1/fact/uuid/%s", id)).request().get();
    assertEquals(200, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).getFact(any(), isA(GetFactByIdRequest.class));
  }

  @Test
  public void testSearchFacts() throws Exception {
    when(getTiService().searchFacts(any(), isA(SearchFactRequest.class))).then(i -> StreamingResultSet.<Fact>builder().setValues(createFacts()).build());

    Response response = target("/v1/fact/search").request().post(Entity.json(new SearchFactRequest()));
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).searchFacts(any(), isA(SearchFactRequest.class));
  }

  @Test
  public void testCreateFact() throws Exception {
    UUID id = UUID.randomUUID();
    when(getTiService().createFact(any(), isA(CreateFactRequest.class))).then(i -> Fact.builder().setId(id).build());

    Response response = target("/v1/fact").request().post(Entity.json(createCreateFactRequest()));
    assertEquals(201, response.getStatus());
    assertEquals(id.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).createFact(any(), isA(CreateFactRequest.class));
  }

  @Test
  public void testGetMetaFacts() throws Exception {
    UUID fact = UUID.randomUUID();
    when(getTiService().searchMetaFacts(any(), isA(SearchMetaFactsRequest.class))).then(i -> {
      SearchMetaFactsRequest request = i.getArgument(1);
      assertEquals(fact, request.getFact());
      assertTrue(request.getIncludeRetracted());
      assertEquals(1480520820000L, (long) request.getBefore());
      assertEquals(1480520821000L, (long) request.getAfter());
      assertEquals(25, (int) request.getLimit());
      return StreamingResultSet.<Fact>builder().setValues(createFacts()).build();
    });

    Response response = target(String.format("/v1/fact/uuid/%s/meta", fact))
            .queryParam("includeRetracted", true)
            .queryParam("before", "2016-11-30T15:47:00Z")
            .queryParam("after", "2016-11-30T15:47:01Z")
            .queryParam("limit", 25)
            .request()
            .get();
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).searchMetaFacts(any(), isA(SearchMetaFactsRequest.class));
  }

  @Test
  public void testCreateMetaFact() throws Exception {
    UUID oldFact = UUID.randomUUID();
    UUID newFact = UUID.randomUUID();
    when(getTiService().createMetaFact(any(), isA(CreateMetaFactRequest.class))).then(i -> {
      assertEquals(oldFact, i.<CreateMetaFactRequest>getArgument(1).getFact());
      return Fact.builder().setId(newFact).build();
    });

    Response response = target(String.format("/v1/fact/uuid/%s/meta", oldFact)).request().post(Entity.json(createCreateMetaFactRequest()));
    assertEquals(201, response.getStatus());
    assertEquals(newFact.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).createMetaFact(any(), isA(CreateMetaFactRequest.class));
  }

  @Test
  public void testRetractFact() throws Exception {
    UUID oldFact = UUID.randomUUID();
    UUID newFact = UUID.randomUUID();
    when(getTiService().retractFact(any(), isA(RetractFactRequest.class))).then(i -> {
      assertEquals(oldFact, i.<RetractFactRequest>getArgument(1).getFact());
      return Fact.builder().setId(newFact).build();
    });

    Response response = target(String.format("/v1/fact/uuid/%s/retract", oldFact)).request().post(Entity.json(new RetractFactRequest()));
    assertEquals(201, response.getStatus());
    assertEquals(newFact.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).retractFact(any(), isA(RetractFactRequest.class));
  }

  @Test
  public void testGetFactAcl() throws Exception {
    UUID fact = UUID.randomUUID();
    when(getTiService().getFactAcl(any(), isA(GetFactAclRequest.class))).then(i -> {
      assertEquals(fact, i.<GetFactAclRequest>getArgument(1).getFact());
      return StreamingResultSet.<AclEntry>builder().setValues(createFactAcl()).build();
    });

    Response response = target(String.format("/v1/fact/uuid/%s/access", fact)).request().get();
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).getFactAcl(any(), isA(GetFactAclRequest.class));
  }

  @Test
  public void testGrantFactAccess() throws Exception {
    UUID fact = UUID.randomUUID();
    UUID subject = UUID.randomUUID();
    UUID entry = UUID.randomUUID();
    when(getTiService().grantFactAccess(any(), isA(GrantFactAccessRequest.class))).then(i -> {
      GrantFactAccessRequest request = i.getArgument(1);
      assertEquals(fact, request.getFact());
      assertEquals(subject, request.getSubject());
      return AclEntry.builder().setId(entry).build();
    });

    Response response = target(String.format("/v1/fact/uuid/%s/access/%s", fact, subject)).request().post(Entity.json(new GrantFactAccessRequest()));
    assertEquals(201, response.getStatus());
    assertEquals(entry.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).grantFactAccess(any(), isA(GrantFactAccessRequest.class));
  }

  @Test
  public void testGetFactComments() throws Exception {
    UUID fact = UUID.randomUUID();
    when(getTiService().getFactComments(any(), isA(GetFactCommentsRequest.class))).then(i -> {
      GetFactCommentsRequest request = i.getArgument(1);
      assertEquals(fact, request.getFact());
      assertEquals(1480520820000L, (long) request.getBefore());
      assertEquals(1480520821000L, (long) request.getAfter());
      return StreamingResultSet.<FactComment>builder().setValues(createFactComments()).build();
    });

    Response response = target(String.format("/v1/fact/uuid/%s/comments", fact))
            .queryParam("before", "2016-11-30T15:47:00Z")
            .queryParam("after", "2016-11-30T15:47:01Z")
            .request()
            .get();
    JsonNode payload = getPayload(response);
    assertEquals(200, response.getStatus());
    assertTrue(payload.isArray());
    assertEquals(3, payload.size());

    verify(getTiService(), times(1)).getFactComments(any(), isA(GetFactCommentsRequest.class));
  }

  @Test
  public void testCreateFactComment() throws Exception {
    UUID fact = UUID.randomUUID();
    UUID comment = UUID.randomUUID();
    when(getTiService().createFactComment(any(), isA(CreateFactCommentRequest.class))).then(i -> {
      assertEquals(fact, i.<CreateFactCommentRequest>getArgument(1).getFact());
      return FactComment.builder().setId(comment).build();
    });

    Response response = target(String.format("/v1/fact/uuid/%s/comments", fact)).request().post(Entity.json(createCreateFactCommentRequest()));
    assertEquals(201, response.getStatus());
    assertEquals(comment.toString(), getPayload(response).get("id").textValue());

    verify(getTiService(), times(1)).createFactComment(any(), isA(CreateFactCommentRequest.class));
  }

  private Collection<Fact> createFacts() {
    Collection<Fact> facts = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      facts.add(Fact.builder().setId(UUID.randomUUID()).build());
    }
    return facts;
  }

  private Collection<AclEntry> createFactAcl() {
    Collection<AclEntry> entries = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      entries.add(AclEntry.builder().setId(UUID.randomUUID()).build());
    }
    return entries;
  }

  private Collection<FactComment> createFactComments() {
    Collection<FactComment> comments = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      comments.add(FactComment.builder().setId(UUID.randomUUID()).build());
    }
    return comments;
  }

  private CreateFactRequest createCreateFactRequest() {
    return new CreateFactRequest()
            .setType("type")
            .setValue("value");
  }

  private CreateMetaFactRequest createCreateMetaFactRequest() {
    return new CreateMetaFactRequest()
            .setType("type")
            .setValue("value");
  }

  private CreateFactCommentRequest createCreateFactCommentRequest() {
    return new CreateFactCommentRequest()
            .setComment("Comment");
  }

}
