package no.mnemonic.act.platform.test.integration;

import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectIdRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectTypeValueRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import org.junit.Test;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;

public class TraverseIT extends AbstractIT {

  @Test
  public void testTraverseFromSingleObject() throws Exception {
    // Create objects and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectRecord source = createObject(objectType.getId());
    ObjectRecord destination = createObject(objectType.getId());
    FactRecord fact = createFact(source, destination);

    // ... and check that the Fact can be received via a simple graph traversal.
    fetchAndAssertList(String.format("/v1/traverse/object/%s", source.getId()),
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()"), fact.getId());

    fetchAndAssertList(String.format("/v1/traverse/object/%s/%s", objectType.getName(), source.getValue()),
            new TraverseGraphByObjectTypeValueRequest().setQuery("g.outE()"), fact.getId());
  }

  @Test
  public void testTraverseFromMultipleObjects() throws Exception {
    // Create objects and a related Fact in the database ...
    ObjectTypeEntity threatActor = createObjectType("threatActor");
    ObjectRecord sofacy = createObject(threatActor.getId(), o -> o.setValue("Sofacy"));
    ObjectRecord apt28 = createObject(threatActor.getId(), o -> o.setValue("apt28"));
    ObjectRecord apt29 = createObject(threatActor.getId(), o -> o.setValue("ap29"));
    FactTypeEntity alias = createFactType(threatActor.getId());
    FactRecord fact = createFact(sofacy, apt28, alias, f -> f);
    FactRecord fact2 = createFact(apt29, apt28, alias, f -> f);

    // ... and check that the Fact can be received via a simple graph traversal.
    fetchAndAssertSet("/v1/traverse/objects",
            new TraverseGraphByObjectsRequest()
                    .setQuery("g.outE()")
                    .setObjects(set(sofacy.getId().toString(), apt29.getId().toString())),
            set(fact.getId(), fact2.getId()));
  }

  @Test
  public void testTraverseFromSingleObjectFilterByTime() throws Exception {
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

    String byIdUrl = String.format("/v1/traverse/object/%s", source.getId());

    // Too late
    fetchAndAssertNone(byIdUrl,
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()")
                    .setAfter(afterT0));

    // Within time filter
    fetchAndAssertList(byIdUrl,
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()")
                    .setAfter(beforeT0)
                    .setBefore(afterT0),
            fact.getId());

    String byTypeUrl = String.format("/v1/traverse/object/%s/%s", objectType.getName(), source.getValue());

    // Too late
    fetchAndAssertNone(byTypeUrl,
            new TraverseGraphByObjectTypeValueRequest().setQuery("g.outE()")
                    .setAfter(afterT0));

    // Within time filter
    fetchAndAssertList(byTypeUrl,
            new TraverseGraphByObjectTypeValueRequest().setQuery("g.outE()")
                    .setAfter(beforeT0)
                    .setBefore(afterT0),
            fact.getId());
  }

  @Test
  public void testTraverseFromMultipleObjectFilterByTime() throws Exception {
    // Create objects and a related Fact in the database ...
    ObjectTypeEntity threatActor = createObjectType("threatActor");
    ObjectRecord sofacy = createObject(threatActor.getId(), o -> o.setValue("Sofacy"));
    ObjectRecord apt28 = createObject(threatActor.getId(), o -> o.setValue("apt28"));
    ObjectRecord apt29 = createObject(threatActor.getId(), o -> o.setValue("ap29"));
    FactTypeEntity alias = createFactType(threatActor.getId());

    Long t0 = 98000000L;
    Long t1 = t0 + 100;
    Long beforeT0 = t0 - 10;
    Long betweenT0T1 = t0 + 10;
    Long afterT1 = t1 + 10;

    FactRecord factT0 = createFact(sofacy, apt28, alias, f -> f.setTimestamp(t0).setLastSeenTimestamp(t0));
    FactRecord factT1 = createFact(apt29, apt28, alias, f -> f.setTimestamp(t1).setLastSeenTimestamp(t1));

    // After both
    fetchAndAssertNone("/v1/traverse/objects",
            new TraverseGraphByObjectsRequest().setQuery("g.outE()")
                    .setObjects(set(sofacy.getId().toString(), apt29.getId().toString()))
                    .setAfter(afterT1));

    // Between t0 and t1, should get just one
    fetchAndAssertSet("/v1/traverse/objects",
            new TraverseGraphByObjectsRequest().setQuery("g.outE()")
                    .setObjects(set(sofacy.getId().toString(), apt29.getId().toString()))
                    .setAfter(betweenT0T1)
                    .setBefore(afterT1),
            set(factT1.getId())
    );

    // In between, should get both
    fetchAndAssertSet("/v1/traverse/objects",
            new TraverseGraphByObjectsRequest().setQuery("g.outE()")
                    .setObjects(set(sofacy.getId().toString(), apt29.getId().toString()))
                    .setAfter(beforeT0)
                    .setBefore(afterT1),
            set(factT0.getId(), factT1.getId())
    );
  }

  @Test
  public void testTraverseByObjectFilterByRetraction() throws Exception {
    // Create objects and a related Fact in the database ...
    ObjectTypeEntity objectType = createObjectType();
    ObjectRecord source = createObject(objectType.getId());
    ObjectRecord destination = createObject(objectType.getId());

    FactRecord retractedFact = createFact(source, destination);
    retractFact(retractedFact);

    String byIdUrl = String.format("/v1/traverse/object/%s", source.getId());

    // No filter, includeRetracted defaults to false
    fetchAndAssertNone(byIdUrl,
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()"));

    // Include retracted facts
    fetchAndAssertList(byIdUrl,
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()")
                    .setIncludeRetracted(true),
            retractedFact.getId());


    String byTypeValueUrl = String.format("/v1/traverse/object/%s/%s", objectType.getName(), source.getValue());

    // No filter, includeRetracted defaults to false
    fetchAndAssertNone(byTypeValueUrl,
            new TraverseGraphByObjectTypeValueRequest().setQuery("g.outE()"));

    // Include retracted facts
    fetchAndAssertList(byTypeValueUrl,
            new TraverseGraphByObjectTypeValueRequest().setQuery("g.outE()")
                    .setIncludeRetracted(true),
            retractedFact.getId());
  }

  @Test
  public void testTraverseFromMultipleObjectsFilterByRetraction() throws Exception {
    // Create objects and a related Fact in the database ...
    ObjectTypeEntity threatActor = createObjectType("threatActor");
    ObjectRecord sofacy = createObject(threatActor.getId(), o -> o.setValue("Sofacy"));
    ObjectRecord apt28 = createObject(threatActor.getId(), o -> o.setValue("apt28"));
    ObjectRecord apt29 = createObject(threatActor.getId(), o -> o.setValue("ap29"));
    FactTypeEntity alias = createFactType(threatActor.getId());

    FactRecord fact = createFact(sofacy, apt28, alias, f -> f);
    FactRecord retractedFact = createFact(apt29, apt28, alias, f -> f);
    retractFact(retractedFact);

    // Without retracted
    fetchAndAssertList("/v1/traverse/objects",
            new TraverseGraphByObjectsRequest().setQuery("g.outE()")
                    .setObjects(set(sofacy.getId().toString(), apt29.getId().toString()))
                    .setIncludeRetracted(false),
            fact.getId());

    // Include retracted
    fetchAndAssertSet("/v1/traverse/objects",
            new TraverseGraphByObjectsRequest().setQuery("g.outE()")
                    .setObjects(set(sofacy.getId().toString(), apt29.getId().toString()))
                    .setIncludeRetracted(true),
            set(fact.getId(), retractedFact.getId())
    );
  }

  @Test
  public void testTraverseByObjectWithACL() throws Exception {
    // Create one Object and multiple related Facts with different access modes in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectRecord object = createObject(objectType.getId());
    FactRecord fact = createFact(object, factType, f -> f.setAccessMode(FactRecord.AccessMode.Public));
    createFact(object, factType, f -> f.setAccessMode(FactRecord.AccessMode.Explicit));

    // ... and check that only one Fact can be received via graph traversal.
    fetchAndAssertList(String.format("/v1/traverse/object/%s", object.getId()),
            new TraverseGraphByObjectIdRequest().setQuery("g.outE()"), fact.getId());

    fetchAndAssertList(String.format("/v1/traverse/object/%s/%s", objectType.getName(), object.getValue()),
            new TraverseGraphByObjectTypeValueRequest().setQuery("g.outE()"), fact.getId());

    fetchAndAssertList(
            "/v1/traverse/objects",
            new TraverseGraphByObjectsRequest().setQuery("g.outE()").setObjects(set(object.getId().toString())),
            fact.getId());
  }
}
