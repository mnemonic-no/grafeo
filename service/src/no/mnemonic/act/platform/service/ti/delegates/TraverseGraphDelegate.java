package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectIdRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectSearchRequest;
import no.mnemonic.act.platform.api.request.v1.TraverseByObjectTypeValueRequest;
import no.mnemonic.act.platform.api.service.v1.TraversalResult;
import no.mnemonic.act.platform.dao.tinkerpop.ActGraph;
import no.mnemonic.act.platform.dao.tinkerpop.FactEdge;
import no.mnemonic.act.platform.dao.tinkerpop.ObjectVertex;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.cassandra.ObjectEntity;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.act.platform.service.ti.TiSecurityContext;
import no.mnemonic.act.platform.service.ti.helpers.GremlinSandboxExtension;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.apache.tinkerpop.gremlin.groovy.engine.GremlinExecutor;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GroovyCompilerGremlinPlugin;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;

public class TraverseGraphDelegate extends AbstractDelegate {

  private static final String SCRIPT_ENGINE = "gremlin-groovy";
  private static final long SCRIPT_EXECUTION_TIMEOUT = 15_000;

  private final ObjectSearchDelegate objectSearch;
  private final TraversalResult.Builder traversalResultBuilder;
  private final TiRequestContext requestContext;
  private final TiSecurityContext securityContext;

  private TraverseGraphDelegate(ObjectSearchDelegate objectSearch) {
    this.objectSearch = objectSearch;
    this.traversalResultBuilder = TraversalResult.builder();
    // Need to store references to the contexts. They won't be available via Context.get() when the graph traversal
    // and processing is executed in a different thread.
    this.requestContext = TiRequestContext.get();
    this.securityContext = TiSecurityContext.get();
  }

  public TraversalResult handle(TraverseByObjectIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.traverseFactObjects);

    return handle(requestContext.getObjectManager().getObject(request.getId()), request.getQuery());
  }

  public TraversalResult handle(TraverseByObjectTypeValueRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.traverseFactObjects);
    assertObjectTypeExists(request.getType(), "type");

    return handle(requestContext.getObjectManager().getObject(request.getType(), request.getValue()), request.getQuery());
  }

  public TraversalResult handle(TraverseByObjectSearchRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    securityContext.checkPermission(TiFunctionConstants.traverseFactObjects);

    // Search for Objects and use the result as starting points for the graph traversal.
    // The search will only return Objects the current user has access to, thus, there is no need to check
    // Object access here (in contrast to the traversal with a single starting Object).
    Collection<UUID> startingObjects = SetUtils.set(objectSearch.handle(request).getValues(), Object::getId);
    if (startingObjects.isEmpty()) {
      // Search returned no results, just return empty TraversalResult as well.
      return traversalResultBuilder.build();
    }

    // Execute traversal and process results (added to 'traversalResultBuilder).
    executeTraversal(startingObjects, request.getQuery());

    return traversalResultBuilder.build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private ObjectSearchDelegate objectSearch;

    private Builder() {
    }

    public TraverseGraphDelegate build() {
      ObjectUtils.notNull(objectSearch, "Cannot instantiate TraverseGraphDelegate without 'objectSearch'.");
      return new TraverseGraphDelegate(objectSearch);
    }

    public Builder setObjectSearch(ObjectSearchDelegate objectSearch) {
      this.objectSearch = objectSearch;
      return this;
    }
  }

  private TraversalResult handle(ObjectEntity startingObject, String query) throws AccessDeniedException {
    // Verify that user has access to starting point of graph traversal.
    checkObjectAccess(startingObject);
    // Execute traversal and process results (added to 'traversalResultBuilder).
    executeTraversal(Collections.singleton(startingObject.getId()), query);

    return traversalResultBuilder.build();
  }

  private void executeTraversal(Collection<UUID> startingObjects, String query) {
    try (Graph graph = createGraph(); GremlinExecutor executor = createExecutor()) {
      // Create the first step of the graph traversal, i.e. starting the traversal at the Object(s) specified in the request.
      // This is injected into the script execution as variable 'g'. Every query has to start from 'g'.
      GraphTraversal<Vertex, Vertex> startingPoint = graph.traversal().V(startingObjects.toArray());
      Map<String, java.lang.Object> bindings = MapUtils.map(T("g", startingPoint));
      // Start script execution and wait until result arrived or execution is aborted.
      // Use 'withResult' callback here because the graph will then be iterated inside the 'eval' thread, thus, every
      // exception caused by the traversal will be handled by the 'afterFailure()' callback.
      executor.eval(query, SCRIPT_ENGINE, bindings, this::produceTraversalResult).get();
    } catch (ExecutionException ignored) {
      // Exceptions causing the script execution to fail (this will cause ExecutionException) are handled by the
      // afterTimeout() and afterFailure() callbacks on 'executor'. These exception are transported back to the REST
      // layer via the TraversalResult.
    } catch (Exception ex) {
      // Something bad happened, abort method.
      throw new RuntimeException(ex);
    }
  }

  private void produceTraversalResult(java.lang.Object result) {
    // The result of the graph traversal will be an iterator, thus, convert result to an iterator here.
    Iterator<?> resultIterator = IteratorUtils.asIterator(result);
    // Iterate result and convert values if necessary. This will perform the actual graph traversal.
    resultIterator.forEachRemaining(value -> {
      if (value instanceof ObjectVertex) {
        // Fetch ObjectEntity and convert to Object model before adding to result.
        ObjectEntity object = ObjectVertex.class.cast(value).getObject();
        traversalResultBuilder.addValue(requestContext.getObjectConverter().apply(object));
      } else if (value instanceof FactEdge) {
        // Fetch FactEntity and convert to Fact model before adding to result.
        FactEntity fact = FactEdge.class.cast(value).getFact();
        traversalResultBuilder.addValue(requestContext.getFactConverter().apply(fact));
      } else {
        // Don't know what this is, just add its string representation to result.
        // For example, it could be a query returning a list of properties.
        // This mimics the behaviour of gremlin-console and avoids returning arbitrary JSON objects.
        traversalResultBuilder.addValue(value.toString());
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
            T("timedInterrupt", SCRIPT_EXECUTION_TIMEOUT),
            // Statically compile scripts before execution (needed for sandbox).
            T("compilation", GroovyCompilerGremlinPlugin.Compilation.COMPILE_STATIC),
            // Execute scripts inside a sandbox (i.e. only allow whitelisted methods).
            T("extensions", GremlinSandboxExtension.class.getName())
    );

    return GremlinExecutor.build()
            .scriptEvaluationTimeout(SCRIPT_EXECUTION_TIMEOUT)
            .addPlugins(SCRIPT_ENGINE, MapUtils.map(T(GroovyCompilerGremlinPlugin.class.getName(), groovyCompilerConfig)))
            .afterTimeout(b -> traversalResultBuilder.addMessage("The performed graph traversal query timed out.", "graph.traversal.timeout"))
            .afterFailure((b, t) -> traversalResultBuilder.addMessage(t.getMessage(), "graph.traversal.error"))
            .create();
  }

}
