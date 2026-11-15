package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.Quality
import xf.xfvrp.base.SiteType

class SynchronizationAnalyzerSpec extends Specification {

	def "Route without transshipment location has no synchronization penalty"() {
		given:
		def route = [depot(0), customer(1, 5), depot(2)] as Node[]
		def q = new Quality()

		when:
		SynchronizationAnalyzer.analyze(route, [route] as Node[][], null, q)

		then:
		q.getPenalty() == 0
	}

	def "Transshipment location with served customer is synchronized"() {
		given:
		def route = [depot(0), transshipment(1), customer(2, 5), depot(3)] as Node[]
		def q = new Quality()

		when:
		SynchronizationAnalyzer.analyze(route, [route] as Node[][], null, q)

		then:
		q.getPenalty() == 0
	}

	def "Transshipment location without served customer is penalized"() {
		given:
		def route = [depot(0), transshipment(1), customer(2, 5), transshipment(3), depot(4)] as Node[]
		def q = new Quality()

		when:
		SynchronizationAnalyzer.analyze(route, [route] as Node[][], null, q)

		then:
		q.getPenalty() == 1
	}

	def "Transshipment location with zero demand customers is penalized"() {
		given:
		def route = [depot(0), transshipment(1), customer(2, 0), depot(3)] as Node[]
		def q = new Quality()

		when:
		SynchronizationAnalyzer.analyze(route, [route] as Node[][], null, q)

		then:
		q.getPenalty() == 1
	}

	def "Transshipment location at the end of a route is penalized"() {
		given:
		def route = [depot(0), customer(1, 5), transshipment(2), depot(3)] as Node[]
		def q = new Quality()

		when:
		SynchronizationAnalyzer.analyze(route, [route] as Node[][], null, q)

		then:
		q.getPenalty() == 1
	}

	def "Multiple synchronized transshipments are not penalized"() {
		given:
		def route = [depot(0), transshipment(1), customer(2, 3), customer(3, 4), transshipment(4), customer(4, 2), depot(5)] as Node[]
		def q = new Quality()

		when:
		SynchronizationAnalyzer.analyze(route, [route] as Node[][], null, q)

		then:
		q.getPenalty() == 0
	}

	//////////////////////////////////////////

	private Node depot(int idx) {
		return new Node(
				idx, "DEP" + idx, SiteType.DEPOT,
				0, 0, -1,
				new float[0],
				new float[][]{new float[]{0, Integer.MAX_VALUE}},
				0, 0, LoadType.UNDEF, 0, "", 0
		)
	}

	private Node transshipment(int idx) {
		return new Node(
				idx, "T" + idx, SiteType.TRANSSHIPMENT,
				0, 0, -1,
				new float[0],
				new float[][]{new float[]{0, 100}},
				5, 0, LoadType.UNDEF, 0, "", 0
		)
	}

	private Node customer(int idx, float demand) {
		return new Node(
				idx, "C" + idx, SiteType.CUSTOMER,
				0, 0, -1,
				[demand] as float[],
				new float[][]{new float[]{0, 100}},
				0, 0, LoadType.DELIVERY, 0, "", 0
		)
	}
}
