package smartthings.ratpack.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.WriteType;
import com.datastax.driver.core.exceptions.DriverException;
import com.datastax.driver.core.policies.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A policy that defines a default behavior to adopt when a request fails.
 * Difference of existing DefaultRetryPolicy is that use Client side retry count variable only. and available retry count fixed 3 times.
 */
public class FixedRetryPolicy implements RetryPolicy {
	private int retryCount = 3;

	private Logger logger = LoggerFactory.getLogger(FixedRetryPolicy.class);

	@Override
	public RetryDecision onReadTimeout(Statement stmnt, ConsistencyLevel cl, int requiredResponses, int receivedResponses, boolean dataReceived, int nbRetry) {
		logger.warn("[CustomRetryPolicy]-onReadTimeout(), requiredResponses={}, receivedResponses={}, dataReceived={}, nbRetry={}", requiredResponses, receivedResponses, dataReceived, nbRetry);
		if (dataReceived) {
			return RetryDecision.ignore();
		} else if (nbRetry < retryCount) {
			return RetryDecision.retry(cl);
		} else {
			return RetryDecision.rethrow();
		}
	}

	@Override
	public RetryDecision onWriteTimeout(Statement stmnt, ConsistencyLevel cl, WriteType wt, int requiredResponses, int receivedResponses, int nbRetry) {
		logger.warn("[CustomRetryPolicy]-onWriteTimeout(), requiredResponses={}, receivedResponses={}, nbRetry={}", requiredResponses, receivedResponses, nbRetry);
		if (nbRetry < retryCount) {
			return RetryDecision.retry(cl);
		}
		return RetryDecision.rethrow();
	}

	@Override
	public RetryDecision onUnavailable(Statement stmnt, ConsistencyLevel cl, int requiredResponses, int receivedResponses, int nbRetry) {
		logger.warn("[CustomRetryPolicy]-onUnavailable(), requiredResponses={}, receivedResponses={}, nbRetry={}", requiredResponses, receivedResponses, nbRetry);
		if (nbRetry < retryCount) {
			return RetryDecision.tryNextHost(cl);
		}
		return RetryDecision.rethrow();
	}

	@Override
	public RetryDecision onRequestError(Statement statement, ConsistencyLevel cl, DriverException e, int nbRetry) {
		logger.warn("[CustomRetryPolicy]-onRequestError(), nbRetry={}", nbRetry);
		if (nbRetry < retryCount) {
			return RetryDecision.tryNextHost(cl);
		}
		return RetryDecision.rethrow();
	}

	@Override
	public void init(Cluster cluster) {
	}

	@Override
	public void close() {
	}
}
