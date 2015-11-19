package smartthings.ratpack.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.EC2MultiRegionAddressTranslater;
import com.datastax.driver.core.policies.PercentileSpeculativeExecutionPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import ratpack.exec.Promise;
import ratpack.server.Service;
import ratpack.server.StopEvent;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class CassandraService implements Service {

	private final Cluster cluster;
	private final Session session;
	private final String[] cipherSuites = new String[]{"TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA"};

	public CassandraService(CassandraModule.Config cassandraConfig) {

		PerHostPercentileTracker tracker = PerHostPercentileTracker.builderWithHighestTrackableLatencyMillis(2000).build();

		Cluster.Builder builder = Cluster.builder()
			.withLoadBalancingPolicy(new TokenAwarePolicy(new DCAwareRoundRobinPolicy()))
			.withNettyOptions(new RatpackCassandraNettyOptions())
			.withSpeculativeExecutionPolicy(new PercentileSpeculativeExecutionPolicy(tracker, 0.99, 3));

		for (String seed : cassandraConfig.seeds) {
			if (seed.contains(":")) {
				String[] tokens = seed.split(":");
				builder.addContactPoint(tokens[0]).withPort(Integer.parseInt(tokens[1]));
			} else {
				builder.addContactPoint(seed);
			}
		}

		builder.withAddressTranslater(new EC2MultiRegionAddressTranslater());

		if (cassandraConfig.truststore != null) {
			try {
				SSLContext sslContext = getSSLContext(cassandraConfig.truststore.path, cassandraConfig.truststore.password, cassandraConfig.keystore.path, cassandraConfig.keystore.password);
				builder.withSSL(new SSLOptions(sslContext, cipherSuites));
			} catch (Exception e) {
				System.out.println(e);
			}
		}

		if (cassandraConfig.user != null) {
			builder.withCredentials(cassandraConfig.user, cassandraConfig.password);
		}

		cluster = builder.build();

		if (cassandraConfig.keyspace != null) {
			session = cluster.connect(cassandraConfig.keyspace);
		} else {
			session = cluster.connect();
		}
	}

	private static SSLContext getSSLContext(String truststorePath, String truststorePassword, String keystorePath, String keystorePassword) throws Exception {
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

	public Promise<ResultSet> execute(Statement statement) {
		return Promise.of(upstream -> {
			ResultSetFuture resultSetFuture = session.executeAsync(statement);
			upstream.accept(resultSetFuture);
		});
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
