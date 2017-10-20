package smartthings.ratpack.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.EC2MultiRegionAddressTranslator;
import com.datastax.driver.core.policies.PercentileSpeculativeExecutionPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Promise;
import ratpack.service.Service;
import ratpack.service.StartEvent;
import ratpack.service.StopEvent;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class CassandraService implements Service {
	private final String[] cipherSuites = new String[]{"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"};
	private final CassandraModule.Config cassandraConfig;
	private final CustomRetryPolicy customRetryPolicy;
	protected Cluster cluster;
	protected Session session;
	private Logger logger = LoggerFactory.getLogger(CassandraService.class);

	@Inject
	public CassandraService(CassandraModule.Config cassandraConfig, CustomRetryPolicy customRetryPolicy) {
		this.cassandraConfig = cassandraConfig;
		this.customRetryPolicy = customRetryPolicy;
	}

	protected static SSLContext getSSLContext(String truststorePath, String truststorePassword, String keystorePath, String keystorePassword) throws Exception {
		FileInputStream tsf = new FileInputStream(truststorePath);
		FileInputStream ksf = new FileInputStream(keystorePath);
		SSLContext ctx = SSLContext.getInstance("SSL");

		KeyStore ts = KeyStore.getInstance("JKS");
		ts.load(tsf, truststorePassword.toCharArray());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ts);

		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(ksf, keystorePassword.toCharArray());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

		kmf.init(ks, keystorePassword.toCharArray());

		ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
		return ctx;
	}

	protected Cluster.Builder builder() {
		//Set the highest tracking to just above the socket timeout for the read.
		PerHostPercentileTracker tracker = PerHostPercentileTracker.builder(SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS + 500).build();

		DCAwareRoundRobinPolicy dcAwareRoundRobinPolicy = DCAwareRoundRobinPolicy.builder().withUsedHostsPerRemoteDc(1).build();

		Cluster.Builder builder = Cluster.builder()
				.withLoadBalancingPolicy(new TokenAwarePolicy(dcAwareRoundRobinPolicy))
				.withSpeculativeExecutionPolicy(new PercentileSpeculativeExecutionPolicy(tracker, 0.99, 3));

		if (cassandraConfig.getShareEventLoopGroup()) {
			builder.withNettyOptions(new RatpackCassandraNettyOptions());
		}

		for (String seed : cassandraConfig.seeds) {
			if (seed.contains(":")) {
				String[] tokens = seed.split(":");
				builder.addContactPoint(tokens[0]).withPort(Integer.parseInt(tokens[1]));
			} else {
				builder.addContactPoint(seed);
			}
		}

		builder.withAddressTranslator(new EC2MultiRegionAddressTranslator());

		if (cassandraConfig.truststore != null) {
			try {
				SSLContext sslContext = getSSLContext(cassandraConfig.truststore.path, cassandraConfig.truststore.password, cassandraConfig.keystore.path, cassandraConfig.keystore.password);
				builder.withSSL(JdkSSLOptions.builder().withSSLContext(sslContext).withCipherSuites(cipherSuites).build());
			} catch (Exception e) {
				logger.error("Couldn't add SSL to the cluster builder.", e);
			}
		}

		if (cassandraConfig.user != null) {
			builder.withCredentials(cassandraConfig.user, cassandraConfig.password);
		}

		builder.withProtocolVersion(ProtocolVersion.values()[cassandraConfig.protocolVersion]);

		QueryOptions queryOptions = new QueryOptions();
		queryOptions.setConsistencyLevel(ConsistencyLevel.values()[cassandraConfig.defaultConsistencyLevel]);
		queryOptions.setDefaultIdempotence(cassandraConfig.defaultIdempotence);

		if (cassandraConfig.retryQuery) {
			cluster.getConfiguration().getSocketOptions().setReadTimeoutMillis(cassandraConfig.readTimeoutMillis);
			builder.withRetryPolicy(customRetryPolicy);
		}

		return builder;
	}

	protected Session connect(Cluster cluster) {
		if (cassandraConfig.keyspace != null) {
			return cluster.connect(cassandraConfig.keyspace);
		} else {
			return cluster.connect();
		}
	}

	public Promise<ResultSet> execute(Statement statement) {
		return Promise.async(upstream -> {
			ResultSetFuture resultSetFuture = session.executeAsync(statement);
			upstream.accept(resultSetFuture);
		});
	}

	@Override
	public void onStart(StartEvent event) throws Exception {
		session = connect(builder().build());
	}

	@Override
	public void onStop(StopEvent event) throws Exception {
		session.closeAsync();
	}

	public Session getSession() {
		return this.session;
	}

	public Cluster getCluster() {
		return cluster;
	}

}
