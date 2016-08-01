package smartthings.ratpack.cassandra

import com.datastax.driver.core.Host
import org.cassandraunit.CassandraCQLUnit
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import org.junit.Rule
import ratpack.service.StartEvent
import spock.lang.Specification

class MigrationIntegrationSpec extends Specification {

	@Rule
	CassandraCQLUnit cassandraCQLUnit = new CassandraCQLUnit(new ClassPathCQLDataSet('empty.cql', 'test'))

	def "Runs migration with SSL options"() {
		setup:
		List<String> seeds = cassandraCQLUnit.cluster.metadata.allHosts.collect { Host host -> "${host.socketAddress.hostName}:${host.socketAddress.port}".toString() }
		CassandraModule.Config config = new CassandraModule.Config(keyspace: 'test', migrationFile: 'changelog.txt', seeds: seeds, autoMigrate: true)
		CassandraMigrationService service = new CassandraMigrationService(config)
		StartEvent mockStartEvent = Mock()

		when:
		service.onStart(mockStartEvent)

		then:
		cassandraCQLUnit.session.execute('SELECT * FROM test.a;').all().size() == 1
	}
}
