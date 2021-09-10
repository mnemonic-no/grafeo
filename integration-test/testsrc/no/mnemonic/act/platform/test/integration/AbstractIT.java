package no.mnemonic.act.platform.test.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.dao.api.ObjectFactDao;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.act.platform.dao.api.record.ObjectRecord;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.dao.cassandra.OriginManager;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectTypeEntity;
import no.mnemonic.act.platform.dao.elastic.FactSearchManager;
import no.mnemonic.act.platform.rest.modules.TiClientModule;
import no.mnemonic.act.platform.rest.modules.TiRestModule;
import no.mnemonic.act.platform.service.modules.TiServerModule;
import no.mnemonic.act.platform.service.modules.TiServiceModule;
import no.mnemonic.act.platform.service.ti.resolvers.request.FactTypeRequestResolver;
import no.mnemonic.commons.container.ComponentContainer;
import no.mnemonic.commons.container.providers.GuiceBeanProvider;
import no.mnemonic.commons.junit.docker.CassandraDockerResource;
import no.mnemonic.commons.junit.docker.DockerResource;
import no.mnemonic.commons.junit.docker.ElasticSearchDockerResource;
import no.mnemonic.commons.testtools.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static no.mnemonic.commons.utilities.collections.SetUtils.set;
import static org.junit.Assert.assertEquals;

public abstract class AbstractIT {

  private static final String ACL_FILE = ClassLoader.getSystemResource("acl.properties").getPath();
  private static final String RESOURCES_FOLDER = ClassLoader.getSystemResource("").getPath();
  private static final int API_SERVER_PORT = AvailablePortFinder.getAvailablePort(8000);

  private static final ObjectMapper mapper = JsonMapper.builder().build();

  private ComponentContainer serviceContainer;
  private ComponentContainer restContainer;
  private OriginManager originManager;
  private ObjectManager objectManager;
  private FactManager factManager;
  private ObjectFactDao objectFactDao;

