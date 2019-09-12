package no.mnemonic.act.platform.service.ti.helpers;

import no.mnemonic.commons.utilities.collections.ListUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import org.apache.tinkerpop.gremlin.groovy.jsr223.customizer.AbstractSandboxExtension;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;

import java.util.List;
import java.util.Map;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;

/**
 * This extension provides a sandbox during execution of Gremlin queries.
 * <p>
 * Only classes and methods included in 'methodWhiteList' are allowed to be invoked. Entries in the white list are
 * regular expressions matched against method descriptors. 'staticVariableTypes' configures the type of known variables.
 * For more information consult the TinkerPop documentation.
 */
public class GremlinSandboxExtension extends AbstractSandboxExtension {

  private static final List<String> methodWhiteList = ListUtils.list(
          "org\\.apache\\.tinkerpop\\.gremlin\\.process\\.traversal\\.Order.*",
          "org\\.apache\\.tinkerpop\\.gremlin\\.process\\.traversal\\.P.*",
          "org\\.apache\\.tinkerpop\\.gremlin\\.process\\.traversal\\.Pop.*",
          "org\\.apache\\.tinkerpop\\.gremlin\\.process\\.traversal\\.Scope.*",
          "org\\.apache\\.tinkerpop\\.gremlin\\.process\\.traversal\\.TextP.*",
          "org\\.apache\\.tinkerpop\\.gremlin\\.process\\.traversal\\.dsl\\.graph\\.__.*",
          "org\\.apache\\.tinkerpop\\.gremlin\\.process\\.traversal\\.dsl\\.graph\\.GraphTraversal.*",
          "org\\.apache\\.tinkerpop\\.gremlin\\.structure\\.Column.*",
          "org\\.apache\\.tinkerpop\\.gremlin\\.structure\\.Direction.*",
          "org\\.apache\\.tinkerpop\\.gremlin\\.structure\\.T.*"
  );

  private static final Map<String, Class<?>> staticVariableTypes = MapUtils.map(T("g", GraphTraversal.class));

  @Override
  public List<String> getMethodWhiteList() {
    return methodWhiteList;
  }

  @Override
  public Map<String, Class<?>> getStaticVariableTypes() {
    return staticVariableTypes;
  }

  @Override
  public boolean allowAutoTypeOfUnknown() {
    return false;
  }

}
