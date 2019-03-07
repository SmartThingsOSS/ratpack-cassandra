package smartthings.ratpack.cassandra;

import com.google.inject.Scopes;
import ratpack.guice.ConfigurableModule;

import java.util.List;

/**
 * Supports Cassandra for Ratpack.
 */
public class CassandraModule extends ConfigurableModule<CassandraModule.Config> {

	public static class Config {

		public Config() {
		}

		JKSConfig truststore;
		JKSConfig keystore;

		String user;
		String password;

		String keyspace;

		Boolean shareEventLoopGroup = false;

		String migrationFile = "/migrations/cql.changelog";
		Boolean autoMigrate = false;

		List<String> seeds;

		public JKSConfig getTruststore() {
			return truststore;
		}

		public void setTruststore(JKSConfig truststore) {
			this.truststore = truststore;
		}

		public JKSConfig getKeystore() {
			return keystore;
		}

		public void setKeystore(JKSConfig keystore) {
			this.keystore = keystore;
		}

		public String getUser() {
			return user;
		}

		public void setUser(String user) {
			this.user = user;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getKeyspace() {
			return keyspace;
		}

		public void setKeyspace(String keyspace) {
			this.keyspace = keyspace;
		}

		public List<String> getSeeds() {
			return seeds;
		}

		public void setSeeds(List<String> seeds) {
			this.seeds = seeds;
		}

		public Boolean getShareEventLoopGroup() {
			return shareEventLoopGroup;
		}

		public void setShareEventLoopGroup(Boolean shareEventLoopGroup) {
			this.shareEventLoopGroup = shareEventLoopGroup;
		}

		public String getMigrationFile() {
			return migrationFile;
		}

		public void setMigrationFile(String migrationFile) {
			this.migrationFile = migrationFile;
		}

		public Boolean getAutoMigrate() {
			return autoMigrate;
		}

		public void setAutoMigrate(Boolean autoMigrate) {
			this.autoMigrate = autoMigrate;
		}

		public static class JKSConfig {

			String path;
			String password;

			public JKSConfig() {
			}

			public String getPath() {
				return path;
			}

			public void setPath(String path) {
				this.path = path;
			}

			public String getPassword() {
				return password;
			}

			public void setPassword(String password) {
				this.password = password;
			}
		}
	}

	@Override
	protected void configure() {
		bind(CassandraService.class).in(Scopes.SINGLETON);
		bind(CassandraHealthCheck.class).in(Scopes.SINGLETON);
	}

}