  private static CassandraDockerResource cassandra = CassandraDockerResource.builder()
          .setImageName("cassandra")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9042)
          .skipReachabilityCheck()
          .setTruncateScript("truncate.cql")
          .build();

  private static ElasticSearchDockerResource elastic = ElasticSearchDockerResource.builder()
          // Need to specify the exact version here because Elastic doesn't publish images with the 'latest' tag.
          // Usually this should be the same version as the ElasticSearch client used.
          .setImageName("elasticsearch/elasticsearch:7.12.1")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(9200)
          .skipReachabilityCheck()
          .addEnvironmentVariable("discovery.type", "single-node")
          .build();

  private static DockerResource activemq = DockerResource.builder()
          .setImageName("webcenter/activemq")
          .setExposedPortsRange("15000-25000")
          .addApplicationPort(61616)
          .addEnvironmentVariable("ACTIVEMQ_CONFIG_QUEUES_ACT", "Service.ACT")
          .build();

  @ClassRule
  // Chain resources in order to allow ActiveMQ to start up properly while Cassandra and ElasticSearch are initializing.
  public static TestRule chain = RuleChain.outerRule(activemq).around(cassandra).around(elastic);

  @Before
  public void setup() {
    // Start up service layer.
    GuiceBeanProvider serviceBeanProvider = new GuiceBeanProvider(new ServiceModuleIT());
    serviceContainer = ComponentContainer.create(serviceBeanProvider);
    serviceContainer.initialize();

    // Start up REST layer.
    GuiceBeanProvider restBeanProvider = new GuiceBeanProvider(new RestModuleIT());
    restContainer = ComponentContainer.create(restBeanProvider);
    restContainer.initialize();

    // Store references to managers used by tests.
    originManager = serviceBeanProvider.getBean(OriginManager.class).orElseThrow(IllegalStateException::new);
    objectManager = serviceBeanProvider.getBean(ObjectManager.class).orElseThrow(IllegalStateException::new);
    factManager = serviceBeanProvider.getBean(FactManager.class).orElseThrow(IllegalStateException::new);
    // GuiceBeanProvider only returns singleton objects, thus, an ObjectFactDao instance must be fetched directly from Guice.
    objectFactDao = serviceBeanProvider.getInjector().getInstance(ObjectFactDao.class);

    // Configure FactSearchManager with test environment in order to make indexed documents available for search immediately.
    serviceBeanProvider.getBean(FactSearchManager.class)
            .orElseThrow(IllegalStateException::new)
            .setTestEnvironment(true);
  }

  @After
  public void teardown() {
    restContainer.destroy();
    serviceContainer.destroy();

    // Truncate database.
    cassandra.truncate();
    // Truncate indices.
    elastic.deleteIndices();
  }

  /* Getters */

  OriginManager getOriginManager() {
    return originManager;
  }

  ObjectManager getObjectManager() {
    return objectManager;
  }

  FactManager getFactManager() {
    return factManager;
  }

  ObjectFactDao getObjectFactDao() {
    return objectFactDao;
  }

  /* Helpers for REST */

  Invocation.Builder request(String url) {
    return request(url, 1);
  }

  Invocation.Builder request(String url, long actUserID) {
    return ClientBuilder.newClient()
            .target("http://localhost:" + API_SERVER_PORT + url)
            .request()
            .header("ACT-User-ID", actUserID);
  }

  JsonNode getPayload(Response response) throws IOException {
    // Return the payload in the "data" field of the returned ResultStash.
    return mapper.readTree(response.readEntity(String.class)).get("data");
  }

  UUID getIdFromModel(JsonNode node) {
    return UUID.fromString(node.get("id").textValue());
  }

  /* Helpers for assertions */

  void fetchAndAssertSingle(String url, UUID id) throws Exception {
    Response response = request(url).get();
    assertEquals(200, response.getStatus());
    assertEquals(id, getIdFromModel(getPayload(response)));
  }

  void fetchAndAssertList(String url, UUID id) throws Exception {
    Response response = request(url).get();
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertEquals(id, getIdFromModel(data.get(0)));
  }

  void fetchAndAssertList(String url, ValidatingRequest request, UUID id) throws Exception {
    Response response = request(url).post(Entity.json(request));
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(1, data.size());
    assertEquals(id, getIdFromModel(data.get(0)));
  }

  void fetchAndAssertSet(String url, ValidatingRequest request, Set<UUID> expectedIds) throws Exception {
    Response response = request(url).post(Entity.json(request));
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(expectedIds.size(), data.size());
    assertEquals(expectedIds, set(data.iterator(), this::getIdFromModel));
  }

  void fetchAndAssertNone(String url, ValidatingRequest request) throws Exception {
    Response response = request(url).post(Entity.json(request));
    assertEquals(200, response.getStatus());
    ArrayNode data = (ArrayNode) getPayload(response);
    assertEquals(0, data.size());
  }

  /* Helpers for Cassandra */

  ObjectTypeEntity createObjectType() {
    return createObjectType("ObjectType");
  }

  ObjectTypeEntity createObjectType(String name) {
    ObjectTypeEntity entity = new ObjectTypeEntity()
            .setId(UUID.randomUUID())
            .setName(name)
            .setValidator("TrueValidator");

    return getObjectManager().saveObjectType(entity);
  }

  FactTypeEntity createFactType() {
    return createFactType(createObjectType().getId());
  }

  FactTypeEntity createFactType(UUID objectTypeID) {
    FactTypeEntity.FactObjectBindingDefinition binding = new FactTypeEntity.FactObjectBindingDefinition()
            .setSourceObjectTypeID(objectTypeID)
            .setBidirectionalBinding(true);

    FactTypeEntity entity = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("FactType")
            .setValidator("TrueValidator")
            .setRelevantObjectBindings(set(binding));

    return getFactManager().saveFactType(entity);
  }

  FactTypeEntity createMetaFactType(UUID referencedFactTypeID) {
    FactTypeEntity entity = new FactTypeEntity()
            .setId(UUID.randomUUID())
            .setName("MetaFactType")
            .setValidator("TrueValidator")
            .addRelevantFactBinding(new FactTypeEntity.MetaFactBindingDefinition().setFactTypeID(referencedFactTypeID));

    return getFactManager().saveFactType(entity);
  }

  FactRecord createFact() {
    ObjectTypeEntity objectType = createObjectType();
    return createFact(createObject(objectType.getId()), createObject(objectType.getId()));
  }

  FactRecord createFact(ObjectRecord source) {
    return createFact(source, null);
  }

  FactRecord createFact(ObjectRecord source, ObjectRecord destination) {
    return createFact(source, destination, createFactType(source.getTypeID()), f -> f);
  }

  FactRecord createFact(ObjectRecord source, FactTypeEntity factType, ObjectPreparation<FactRecord> preparation) {
    return createFact(source, createObject(source.getTypeID()), factType, preparation);
  }



  FactRecord createFact(ObjectRecord source, ObjectRecord destination, FactTypeEntity factType, ObjectPreparation<FactRecord> preparation) {
    FactRecord fact = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(factType.getId())
            .setValue("factValue")
            .setAddedByID(UUID.fromString("00000000-0000-0000-0000-000000000009"))
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOriginID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setSourceObject(source)
            .setDestinationObject(destination)
            .setBidirectionalBinding(true);

    return getObjectFactDao().storeFact(preparation.prepare(fact));
  }

  FactRecord createMetaFact(FactRecord referencedFact, FactTypeEntity metaFactType, ObjectPreparation<FactRecord> preparation) {
    FactRecord meta = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(metaFactType.getId())
            .setValue("factValue")
            .setOrganizationID(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .setOriginID(UUID.randomUUID())
            .setAccessMode(FactRecord.AccessMode.RoleBased)
            .setTimestamp(123456789)
            .setLastSeenTimestamp(987654321)
            .setInReferenceToID(referencedFact.getId());

    return getObjectFactDao().storeFact(preparation.prepare(meta));
  }

  FactRecord retractFact(FactRecord factToRetract) {
    FactTypeRequestResolver factTypeRequestResolver = new FactTypeRequestResolver(factManager);
    FactTypeEntity retractFactType = factTypeRequestResolver.resolveRetractionFactType();
    FactRecord retractionFact = new FactRecord()
            .setId(UUID.randomUUID())
            .setTypeID(retractFactType.getId())
            .setInReferenceToID(factToRetract.getId())
            .setAccessMode(FactRecord.AccessMode.Public)
            .setTimestamp(System.currentTimeMillis())
            .setLastSeenTimestamp(System.currentTimeMillis());

    getObjectFactDao().storeFact(retractionFact);
    return getObjectFactDao().retractFact(factToRetract);
  }

  ObjectRecord createObject() {
    return createObject(createObjectType().getId());
  }

  ObjectRecord createObject(UUID objectTypeID) {
    return createObject(objectTypeID, o -> o);
  }

  ObjectRecord createObject(UUID objectTypeID, ObjectPreparation<ObjectRecord> preparation) {
    ObjectRecord object = new ObjectRecord()
            .setId(UUID.randomUUID())
            .setTypeID(objectTypeID)
            .setValue("objectValue" + UUID.randomUUID());

    return getObjectFactDao().storeObject(preparation.prepare(object));
  }

  interface ObjectPreparation<T> {
    T prepare(T e);
  }

  private static class ServiceModuleIT extends AbstractModule {
    @Override
    protected void configure() {
      install(new TiServiceModule());
      install(new TiServerModule());
      // Configuration
      String smbServerUrl = "tcp://" + activemq.getExposedHost() + ":" + activemq.getExposedHostPort(61616);
      bind(String.class).annotatedWith(Names.named("act.access.controller.properties.configuration.file")).toInstance(ACL_FILE);
      bind(String.class).annotatedWith(Names.named("act.access.controller.properties.reload.interval")).toInstance("60000");
      bind(String.class).annotatedWith(Names.named("act.access.controller.properties.service.account.user.id")).toInstance("3");
      bind(String.class).annotatedWith(Names.named("act.action.triggers.enabled")).toInstance("true");
      bind(String.class).annotatedWith(Names.named("trigger.administration.service.configuration.directory")).toInstance(RESOURCES_FOLDER);
      bind(String.class).annotatedWith(Names.named("act.cassandra.data.center")).toInstance("datacenter1");
      bind(String.class).annotatedWith(Names.named("act.cassandra.contact.points")).toInstance(cassandra.getExposedHost());
      bind(String.class).annotatedWith(Names.named("act.cassandra.port")).toInstance(String.valueOf(cassandra.getExposedHostPort(9042)));
      bind(String.class).annotatedWith(Names.named("act.elasticsearch.contact.points")).toInstance(elastic.getExposedHost());
      bind(String.class).annotatedWith(Names.named("act.elasticsearch.port")).toInstance(String.valueOf(elastic.getExposedHostPort(9200)));
      bind(String.class).annotatedWith(Names.named("act.seb.kafka.port")).toInstance("9092");
      bind(String.class).annotatedWith(Names.named("act.seb.kafka.contact.points")).toInstance("localhost");
      bind(String.class).annotatedWith(Names.named("act.seb.kafka.producer.topic")).toInstance("ThreatIntel.Fact");
      bind(String.class).annotatedWith(Names.named("act.seb.kafka.producer.enabled")).toInstance("false");
      bind(String.class).annotatedWith(Names.named("act.seb.kafka.esengine.consumer.topics")).toInstance("ThreatIntel.Fact");
      bind(String.class).annotatedWith(Names.named("act.seb.kafka.esengine.consumer.group")).toInstance("ACT.ESEngine");
      bind(String.class).annotatedWith(Names.named("act.seb.kafka.esengine.consumer.enabled")).toInstance("false");
      bind(String.class).annotatedWith(Names.named("act.hazelcast.instance.name")).toInstance(UUID.randomUUID().toString());
      bind(String.class).annotatedWith(Names.named("act.hazelcast.group.name")).toInstance(UUID.randomUUID().toString());
      bind(String.class).annotatedWith(Names.named("act.hazelcast.multicast.address")).toInstance("224.2.2.3");
      bind(String.class).annotatedWith(Names.named("act.hazelcast.multicast.port")).toInstance("54327");
      bind(String.class).annotatedWith(Names.named("act.hazelcast.multicast.enabled")).toInstance("false");
      bind(String.class).annotatedWith(Names.named("act.smb.queue.name")).toInstance("Service.ACT");
      bind(String.class).annotatedWith(Names.named("act.smb.server.url")).toInstance(smbServerUrl);
      bind(String.class).annotatedWith(Names.named("act.smb.server.username")).toInstance("admin");
      bind(String.class).annotatedWith(Names.named("act.smb.server.password")).toInstance("admin");
    }
  }

  private static class RestModuleIT extends AbstractModule {
    @Override
    protected void configure() {
      install(new TiRestModule());
      install(new TiClientModule());
      // Configuration
      String smbClientUrl = "tcp://" + activemq.getExposedHost() + ":" + activemq.getExposedHostPort(61616);
      bind(String.class).annotatedWith(Names.named("act.api.server.port")).toInstance(String.valueOf(API_SERVER_PORT));
      bind(String.class).annotatedWith(Names.named("act.api.cors.allowed.origins")).toInstance("http://www.example.org");
      bind(String.class).annotatedWith(Names.named("act.smb.queue.name")).toInstance("Service.ACT");
      bind(String.class).annotatedWith(Names.named("act.smb.client.url")).toInstance(smbClientUrl);
      bind(String.class).annotatedWith(Names.named("act.smb.client.username")).toInstance("admin");
      bind(String.class).annotatedWith(Names.named("act.smb.client.password")).toInstance("admin");
    }
  }
}
