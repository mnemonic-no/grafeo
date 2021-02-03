package no.mnemonic.act.platform.cli.tools.commands;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import no.mnemonic.act.platform.cli.tools.handlers.CassandraToElasticSearchReindexHandler;
import no.mnemonic.act.platform.dao.DaoModule;
import no.mnemonic.act.platform.dao.api.record.FactRecord;
import no.mnemonic.commons.container.PropertiesResolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.io.File;
import java.time.Instant;
import java.util.Properties;
import java.util.function.Consumer;

@Command(
        description = "Reindex data from Cassandra (primary storage) into ElasticSearch.",
        name = "reindex"
)
public class ReindexCommand implements Runnable {

  @Option(
          description = "Path to application.properties file containing the Cassandra and ElasticSearch configurations.",
          names = "--conf",
          required = true
  )
  private File configurationFile;

  @Option(
          description = "Timestamp to start reindexing (format of '2021-01-01T00:00:00.00Z' in UTC).",
          names = "--start",
          required = true
  )
  private Instant startTimestamp;

  @Option(
          description = "Timestamp to stop reindexing (format of '2021-01-01T00:00:00.00Z' in UTC).",
          names = "--end",
          required = true
  )
  private Instant endTimestamp;

  @Spec
  private CommandSpec spec;

  @Override
  public void run() {
    if (!configurationFile.exists()) {
      throw new ParameterException(spec.commandLine(), "Configuration file specified by '--conf' does not exist.");
    }
    if (endTimestamp.isBefore(startTimestamp)) {
      throw new ParameterException(spec.commandLine(), "'--end' option cannot be before '--start'.");
    }

    // Read the application properties from the given configuration file and set up the ComponentContainer.
    Properties applicationProperties = PropertiesResolver.loadPropertiesFile(configurationFile);
    ComponentContainerWrapper wrapper = new ComponentContainerWrapper(new ReindexCommandModule(applicationProperties));
    // Execute the command inside the ComponentContainer. The implementation is delegated to CassandraToElasticSearchReindexHandler.
    wrapper.execute(() -> wrapper.getBean(CassandraToElasticSearchReindexHandler.class).reindex(startTimestamp, endTimestamp));
  }

  private static class ReindexCommandModule extends AbstractModule {

    private final Properties applicationProperties;

    private ReindexCommandModule(Properties applicationProperties) {
      this.applicationProperties = applicationProperties;
    }

    @Override
    protected void configure() {
      // DaoModule provides the Cassandra and ElasticSearch managers.
      install(new DaoModule());
      // Bind application properties to make them available for injection.
      Names.bindProperties(binder(), applicationProperties);
      // Provide a noop implementation for the DC replication component (not needed here).
      bind(new TypeLiteral<Consumer<FactRecord>>() {}).toInstance(o -> {});
      // Handler must be a singleton in order to be handled by the ComponentContainer.
      bind(CassandraToElasticSearchReindexHandler.class).in(Scopes.SINGLETON);
    }
  }
}
