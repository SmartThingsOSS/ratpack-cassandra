package smartthings.ratpack.cassandra;

import com.google.inject.Scopes;
import ratpack.guice.ConfigurableModule;

public class CassandraMigrationModule extends ConfigurableModule<CassandraModule.Config> {

	@Override
	protected void configure() {
		bind(CassandraMigrationService.class).in(Scopes.SINGLETON);
	}
}
