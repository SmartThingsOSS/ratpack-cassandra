package smartthings.ratpack.cassandra

import com.datastax.driver.core.Host
import org.testcontainers.containers.CassandraContainer
import ratpack.registry.internal.SimpleMutableRegistry
import ratpack.service.StartEvent
import spock.lang.Shared
import spock.lang.Specification
import org.testcontainers.spock.Testcontainers


@Testcontainers
class MigrationIntegrationSpec extends Specification {

	@Shared
	CassandraContainer cassandraCluster = new CassandraContainer();

	@Shared
	private String TEST_SEED = "localhost:9042"

	private static final String TEST_KEYSPACE = "migration_test"

	def setupSpec() {
		TEST_SEED = "${cassandraCluster.getContainerIpAddress()}:${cassandraCluster.getMappedPort(9042)}"
		cassandraCluster.getCluster().connect().execute("CREATE KEYSPACE IF NOT EXISTS $TEST_KEYSPACE WITH replication = \n" +
				"{'class':'SimpleStrategy','replication_factor':'1'};")

	}

	def "Runs migration with SSL options"() {
		given:
		List<String> seeds = [TEST_SEED]
		CassandraModule.Config config = new CassandraModule.Config(keyspace: TEST_KEYSPACE, migrationFile: 'changelog.txt', seeds: seeds, autoMigrate: true)
		CassandraMigrationService service = new CassandraMigrationService(config)
		StartEvent mockStartEvent = Mock()

		and:
		CassandraService cassandraService = new CassandraService(config)

		and:
		def registry = new SimpleMutableRegistry()
		registry.add(CassandraService, cassandraService)

		and:
		1 * mockStartEvent.getRegistry() >> registry


		when:
		cassandraService.onStart(Mock(StartEvent))

		and:
		service.onStart(mockStartEvent)

		then:
		cassandraCluster.getCluster().connect().execute("SELECT * FROM ${TEST_KEYSPACE}.a;").all().size() == 1
	}
}
