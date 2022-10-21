package no.mnemonic.act.platform.cli.tools.commands;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import no.mnemonic.act.platform.cli.tools.handlers.CassandraToElasticSearchReindexHandler;
import no.mnemonic.act.platform.dao.modules.CassandraModule;
import no.mnemonic.act.platform.dao.modules.ElasticSearchModule;
import no.mnemonic.commons.container.PropertiesResolver;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.io.File;
import java.time.Instant;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

@Command(
        description = "Reindex data from Cassandra (primary storage) into ElasticSearch.",
        name = "reindex",
        sortOptions = false
)
public class ReindexCommand implements Runnable {

  @Option(
          description = "Path to application.properties file containing the Cassandra and ElasticSearch configurations.",
          names = "--conf",
          required = true
  )
  private File configurationFile;

  @ArgGroup(
          heading = "Reindex all Facts inside a specified time window.%n",
          exclusive = false
  )
  private TimeReindexOptions timeReindexOptions;

  @ArgGroup(
          heading = "Reindex Facts by a fixed set of IDs.%n",
          exclusive = false
  )
  private IdReindexOptions idReindexOptions;

  private static class TimeReindexOptions {
    @Option(
            description = "Timestamp to start reindexing (format of '2021-01-01T00:00:00.00Z' in UTC).",
            names = "--start",
            required = true,
            order = 1
    )
    private Instant startTimestamp;

    @Option(
            description = "Timestamp to stop reindexing (format of '2021-01-01T00:00:00.00Z' in UTC).",
            names = "--end",
            required = true,
            order = 2
    )
    private Instant endTimestamp;

    @Option(
            description = "By default, data will be reindex from '--start' to '--end'. " +
                    "Specify this option to reverse the order, i.e. reindex from '--end' to '--start'.",
            names = "--reverse",
            order = 3
    )
    private boolean reverse;
  }

  private static class IdReindexOptions {
    @Option(
            description = "IDs of Facts to reindex",
            names = "--id",
            required = true,
            split = ","
    )
    private Set<UUID> id;
  }

  @Spec
  private CommandSpec spec;

  @Override
  public void run() {
    if (!configurationFile.exists()) {
      throw new ParameterException(spec.commandLine(), "Configuration file specified by '--conf' does not exist.");
    }

    // Read the application properties from the given configuration file and set up the ComponentContainer.
    Properties applicationProperties = PropertiesResolver.loadPropertiesFile(configurationFile);
    // Execute the command inside the ComponentContainer. The implementation is delegated to CassandraToElasticSearchReindexHandler.
    ComponentContainerWrapper wrapper = new ComponentContainerWrapper(new ReindexCommandModule(applicationProperties));

    if (timeReindexOptions != null) {
      if (timeReindexOptions.endTimestamp.isBefore(timeReindexOptions.startTimestamp)) {
        throw new ParameterException(spec.commandLine(), "'--end' option cannot be before '--start'.");
      }

      wrapper.execute(() -> wrapper.getBean(CassandraToElasticSearchReindexHandler.class).reindex(
              timeReindexOptions.startTimestamp, timeReindexOptions.endTimestamp, timeReindexOptions.reverse));
    } else if (idReindexOptions != null) {
      wrapper.execute(() -> wrapper.getBean(CassandraToElasticSearchReindexHandler.class).reindex(idReindexOptions.id));
    } else {
      throw new ParameterException(spec.commandLine(), "Either specify options to index by time or by ID.");
    }
  }

  private static class ReindexCommandModule extends AbstractModule {

    private final Properties applicationProperties;

    private ReindexCommandModule(Properties applicationProperties) {
      this.applicationProperties = applicationProperties;
    }

    @Override
    protected void configure() {
      install(new CassandraModule());
      install(new ElasticSearchModule());

      // Bind application properties to make them available for injection.
      Names.bindProperties(binder(), applicationProperties);
      // Handler must be a singleton in order to be handled by the ComponentContainer.
      bind(CassandraToElasticSearchReindexHandler.class).in(Scopes.SINGLETON);
    }
  }
}
