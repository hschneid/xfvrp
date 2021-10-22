package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

class EvaluationServiceStopSpec extends Specification {

	def service = new EvaluationService()

	def nd = new TestNode(
	externID: "DEP",
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def nr = new TestNode(
	externID: "REP",
	siteType: SiteType.REPLENISH,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def sol

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Max Stop count - Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3], maxStopCount: 4).getVehicle()
		def model = initScen(v)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nr, n[4], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
	}

	def "Max Stop count - Not okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3], maxStopCount: 3).getVehicle()
		def model = initScen(v)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nr, n[4], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
	}
	
	def "Min Stop count - Okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3], maxStopCount: 2).getVehicle()
		def model = initScen(v)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[4], nr, n[3], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
	}

	def "Min Stop count - Not okay"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3], maxStopCount: 1).getVehicle()
		def model = initScen(v)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[4], nr, n[3], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
	}

	XFVRPModel initScen(Vehicle v) {
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 1,
				ylat: 0,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 0,
				ylat: 1,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 0,
				ylat: 1,
				geoId: 4,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0)
		nr.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)
		n4.setIdx(5)

		def nodes = [nd, nr, n1, n2, n3, n4] as Node[]

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v)

		return TestXFVRPModel.get(nodes, iMetric, iMetric, v, parameter)
	}

}
