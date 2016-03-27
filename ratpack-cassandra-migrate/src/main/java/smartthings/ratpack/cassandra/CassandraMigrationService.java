package smartthings.ratpack.cassandra;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.server.Service;
import ratpack.server.StartEvent;
import smartthings.migration.MigrationParameters;
import smartthings.migration.MigrationRunner;

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
			logger.info("Auto Migrating Cassandra");
			MigrationRunner migrationRunner = new MigrationRunner();

			MigrationParameters.Builder builder = new MigrationParameters.Builder()
				.setKeyspace(config.getKeyspace())
				.setMigrationsLogFile(config.migrationFile);

			String seed = config.getSeeds().get(0);

			if (seed.contains(":")) {
				String[] tokens = seed.split(":");
				builder.setHost(tokens[0]);
				builder.setPort(Integer.parseInt(tokens[1]));
			} else {
				builder.setHost(seed);
			}

			if (config.getUser() != null) {
				builder.setUsername(config.getUser()).setPassword(config.getPassword());
			}

			if (config.getTruststore() != null) {
				builder.setTruststorePath(config.getTruststore().getPath())
					.setTruststorePassword(config.getTruststore().getPassword())
					.setKeystorePath(config.getKeystore().getPath())
					.setKeystorePassword(config.getKeystore().getPassword());
			}

			MigrationParameters parameters = builder.build();
			migrationRunner.run(parameters);
		} else {
			//We should consider deprecating this and just not using the migration module if we don't want migrations.
			logger.info("Not Migrating as the module is configured to not auto migrate.");
		}
	}
}
