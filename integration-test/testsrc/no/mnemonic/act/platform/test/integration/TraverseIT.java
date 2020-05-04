package no.mnemonic.act.platform.test.integration;

import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectIdRequest;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.Test;

public class TraverseIT extends AbstractIT {

  @Test
  public void testTraverseByObjectId() throws Exception {
    // Create objects and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectRecord source = createObject(objectType.getId());
    ObjectRecord destination = createObject(objectType.getId());
    FactRecord fact = createFact(source, destination);

    // ... and check that the Fact can be received via a simple graph traversal.
    fetchAndAssertList(String.format("/v1/traverse/object/%s", source.getId()),
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()"), fact.getId());
  }

  @Test
  public void testTraverseByObjectIdFilterByTime() throws Exception {
    // Create objects and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectRecord source = createObject(objectType.getId());
    ObjectRecord destination = createObject(objectType.getId());

    Long t0 = 98000000L;
    Long beforeT0 = t0 - 10;
    Long afterT0 = t0 + 10;

    FactRecord fact = createFact(
            source, destination,
            createFactType(source.getId()),
            f -> f.setTimestamp(t0).setLastSeenTimestamp(t0));

    String url = String.format("/v1/traverse/object/%s", source.getId());

    // Too late
    fetchAndAssertNone(url,
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()").setAfter(afterT0));

    // Within time filter
    fetchAndAssertList(url,
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()")
                    .setAfter(beforeT0)
                    .setBefore(afterT0),
            fact.getId());
  }

  @Test
  public void testTraverseByObjectIdFilterByRetraction() throws Exception {
    // Create objects and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectRecord source = createObject(objectType.getId());
    ObjectRecord destination = createObject(objectType.getId());

    FactRecord retractedFact = createFact(source, destination);
    retractFact(retractedFact);

    String url = String.format("/v1/traverse/object/%s", source.getId());

    // No filter, includeRetracted defaults to false
    fetchAndAssertNone(url,
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()"));

    // Include retracted facts
    fetchAndAssertList(url,
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()")
                    .setIncludeRetracted(true),
            retractedFact.getId());
  }

  @Test
  public void testTraverseObjectsWithACL() throws Exception {
    // Create one Object and multiple related Facts with different access modes in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectRecord object = createObject(objectType.getId());
    FactRecord fact = createFact(object, factType, f -> f.setAccessMode(FactRecord.AccessMode.Public));
    createFact(object, factType, f -> f.setAccessMode(FactRecord.AccessMode.Explicit));

    // ... and check that only one Fact can be received via a simple graph traversal.
    fetchAndAssertList(String.format("/v1/traverse/object/%s", object.getId()),
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()"), fact.getId());
  }
}
