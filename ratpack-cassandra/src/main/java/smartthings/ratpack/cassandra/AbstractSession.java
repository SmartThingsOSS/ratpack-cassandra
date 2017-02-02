package smartthings.ratpack.cassandra;

import com.datastax.driver.core.CloseFuture;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.Optional;
import ratpack.exec.Promise;
import ratpack.service.Service;
import ratpack.service.StartEvent;
import ratpack.service.StopEvent;

/**
 * Abstract class that transparently provides the role of both an
 * injectable {@link Session} and fills the contract of a Ratpack {@link Service}.
 *
 * This is an abstract class to allow users to extend it for different reasons including
 * the ability to provide different types for DI resolution.
 */
public abstract class AbstractSession implements RatpackSession, Session, Service {

	private final Cluster cluster;
	private final Optional<String> keyspace;
	private Session delegate;

	AbstractSession(Cluster cluster) {
		this.cluster = cluster;
		this.keyspace = Optional.empty();
	}

	AbstractSession(Cluster cluster, String keyspace) {
		this.cluster = cluster;
		this.keyspace = Optional.of(keyspace);
	}

	@Override
	public String getLoggedKeyspace() {
		return getDelegate().getLoggedKeyspace();
	}

	@Override
	public Session init() {
		return getDelegate().init();
	}

	@Override
	public ListenableFuture<Session> initAsync() {
		return getDelegate().initAsync();
	}

	@Override
	public ResultSet execute(String s) {
		return getDelegate().execute(s);
	}

	@Override
	public ResultSet execute(String s, Object... objects) {
		return getDelegate().execute(s, objects);
	}

	@Override
	public ResultSet execute(String s, Map<String, Object> map) {
		return getDelegate().execute(s, map);
	}

	@Override
	public ResultSet execute(Statement statement) {
		return getDelegate().execute(statement);
	}

	@Override
	public ResultSetFuture executeAsync(String s) {
		return getDelegate().executeAsync(s);
	}

	@Override
	public ResultSetFuture executeAsync(String s, Object... objects) {
		return getDelegate().executeAsync(s, objects);
	}

	@Override
	public ResultSetFuture executeAsync(String s, Map<String, Object> map) {
		return getDelegate().executeAsync(s, map);
	}

	@Override
	public ResultSetFuture executeAsync(Statement statement) {
		return getDelegate().executeAsync(statement);
	}

	@Override
	public PreparedStatement prepare(String s) {
		return getDelegate().prepare(s);
	}

	@Override
	public PreparedStatement prepare(RegularStatement regularStatement) {
		return getDelegate().prepare(regularStatement);
	}

	@Override
	public ListenableFuture<PreparedStatement> prepareAsync(String s) {
		return getDelegate().prepareAsync(s);
	}

	@Override
	public
	ListenableFuture<PreparedStatement> prepareAsync(RegularStatement regularStatement) {
		return getDelegate().prepareAsync(regularStatement);
	}

	@Override
	public Promise<ResultSet> executePromise(String query) {
		return Promise.async(upstream -> {
			ResultSetFuture resultSetFuture = getDelegate().executeAsync(query);
			upstream.accept(resultSetFuture);
		});
	}

	@Override
	public Promise<ResultSet> executePromise(String query, Object... values) {
		return Promise.async(upstream -> {
			ResultSetFuture resultSetFuture = getDelegate().executeAsync(query, values);
			upstream.accept(resultSetFuture);
		});
	}

	@Override
	public Promise<ResultSet> executePromise(String query, Map<String, Object> values) {
		return Promise.async(upstream -> {
			ResultSetFuture resultSetFuture = getDelegate().executeAsync(query, values);
			upstream.accept(resultSetFuture);
		});
	}

	@Override
	public Promise<ResultSet> executePromise(Statement statement) {
		return Promise.async(upstream -> {
			ResultSetFuture resultSetFuture = getDelegate().executeAsync(statement);
			upstream.accept(resultSetFuture);
		});
	}

	@Override
	public Promise<PreparedStatement> preparePromise(String query) {
		return Promise.async(upstream -> {
			ListenableFuture<PreparedStatement> f = getDelegate().prepareAsync(query);
			upstream.accept(f);
		});
	}

	@Override
	public Promise<PreparedStatement> preparePromise(RegularStatement statement) {
		return Promise.async(upstream -> {
			ListenableFuture<PreparedStatement> f = getDelegate().prepareAsync(statement);
			upstream.accept(f);
		});
	}

	@Override
	public CloseFuture closeAsync() {
		return getDelegate().closeAsync();
	}

	@Override
	public void close() {
		getDelegate().close();
	}

	@Override
	public boolean isClosed() {
		return getDelegate().isClosed();
	}

	@Override
	public Cluster getCluster() {
		return cluster;
	}

	@Override
	public State getState() {
		return getDelegate().getState();
	}

	@Override
	public void onStart(StartEvent event) throws Exception {
		delegate = getDelegate();
	}

	@Override
	public void onStop(StopEvent event) throws Exception {
		if (getDelegate() != null && !getDelegate().isClosed()) {
			getDelegate().closeAsync();
		}
	}

	protected final Session getDelegate() {
		if (delegate == null) {
			synchronized (this) {
				if (delegate == null) {
					delegate = createDelegate();
				}
			}
		}
		return delegate;
	}

	protected Session createDelegate() {
		return (keyspace.isPresent()) ? cluster.connect(keyspace.get()) : cluster.connect();
	}
}
