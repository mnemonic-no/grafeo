package no.mnemonic.act.platform.cli.tools.commands;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import no.mnemonic.act.platform.cli.tools.handlers.CassandraMigrateTimeGlobalFlagHandler;
import no.mnemonic.act.platform.dao.cassandra.ClusterManager;
import no.mnemonic.act.platform.dao.cassandra.ClusterManagerProvider;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.commons.container.PropertiesResolver;
import picocli.CommandLine.*;
import picocli.CommandLine.Model.CommandSpec;

import java.io.File;
import java.time.Instant;
import java.util.Properties;

@Command(
        description = "Execute Cassandra database migrations.",
        name = "migrate"
)
public class MigrateCommand implements Runnable {

  public enum Migration {
    timeGlobalFlag
  }

  @Parameters(
          description = "Select one of the migrations to execute: ${COMPLETION-CANDIDATES}."
  )
  private Migration migration;

  @Option(
          description = "Path to application.properties file containing the Cassandra configuration.",
          names = "--conf",
          required = true
  )
  private File configurationFile;

  @Option(
          description = "Timestamp to start migrating (format of '2021-01-01T00:00:00.00Z' in UTC).",
          names = "--start",
          required = true
  )
  private Instant startTimestamp;

  @Option(
          description = "Timestamp to stop migrating (format of '2021-01-01T00:00:00.00Z' in UTC).",
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
    ComponentContainerWrapper wrapper = new ComponentContainerWrapper(new MigrateCommandModule(applicationProperties));
    // Execute the command inside the ComponentContainer. The implementation is delegated to CassandraMigrateTimeGlobalFlagHandler
    // which is currently the only available migration.
    wrapper.execute(() -> wrapper.getBean(CassandraMigrateTimeGlobalFlagHandler.class).migrate(startTimestamp, endTimestamp));
  }

  private static class MigrateCommandModule extends AbstractModule {

    private final Properties applicationProperties;

    private MigrateCommandModule(Properties applicationProperties) {
      this.applicationProperties = applicationProperties;
    }

    @Override
    protected void configure() {
      // Bind application properties to make them available for injection.
      Names.bindProperties(binder(), applicationProperties);
      // Bind the required Cassandra managers and providers.
      bind(ClusterManager.class).toProvider(ClusterManagerProvider.class).in(Scopes.SINGLETON);
      bind(FactManager.class);
      bind(ObjectManager.class);
      // Handler must be a singleton in order to be handled by the ComponentContainer.
      bind(CassandraMigrateTimeGlobalFlagHandler.class).in(Scopes.SINGLETON);
    }
  }
}
