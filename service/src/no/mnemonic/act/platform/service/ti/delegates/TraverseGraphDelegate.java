package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.OperationTimeoutException;
import no.mnemonic.act.platform.api.model.v1.Fact;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectIdRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectSearchRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectTypeValueRequest;
import no.mnemonic.act.platform.api.service.v1.StreamingResultSet;
import no.mnemonic.act.platform.dao.cassandra.entity.FactEntity;
import no.mnemonic.act.platform.dao.cassandra.entity.ObjectEntity;
import no.mnemonic.act.platform.dao.tinkerpop.ActGraph;
import no.mnemonic.act.platform.dao.tinkerpop.FactEdge;
import no.mnemonic.act.platform.dao.tinkerpop.ObjectVertex;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.helpers.GremlinSandboxExtension;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.common.api.ResultSet;
import org.apache.tinkerpop.gremlin.groovy.engine.GremlinExecutor;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GroovyCompilerGremlinPlugin;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;

public class TraverseGraphDelegate extends AbstractDelegate {

  private static final String SCRIPT_ENGINE = "gremlin-groovy";
  private static final long SCRIPT_EXECUTION_TIMEOUT = 120_000;

  private final ObjectSearchDelegate objectSearch;
  private final Function<ObjectEntity, Object> objectConverter;
  private final Function<FactEntity, Fact> factConverter;
  private final long scriptExecutionTimeout;
  private final TiRequestContext requestContext;
  private final TiSecurityContext securityContext;

  private final Collection<java.lang.Object> traversalResult = new ArrayList<>();

  private TraverseGraphDelegate(ObjectSearchDelegate objectSearch,
                                Function<ObjectEntity, Object> objectConverter,
                                Function<FactEntity, Fact> factConverter,
                                long scriptExecutionTimeout) {
    this.objectSearch = objectSearch;
    this.objectConverter = objectConverter;
    this.factConverter = factConverter;
    this.scriptExecutionTimeout = scriptExecutionTimeout > 0 ? scriptExecutionTimeout : SCRIPT_EXECUTION_TIMEOUT;
    // Need to store references to the contexts. They won't be available via Context.get() when the graph traversal
    // and processing is executed in a different thread.
    this.requestContext = TiRequestContext.get();
    this.securityContext = TiSecurityContext.get();
  }

  public ResultSet<?> handle(TraverseByObjectIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(TiFunctionConstants.traverseFactObjects);

    return handle(requestContext.getObjectManager().getObject(request.getId()), request.getQuery());
  }

  public ResultSet<?> handle(TraverseByObjectTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(TiFunctionConstants.traverseFactObjects);
    assertObjectTypeExists(request.getType(), "type");

    return handle(requestContext.getObjectManager().getObject(request.getType(), request.getValue()), request.getQuery());
  }

  public ResultSet<?> handle(TraverseByObjectSearchRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    securityContext.checkPermission(TiFunctionConstants.traverseFactObjects);

    // Search for Objects and use the result as starting points for the graph traversal.
    // The search will only return Objects the current user has access to, thus, there is no need to check
    // Object access here (in contrast to the traversal with a single starting Object).
    Collection<UUID> startingObjects = SetUtils.set(objectSearch.handle(request).iterator(), Object::getId);
    if (startingObjects.isEmpty()) {
      // Search returned no results, just return empty traversal result as well.
      return StreamingResultSet.builder().build();
    }

    // Execute traversal and process results.
    executeTraversal(startingObjects, request.getQuery());

    return StreamingResultSet.builder()
            .setCount(traversalResult.size())
            .setValues(traversalResult)
            .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ObjectSearchDelegate objectSearch;
    private Function<ObjectEntity, Object> objectConverter;
    private Function<FactEntity, Fact> factConverter;
    private long scriptExecutionTimeout;

    private Builder() {
    }

    public TraverseGraphDelegate build() {
      ObjectUtils.notNull(objectSearch, "Cannot instantiate TraverseGraphDelegate without 'objectSearch'.");
      ObjectUtils.notNull(objectConverter, "Cannot instantiate TraverseGraphDelegate without 'objectConverter'.");
      ObjectUtils.notNull(factConverter, "Cannot instantiate TraverseGraphDelegate without 'factConverter'.");
      return new TraverseGraphDelegate(objectSearch, objectConverter, factConverter, scriptExecutionTimeout);
    }

    public Builder setObjectSearch(ObjectSearchDelegate objectSearch) {
      this.objectSearch = objectSearch;
      return this;
    }

    public Builder setObjectConverter(Function<ObjectEntity, Object> objectConverter) {
      this.objectConverter = objectConverter;
      return this;
    }

    public Builder setFactConverter(Function<FactEntity, Fact> factConverter) {
      this.factConverter = factConverter;
      return this;
    }

    public Builder setScriptExecutionTimeout(long scriptExecutionTimeout) {
      this.scriptExecutionTimeout = scriptExecutionTimeout;
      return this;
    }
  }

