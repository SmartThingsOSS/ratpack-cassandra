package smartthings.ratpack.cassandra

import com.datastax.driver.core.exceptions.NoHostAvailableException
import com.datastax.driver.core.policies.RetryPolicy
import org.cassandraunit.CassandraCQLUnit
import org.cassandraunit.dataset.CQLDataSet
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import ratpack.registry.Registry
import ratpack.service.StartEvent
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class CassandraServiceSpec extends Specification {

	@Shared
	CassandraCQLUnit cassandraCQLUnit

	@AutoCleanup
	ExecHarness harness = ExecHarness.harness()

	private static final String TEST_KEYSPACE = "ratpack_cassandra_test"

	//This is set in test-cassandra.yaml
	private static final String TEST_SEED = "localhost:9142"

	def setupSpec() {
		CQLDataSet dataSet = new ClassPathCQLDataSet('test-baseline.cql', TEST_KEYSPACE)
		cassandraCQLUnit = new CassandraCQLUnit(dataSet, 'test-cassandra.yaml')
		cassandraCQLUnit.before()
	}

	def cleanupSpec() {
		cassandraCQLUnit.after()
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

	def "Establish a connection and get a session object for FixedRetryPolicy"() {
		given:
		CassandraModule.Config cassConfig = new CassandraModule.Config()
		cassConfig.setKeyspace(TEST_KEYSPACE)
		cassConfig.setSeeds([TEST_SEED])
		RetryPolicy fixedRetryPolicy = new FixedRetryPolicy();
		CassandraService service

		when:
		harness.run {
			service = new CassandraService(cassConfig, fixedRetryPolicy)
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

	def "Error on bad connection for FixedRetryPolicy"() {
		given:
		CassandraModule.Config cassConfig = new CassandraModule.Config()
		cassConfig.setKeyspace(TEST_KEYSPACE)
		cassConfig.setSeeds(["localhost:1111"])
		RetryPolicy fixedRetryPolicy = new FixedRetryPolicy();
		CassandraService service

		when:
		harness.run {
			service = new CassandraService(cassConfig, fixedRetryPolicy)
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
