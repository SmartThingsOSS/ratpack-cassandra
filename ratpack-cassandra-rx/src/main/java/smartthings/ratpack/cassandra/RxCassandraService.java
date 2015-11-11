package smartthings.ratpack.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import rx.Observable;

import java.util.Arrays;
import java.util.List;

import static ratpack.rx.RxRatpack.observe;

public class RxCassandraService {

	private final CassandraService cassandraService;

	@Inject
	public RxCassandraService(CassandraService cassandraService) {
		this.cassandraService = cassandraService;
	}

	public Observable<ResultSet> executeAndReturnResultSet(Statement statement) {
		return observe(cassandraService.execute(statement));
	}

	public Observable<Row> execute(Statement... statements) {
		List<Observable<ResultSet>> resultSets = Lists.transform(Arrays.asList(statements), this::executeAndReturnResultSet);
		return Observable.merge(resultSets).flatMap(Observable::from);
	}
}
