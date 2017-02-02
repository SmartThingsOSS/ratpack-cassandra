package smartthings.ratpack.cassandra

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Host
import com.datastax.driver.core.Session
import org.cassandraunit.CassandraCQLUnit
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import org.junit.Rule
import ratpack.registry.internal.SimpleMutableRegistry
import ratpack.service.StartEvent
import spock.lang.Specification

class MigrationIntegrationSpec extends Specification {

	@Rule
	CassandraCQLUnit cassandraCQLUnit = new CassandraCQLUnit(new ClassPathCQLDataSet('empty.cql', 'test'))

	def "Runs migration with SSL options"() {
		given:
		List<String> seeds = cassandraCQLUnit.cluster.metadata.allHosts.collect { Host host -> "${host.socketAddress.hostName}:${host.socketAddress.port}".toString() }
		CassandraModule.Config config = new CassandraModule.Config(keyspace: 'test', migrationFile: 'changelog.txt', seeds: seeds, autoMigrate: true)
		CassandraMigrationService service = new CassandraMigrationService(config)
		StartEvent mockStartEvent = Mock()
		CassandraModule cassandraModule = new CassandraModule()
		Cluster cluster = cassandraModule.cluster(config)

		and:
		CassandraService cassandraService = new CassandraService(new DefaultSession(cluster, config))

		and:
		def registry = new SimpleMutableRegistry()
		registry.add(Session, cassandraService.session)
		registry.add(CassandraService, cassandraService)

		and:
		1 * mockStartEvent.getRegistry() >> registry


		when:
		cassandraService.onStart(Mock(StartEvent))

		and:
		service.onStart(mockStartEvent)

		then:
		cassandraCQLUnit.session.execute('SELECT * FROM test.a;').all().size() == 1
	}
}
