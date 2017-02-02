package smartthings.ratpack.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;

/**
 * Default implementation of {@link AbstractSession} that is registered as the default
 * implementation of {@link Session} with Guice
 */
public final class DefaultSession extends AbstractSession {

	@Inject
	public DefaultSession(Cluster cluster, CassandraModule.Config config) {
		super(cluster, config.keyspace);
	}

}
