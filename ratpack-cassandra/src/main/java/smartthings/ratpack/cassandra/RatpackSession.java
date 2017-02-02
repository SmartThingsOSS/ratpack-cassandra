package smartthings.ratpack.cassandra;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import java.util.Map;
import ratpack.exec.Promise;

public interface RatpackSession extends Session {

	Promise<ResultSet> executePromise(String query);

	Promise<ResultSet> executePromise(String query, Object... values);

	Promise<ResultSet> executePromise(String query, Map<String, Object> values);

	Promise<ResultSet> executePromise(Statement statement);

	Promise<PreparedStatement> preparePromise(String query);

	Promise<PreparedStatement> preparePromise(RegularStatement statement);

}
