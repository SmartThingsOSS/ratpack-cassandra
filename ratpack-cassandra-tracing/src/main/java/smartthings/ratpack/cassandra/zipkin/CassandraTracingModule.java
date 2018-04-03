package smartthings.ratpack.cassandra.zipkin;

import com.datastax.driver.core.Session;
import com.google.inject.Scopes;
import smartthings.ratpack.cassandra.CassandraHealthCheck;
import smartthings.ratpack.cassandra.CassandraModule;
import smartthings.ratpack.cassandra.CassandraService;
import smartthings.ratpack.cassandra.RatpackSession;

public class CassandraTracingModule extends CassandraModule {

	@Override
	protected void configure() {
		bind(TracedSession.class).in(Scopes.SINGLETON);
		bind(Session.class).to(TracedSession.class);
		bind(RatpackSession.class).to(TracedSession.class);
		bind(CassandraService.class).in(Scopes.SINGLETON);
		bind(CassandraHealthCheck.class).in(Scopes.SINGLETON);
	}

	public static class Config extends CassandraModule.Config {
		String serviceName;

		public String getServiceName() {
			return serviceName;
		}

		public void setServiceName(String serviceName) {
			this.serviceName = serviceName;
		}
	}

}