  private ResultSet<?> handle(ObjectEntity startingObject, String query)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, OperationTimeoutException {
    // Verify that user has access to starting point of graph traversal.
    securityContext.checkReadPermission(startingObject);
    // Execute traversal and process results.
    executeTraversal(Collections.singleton(startingObject.getId()), query);

    return StreamingResultSet.builder()
            .setCount(traversalResult.size())
            .setValues(traversalResult)
            .build();
  }

  private void executeTraversal(Collection<UUID> startingObjects, String query)
          throws InvalidArgumentException, OperationTimeoutException {
    try (Graph graph = createGraph(); GremlinExecutor executor = createExecutor()) {
      // Create the first step of the graph traversal, i.e. starting the traversal at the Object(s) specified in the request.
      // This is injected into the script execution as variable 'g'. Every query has to start from 'g'.
      GraphTraversal<Vertex, Vertex> startingPoint = graph.traversal().V(startingObjects.toArray());
      Map<String, java.lang.Object> bindings = MapUtils.map(T("g", startingPoint));
      // Start script execution and wait until result arrived or execution is aborted.
      // Use 'withResult' callback here because the graph will then be iterated inside the 'eval' thread, thus, every
      // exception caused by the traversal will be handled inside that thread as well which will result in an ExecutionException.
      executor.eval(query, SCRIPT_ENGINE, bindings, this::produceTraversalResult).get();
    } catch (ExecutionException ex) {
      // Exceptions causing the script execution to fail are wrapped inside an ExecutionException. Need to unwrap them.
      Throwable cause = ObjectUtils.ifNull(ex.getCause(), ex);
      // A TimeoutException will be thrown when either the GremlinExecutor or the Groovy sandbox abort the script execution.
      // In both cases throw an own OperationTimeoutException in order to signal the timeout to the user.
      if (cause instanceof TimeoutException) {
        throw new OperationTimeoutException("The performed graph traversal query timed out.", "graph.traversal.timeout");
      }
      // In all other cases throw an InvalidArgumentException because the failure is most likely caused by a wrong query,
      // e.g. invalid syntax, an unsupported operation such as 'addE()', or an operation not allowed by the sandbox.
      throw new InvalidArgumentException()
              .addValidationError(cause.getMessage(), "graph.traversal.failure", "query", query);
    } catch (Exception ex) {
      // Something bad happened, abort method.
      throw new IllegalStateException("Could not perform graph traversal.", ex);
    }
  }

  private void produceTraversalResult(java.lang.Object result) {
    // The result of the graph traversal will be an iterator, thus, convert result to an iterator here.
    Iterator<?> resultIterator = IteratorUtils.asIterator(result);
    // Iterate result and convert values if necessary. This will perform the actual graph traversal.
    resultIterator.forEachRemaining(value -> {
      if (value instanceof ObjectVertex) {
        // Fetch ObjectEntity and convert to Object model before adding to result. Avoid explicitly checking access to
        // Object and rely on access control implemented in graph traversal only. Checking this would be too expensive
        // because it requires fetching Facts for each Object. In addition, accidentally returning non-accessible
        // Objects will only leak the information that the Object exists and will not give further access to any Facts.
        ObjectEntity object = ObjectVertex.class.cast(value).getObject();
        traversalResult.add(objectConverter.apply(object));
      } else if (value instanceof FactEdge) {
        // Fetch FactEntity and convert to Fact model before adding to result.
        FactEntity fact = FactEdge.class.cast(value).getFact();
        // But only add it if user has access to the Fact. Skip Fact otherwise.
        if (securityContext.hasReadPermission(fact)) {
          traversalResult.add(factConverter.apply(fact));
        }
      } else {
        // Don't know what this is, just add its string representation to result.
        // For example, it could be a query returning a list of properties.
        // This mimics the behaviour of gremlin-console and avoids returning arbitrary JSON objects.
        traversalResult.add(value.toString());
      }
    });
  }

  private Graph createGraph() {
    return ActGraph.builder()
            .setObjectManager(requestContext.getObjectManager())
            .setFactManager(requestContext.getFactManager())
            .setHasFactAccess(securityContext::hasReadPermission)
            .build();
  }

  private GremlinExecutor createExecutor() {
    Map<String, java.lang.Object> groovyCompilerConfig = MapUtils.map(
            // Protect against scripts going haywire (endless loops, etc.).
            T("timedInterrupt", scriptExecutionTimeout),
            // Statically compile scripts before execution (needed for sandbox).
            T("compilation", GroovyCompilerGremlinPlugin.Compilation.COMPILE_STATIC),
            // Execute scripts inside a sandbox (i.e. only allow whitelisted methods).
            T("extensions", GremlinSandboxExtension.class.getName())
    );

    return GremlinExecutor.build()
            .scriptEvaluationTimeout(scriptExecutionTimeout)
            .addPlugins(SCRIPT_ENGINE, MapUtils.map(T(GroovyCompilerGremlinPlugin.class.getName(), groovyCompilerConfig)))
            .create();
  }

}
