package smartthings.ratpack.cassandra;

import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.service.DependsOn;
import ratpack.service.Service;
import ratpack.service.StartEvent;
import smartthings.migration.MigrationParameters;
import smartthings.migration.MigrationRunner;

@DependsOn(CassandraService.class)
public class CassandraMigrationService implements Service {

	private Logger logger = LoggerFactory.getLogger(CassandraMigrationService.class);

	private CassandraModule.Config config;

	@Inject
	public CassandraMigrationService(CassandraModule.Config config) {
		this.config = config;
	}

	@Override
	public void onStart(StartEvent event) throws Exception {
		if (config.autoMigrate) {
			Session session = event.getRegistry().get(CassandraService.class).getSession();
			logger.info("Auto Migrating Cassandra");
			MigrationRunner migrationRunner = new MigrationRunner();

			MigrationParameters.Builder builder = new MigrationParameters.Builder()
				.setKeyspace(config.getKeyspace())
				.setMigrationsLogFile(config.migrationFile)
				.setSession(session);

			MigrationParameters parameters = builder.build();
			migrationRunner.run(parameters);
		} else {
			//We should consider deprecating this and just not using the migration module if we don't want migrations.
			logger.info("Not Migrating as the module is configured to not auto migrate.");
		}
	}
}
