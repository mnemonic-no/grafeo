package no.mnemonic.act.platform.test.integration;

import no.mnemonic.act.platform.api.request.v1.SearchObjectRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectSearchRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphByObjectsRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseGraphRequest;
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
            new TraverseGraphRequest().setQuery("g.outE()"), fact.getId());

    fetchAndAssertList(String.format("/v1/traverse/object/%s/%s", objectType.getName(), source.getValue()),
            new TraverseGraphRequest().setQuery("g.outE()"), fact.getId());
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

    // Traverse from set of objects
    fetchAndAssertSet("/v1/traverse/objects",
            new TraverseGraphByObjectsRequest()
                    .setQuery("g.outE()")
                    .setObjects(set(sofacy.getId().toString(), apt29.getId().toString())),
            set(fact.getId(), fact2.getId()));

    // Traverse after searching for objects
    fetchAndAssertSet("/v1/traverse/objects/search",
            new TraverseGraphByObjectSearchRequest()
                    .setSearch(new SearchObjectRequest().setObjectID(set(sofacy.getId(), apt29.getId())))
                    .setTraverse(new TraverseGraphRequest()
                            .setQuery("g.outE()")),
            set(fact.getId(), fact2.getId()));
  }

  @Test
  public void testTraverseFromMultipleObjectsWithACL() throws Exception {
    // Create one Object and multiple related Facts with different access modes in the database ...
    ObjectTypeEntity objectType = createObjectType();
    FactTypeEntity factType = createFactType(objectType.getId());
    ObjectRecord object = createObject(objectType.getId());
    FactRecord fact = createFact(object, factType, f -> f.setAccessMode(FactRecord.AccessMode.Public));
    createFact(object, factType, f -> f.setAccessMode(FactRecord.AccessMode.Explicit));

    // ... and check that only one Fact can be received via graph traversal.
    fetchAndAssertList(
            "/v1/traverse/objects",
            new TraverseGraphByObjectsRequest()
                    .setQuery("g.outE()")
                    .setObjects(set(object.getId().toString())),
            fact.getId());
  }
}
