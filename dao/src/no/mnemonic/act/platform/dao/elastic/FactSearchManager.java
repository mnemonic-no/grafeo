package no.mnemonic.act.platform.dao.elastic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.mnemonic.act.platform.dao.elastic.document.FactDocument;
import no.mnemonic.act.platform.entity.handlers.EntityHandler;
import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

/**
 * Class for indexing Facts into ElasticSearch as well as for retrieving and searching indexed Facts.
 */
@Singleton
public class FactSearchManager implements LifecycleAspect {

  private static final String INDEX_NAME = "act";
  private static final String TYPE_NAME = "fact";
  private static final String MAPPINGS_JSON = "mappings.json";

  private static final Logger LOGGER = Logging.getLogger(FactSearchManager.class);

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader FACT_DOCUMENT_READER = MAPPER.readerFor(FactDocument.class);
  private static final ObjectWriter FACT_DOCUMENT_WRITER = MAPPER.writerFor(FactDocument.class);

  @Dependency
  private final ClientFactory clientFactory;
  private final Function<UUID, EntityHandler> entityHandlerForTypeIdResolver;

  @Inject
  public FactSearchManager(ClientFactory clientFactory, Function<UUID, EntityHandler> entityHandlerForTypeIdResolver) {
    this.clientFactory = clientFactory;
    this.entityHandlerForTypeIdResolver = entityHandlerForTypeIdResolver;
  }

  @Override
  public void startComponent() {
    if (!indexExists()) {
      LOGGER.info("Index '%s' does not exist, create it.", INDEX_NAME);
      createIndex();
    }
  }

  @Override
  public void stopComponent() {
    // NOOP
  }

  /**
   * Retrieve an indexed Fact by its UUID. Returns NULL if Fact cannot be fetched from ElasticSearch.
   *
   * @param id UUID of indexed Fact
   * @return Indexed Fact or NULL if not available
   */
  public FactDocument getFact(UUID id) {
    if (id == null) return null;
    GetResponse response;

    try {
      GetRequest request = new GetRequest(INDEX_NAME, TYPE_NAME, id.toString());
      response = clientFactory.getHighLevelClient().get(request);
    } catch (IOException ex) {
      throw logAndExit(ex, String.format("Could not perform request to fetch Fact with id = %s.", id));
    }

    if (response.isExists()) {
      try {
        LOGGER.info("Successfully fetched Fact with id = %s.", id);
        FactDocument fact = decodeValues(FACT_DOCUMENT_READER.readValue(response.getSourceAsBytes()));
        // Need to set ID manually because it's not indexed as an own field.
        return fact.setId(id);
      } catch (IOException ex) {
        LOGGER.warning(ex, "Could not deserialize Fact with id = %s. Source document not stored?", id);
        return null;
      }
    } else {
      // Fact isn't indexed in ElasticSearch, log warning and return null.
      LOGGER.warning("Could not fetch Fact with id = %s. Fact not indexed?", id);
      return null;
    }
  }

  /**
   * Index a Fact into ElasticSearch.
   *
   * @param fact Fact to index
   * @return Indexed Fact
   */
  public FactDocument indexFact(FactDocument fact) {
    if (fact == null || fact.getId() == null) return null;
    IndexResponse response;

    try {
      IndexRequest request = new IndexRequest(INDEX_NAME, TYPE_NAME, fact.getId().toString())
              .source(FACT_DOCUMENT_WRITER.writeValueAsBytes(encodeValues(fact)), XContentType.JSON);
      response = clientFactory.getHighLevelClient().index(request);
    } catch (IOException ex) {
      throw logAndExit(ex, String.format("Could not perform request to index Fact with id = %s.", fact.getId()));
    }

    if (response.status() != RestStatus.OK && response.status() != RestStatus.CREATED) {
      LOGGER.warning("Could not index Fact with id = %s.", fact.getId());
    } else if (response.getResult() == DocWriteResponse.Result.CREATED) {
      LOGGER.info("Successfully indexed Fact with id = %s.", fact.getId());
    } else if (response.getResult() == DocWriteResponse.Result.UPDATED) {
      LOGGER.info("Successfully re-indexed existing Fact with id = %s.", fact.getId());
    }

    return fact;
  }

  private boolean indexExists() {
    Response response;

    try {
      // Need to use low-level client here because the Index API is not yet supported by the high-level client.
      response = clientFactory.getLowLevelClient().performRequest("HEAD", INDEX_NAME);
    } catch (IOException ex) {
      throw logAndExit(ex, "Could not perform request to verify if index exists.");
    }

    // Index exists if request returns with status code 200.
    return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
  }

  private void createIndex() {
    Response response;

    try (InputStream payload = FactSearchManager.class.getClassLoader().getResourceAsStream(MAPPINGS_JSON)) {
      // Need to use low-level client here because the Index API is not yet supported by the high-level client.
      HttpEntity body = new InputStreamEntity(payload, ContentType.APPLICATION_JSON);
      response = clientFactory.getLowLevelClient().performRequest("PUT", INDEX_NAME, Collections.emptyMap(), body);
    } catch (IOException ex) {
      throw logAndExit(ex, "Could not perform request to create index.");
    }

    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
      String msg = String.format("Could not create index '%s'.", INDEX_NAME);
      LOGGER.error(msg);
      throw new IllegalStateException(msg);
    }

    LOGGER.info("Successfully created index '%s'.", INDEX_NAME);
  }

  private FactDocument encodeValues(FactDocument fact) {
    // Clone document first in order to not change supplied instance.
    FactDocument clone = fact.clone();
    // Encode Fact value using EntityHandler to store value in encoded format.
    clone.setValue(entityHandlerForTypeIdResolver.apply(fact.getTypeID()).encode(fact.getValue()));
    // Also encode all Object values.
    SetUtils.set(clone.getObjects()).forEach(o -> o.setValue(entityHandlerForTypeIdResolver.apply(o.getTypeID()).encode(o.getValue())));

    return clone;
  }

  private FactDocument decodeValues(FactDocument fact) {
    // Decode Fact value using EntityHandler because it's stored encoded.
    fact.setValue(entityHandlerForTypeIdResolver.apply(fact.getTypeID()).decode(fact.getValue()));
    // Also decode all Object values.
    SetUtils.set(fact.getObjects()).forEach(o -> o.setValue(entityHandlerForTypeIdResolver.apply(o.getTypeID()).decode(o.getValue())));

    return fact;
  }

  private RuntimeException logAndExit(Exception ex, String msg) {
    LOGGER.error(ex, msg);
    return new RuntimeException(msg, ex);
  }

}
