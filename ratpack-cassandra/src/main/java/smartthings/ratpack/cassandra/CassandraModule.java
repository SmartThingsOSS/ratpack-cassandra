package smartthings.ratpack.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.JdkSSLOptions;
import com.datastax.driver.core.PerHostPercentileTracker;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.EC2MultiRegionAddressTranslator;
import com.datastax.driver.core.policies.PercentileSpeculativeExecutionPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.guice.ConfigurableModule;

/**
 * Supports Cassandra for Ratpack.
 */
public class CassandraModule extends ConfigurableModule<CassandraModule.Config> {

	private static final Logger logger = LoggerFactory.getLogger(CassandraModule.class);

	public static class Config {

		public Config() {
		}

		JKSConfig truststore;
		JKSConfig keystore;

		String user;
		String password;

		String keyspace;
		String validationQuery = "SELECT * FROM system.schema_keyspaces";

		Boolean shareEventLoopGroup = false;

		String migrationFile = "/migrations/cql.changelog";
		Boolean autoMigrate = false;

		List<String> cipherSuites = Arrays.asList("TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA");

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

		public String getValidationQuery() {
			return validationQuery;
		}

		public void setValidationQuery(String validationQuery) {
			this.validationQuery = validationQuery;
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
		bind(DefaultSession.class).in(Scopes.SINGLETON);
		bind(Session.class).to(DefaultSession.class);
		bind(RatpackSession.class).to(DefaultSession.class);
		bind(CassandraService.class).in(Scopes.SINGLETON);
		bind(CassandraHealthCheck.class).in(Scopes.SINGLETON);
	}

	@Provides
	public Cluster cluster(final Config config) {
		return createCluster(config);
	}

	/**
	 * Extension point for overriding {@link Cluster} creation.
	 *
	 * @param config
	 * @return
	 */
	protected Cluster createCluster(final Config config) {
		//Set the highest tracking to just above the socket timeout for the read.
		PerHostPercentileTracker
				tracker = PerHostPercentileTracker.builder(SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS + 500).build();

		DCAwareRoundRobinPolicy
				dcAwareRoundRobinPolicy = DCAwareRoundRobinPolicy.builder().withUsedHostsPerRemoteDc(1).build();

		Cluster.Builder builder = Cluster.builder()
				.withLoadBalancingPolicy(new TokenAwarePolicy(dcAwareRoundRobinPolicy))
				.withSpeculativeExecutionPolicy(new PercentileSpeculativeExecutionPolicy(tracker, 0.99, 3));

		if (config.getShareEventLoopGroup()) {
			builder.withNettyOptions(new RatpackCassandraNettyOptions());
		}

		for (String seed : config.seeds) {
			if (seed.contains(":")) {
				String[] tokens = seed.split(":");
				builder.addContactPoint(tokens[0]).withPort(Integer.parseInt(tokens[1]));
			} else {
				builder.addContactPoint(seed);
			}
		}

		builder.withAddressTranslator(new EC2MultiRegionAddressTranslator());

		if (config.truststore != null) {
			try {
				SSLContext sslContext = createSSLContext(config);
				builder.withSSL(
						JdkSSLOptions.builder()
								.withSSLContext(sslContext)
								.withCipherSuites(config.cipherSuites.toArray(new String[0]))
								.build());
			} catch (Exception e) {
				logger.error("Couldn't add SSL to the cluster builder.", e);
			}
		}

		if (config.user != null) {
			builder.withCredentials(config.user, config.password);
		}

		return builder.build();
	}

	/**
	 * Extension point for overriding {@link SSLContext} creation.
	 *
	 * @param config
	 * @return
	 */
	protected SSLContext createSSLContext(final Config config) throws Exception {
		FileInputStream tsf = new FileInputStream(config.truststore.path);
		FileInputStream ksf = new FileInputStream(config.keystore.path);
		SSLContext ctx = SSLContext.getInstance("SSL");

		KeyStore ts = KeyStore.getInstance("JKS");
		ts.load(tsf, config.truststore.password.toCharArray());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ts);

		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(ksf, config.keystore.password.toCharArray());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

		kmf.init(ks, config.keystore.password.toCharArray());

		ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
		return ctx;
	}

}
