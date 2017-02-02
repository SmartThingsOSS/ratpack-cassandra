package smartthings.ratpack.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.inject.Inject;
import ratpack.exec.Promise;
import ratpack.service.DependsOn;
import ratpack.service.Service;
import ratpack.service.StartEvent;
import ratpack.service.StopEvent;

/**
 * Deprecated.  Should use Guice DI and refer to an instance of {@link Session} instead.
 */
@Deprecated()
@DependsOn(Session.class)
public class CassandraService implements Service {

	private Session session;

	@Inject
	public CassandraService(Session session) {
		this.session = session;
	}

	public Promise<ResultSet> execute(Statement statement) {
		return Promise.async(upstream -> {
			ResultSetFuture resultSetFuture = session.executeAsync(statement);
			upstream.accept(resultSetFuture);
		});
	}

	@Override
	public void onStart(StartEvent event) throws  Exception {
		session.init();
	}

	@Override
	public void onStop(StopEvent event) throws Exception {
		session.closeAsync();
	}

	public Session getSession() {
		return this.session;
	}

	public Cluster getCluster() {
		return session.getCluster();
	}

}
