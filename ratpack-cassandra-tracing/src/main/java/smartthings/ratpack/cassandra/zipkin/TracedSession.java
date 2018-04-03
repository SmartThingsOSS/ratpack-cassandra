package smartthings.ratpack.cassandra.zipkin;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.github.kristofa.brave.Brave;
import com.google.inject.Inject;
import smartthings.ratpack.cassandra.AbstractSession;

public final class TracedSession extends AbstractSession {

	private final Brave brave;
	private final Cluster cluster;
	private final String keyspace;
	private final CassandraTracingModule.Config config;

	@Inject
	public TracedSession(Cluster cluster, Brave brave, CassandraTracingModule.Config config) {
		super(cluster, config.getKeyspace());
		this.brave = brave;
		this.cluster = cluster;
		this.keyspace = config.getKeyspace();
		this.config = config;
	}

	protected Session createDelegate() {
		Session session = (keyspace != null && !keyspace.equals("")) ? cluster.connect(keyspace) : cluster.connect();

		return smartthings.brave.cassandra.TracedSession.create(session, brave, config.getServiceName());
	}

}
