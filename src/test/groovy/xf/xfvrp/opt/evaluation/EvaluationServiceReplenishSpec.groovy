package xf.xfvrp.opt.evaluation

import spock.lang.Ignore
import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

//TODO(Holger & Lars): Discuss! Bring this back to work
class EvaluationServiceReplenishSpec extends Specification {

	def service = new EvaluationService()

	def nd = new TestNode(
			externID: "DEP",
			siteType: SiteType.DEPOT,
			demand: [0, 0, 0],
			timeWindow: [[0,99],[2,99]]
	).getNode()

	Node nr

	def sol

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Valid - Replenish all compartments by default values"() {
		def v = new TestVehicle(name: "V1", capacity: [5, 50, 500]).getVehicle()
		def model = initScen(v, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nr, n[4], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() == 0
		nr.isCompartmentReplenished() == null
	}

	def "Valid - Replenish all compartments by defined values"() {
		def v = new TestVehicle(name: "V1", capacity: [5, 50, 500]).getVehicle()
		def model = initScen(v, new boolean[]{true, true, true})
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nr, n[4], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() == 0
		nr.isCompartmentReplenished()[0]
		nr.isCompartmentReplenished()[1]
		nr.isCompartmentReplenished()[2]
	}

	def "Invalid - One compartment is not replenished and exceed capacity"() {
		def v = new TestVehicle(name: "V1", capacity: [5, 50, 500]).getVehicle()
		def model = initScen(v, new boolean[]{true, false, true})
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nr, n[4], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() > 0
		!nr.isCompartmentReplenished()[1]
	}

	def "Valid - One compartment is not replenished but it is enough capacity"() {
		def v = new TestVehicle(name: "V1", capacity: [5, 80, 500]).getVehicle()
		def model = initScen(v, new boolean[]{true, false, true})
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nr, n[4], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() == 0
	}

	def "Valid - All compartments are not replenished but it is enough capacity"() {
		def v = new TestVehicle(name: "V1", capacity: [8, 80, 800]).getVehicle()
		def model = initScen(v, new boolean[]{false, false, false})
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nr, n[4], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() == 0
		!nr.isCompartmentReplenished()[0]
		!nr.isCompartmentReplenished()[1]
		!nr.isCompartmentReplenished()[2]
	}

	def "Invalid - All compartments are not replenished and it exceeds capacity"() {
		def v = new TestVehicle(name: "V1", capacity: [7, 79, 799]).getVehicle()
		def model = initScen(v, new boolean[]{false, false, false})
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nr, n[4], n[5], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() > 0
	}

	XFVRPModel initScen(Vehicle v, boolean[] isCompartmentReplenished) {
		createReplenishmentNode(isCompartmentReplenished)

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 1,
				ylat: 1,
				geoId: 1,
				demand: [3, 30, 300],
				timeWindow: [[0,9],[2,9]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 0,
				ylat: 1,
				geoId: 2,
				demand: [2, 20, 200],
				timeWindow: [[0,9],[2,9]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [4, 40, 400],
				timeWindow: [[0,9],[2,9]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 0,
				ylat: -1,
				geoId: 4,
				demand: [1, 10, 100],
				timeWindow: [[0,9],[2,9]],
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

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}

	void createReplenishmentNode(boolean[] isCompartmentReplenished) {
		nr = new TestNode(
				externID: "REP",
				siteType: SiteType.REPLENISH,
				demand: [0, 0, 0],
				timeWindow: [[0,99],[2,99]],
				isCompartmentReplenished: isCompartmentReplenished
		).getNode()
	}

}
