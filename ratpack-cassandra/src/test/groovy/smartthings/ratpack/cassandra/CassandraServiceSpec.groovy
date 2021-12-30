package smartthings.ratpack.cassandra

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session
import com.datastax.driver.core.exceptions.NoHostAvailableException
import ratpack.registry.Registry
import ratpack.service.StartEvent
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import org.testcontainers.spock.Testcontainers
import org.testcontainers.containers.CassandraContainer
import ratpack.test.exec.ExecHarness

@Testcontainers
class CassandraServiceSpec extends Specification {

	@Shared
	CassandraContainer cassandraCluster = new CassandraContainer();

	@AutoCleanup
	ExecHarness harness = ExecHarness.harness()

	private static final String TEST_KEYSPACE = "ratpack_cassandra_test"

	@Shared
	private String TEST_SEED = "localhost:9042"

	def setupSpec() {
		TEST_SEED = "${cassandraCluster.getContainerIpAddress()}:${cassandraCluster.getMappedPort(9042)}"
		cassandraCluster.getCluster().connect().execute("CREATE KEYSPACE IF NOT EXISTS $TEST_KEYSPACE WITH replication = \n" +
				"{'class':'SimpleStrategy','replication_factor':'1'};")

	}

	def "Establish a connection and get a session object"() {
		given:
		CassandraModule.Config cassConfig = new CassandraModule.Config()
		cassConfig.setKeyspace(TEST_KEYSPACE)
		cassConfig.setSeeds([TEST_SEED])
		CassandraService service

		when:
		harness.run {
			service = new CassandraService(cassConfig)
			service.onStart(new StartEvent() {
				@Override
				Registry getRegistry() {
					return Registry.empty()
				}

				@Override
				boolean isReload() {
					return false
				}
			})
		}

		then:
		service.getSession()
	}

	def "Establish a connection and get a session object without ec2 translation"() {
		given:
		CassandraModule.Config cassConfig = new CassandraModule.Config()
		cassConfig.setKeyspace(TEST_KEYSPACE)
		cassConfig.setSeeds([TEST_SEED])
		cassConfig.setEC2TranslationEnabled(false)
		CassandraService service

		when:
		harness.run {
			service = new CassandraService(cassConfig)
			service.onStart(new StartEvent() {
				@Override
				Registry getRegistry() {
					return Registry.empty()
				}

				@Override
				boolean isReload() {
					return false
				}
			})
		}

		then:
		//All we can tell is there is a session
		service.getSession()

	}

	def "Error on bad connection"() {
		given:
		CassandraModule.Config cassConfig = new CassandraModule.Config()
		cassConfig.setKeyspace(TEST_KEYSPACE)
		cassConfig.setSeeds(["localhost:1111"])
		CassandraService service

		when:
		harness.run {
			service = new CassandraService(cassConfig)
			service.onStart(new StartEvent() {
				@Override
				Registry getRegistry() {
					return Registry.empty()
				}

				@Override
				boolean isReload() {
					return false
				}
			})
		}

		then:
		thrown(NoHostAvailableException)
	}

}
