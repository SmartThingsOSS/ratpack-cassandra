package smartthings.ratpack.cassandra;

import com.datastax.driver.core.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Promise;
import ratpack.health.HealthCheck;
import ratpack.registry.Registry;

public class CassandraHealthCheck implements HealthCheck {
	private final Session session;
	private final String validationQuery;

	Logger logger = LoggerFactory.getLogger(CassandraHealthCheck.class);

	public CassandraHealthCheck(CassandraModule.Config cassandraConfig, Session session) {
		this.session = session;
		this.validationQuery = cassandraConfig.getValidationQuery();
	}

	@Override
	public String getName() {
		return "cassandra";
	}

	@Override
	public Promise<Result> check(Registry registry) throws Exception {
		return Promise.of(upstream -> {
			try {
				session.execute(validationQuery);
				upstream.success(Result.healthy());
			} catch (Exception ex) {
				logger.error("Cassandra connection is unhealthy", ex);
				upstream.success(Result.unhealthy("Cassandra connection is unhealthy"));
			}
		});
	}
}
