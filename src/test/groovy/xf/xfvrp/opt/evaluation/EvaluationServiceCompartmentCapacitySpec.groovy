package xf.xfvrp.opt.evaluation

import spock.lang.Ignore
import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.fleximport.CompartmentCapacity
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

//TODO(Holger & Lars): Discuss this! Tests are not working!
class EvaluationServiceCompartmentCapacitySpec extends Specification {

	def service = new EvaluationService()

	def nd = new TestNode(
	externID: "DEP",
	siteType: SiteType.DEPOT,
	demand: [0, 0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def sol

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Valid - 3 compartments and load types are all equal"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
				        new CompartmentCapacity(6),
						new CompartmentCapacity(6),
						new CompartmentCapacity(6)
				]).getVehicle()
		def model = initScen(v, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() == 0
	}

	def "Valid - 3 compartments, for mixed routes only mixed capacity needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(6),
						new CompartmentCapacity(1,2,6),
						new CompartmentCapacity(2,1,6)
				]).getVehicle()
		def model = initScen(v, null)
		// Pickup corrections
		model.nodes[3].demand[1] = 0
		model.nodes[3].demand[2] = 1
		// Delivery corrections
		model.nodes[2].demand[1] = 1
		model.nodes[2].demand[2] = 1
		model.nodes[4].demand[1] = 1
		model.nodes[4].demand[2] = 0
		def n = model.getNodes()

		sol = new Solution()
		// Pickup 4 at depot (truck=0,0,4)
		// Pickup 1 at 1.node (truck=0,1,5)
		// Unload 2 at 2.node (truck=2,1,3)
		// Pickup 3 at 3.node (truck=2,4,6)
		// Unload 2 at 4.node (truck=4,4,4)
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() == 0
	}

	@Ignore
	def "Invalid - 3 compartments and mixed is too less"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(1, 1, 6),
						new CompartmentCapacity(1,1,5),
						new CompartmentCapacity(1,1,6)
				]).getVehicle()
		def model = initScen(v, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() > 0
	}

	@Ignore
	def "Valid - 3 compartments, only deliveries, only capacity for delivery needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(1,8,1),
						new CompartmentCapacity(1,8,1),
						new CompartmentCapacity(1,8,1)
				]).getVehicle()
		def model = initScen(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() == 0
	}

	@Ignore
	def "Invalid - 3 compartments, only deliveries, only capacity for delivery needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(1,8,1),
						new CompartmentCapacity(1,7,1),
						new CompartmentCapacity(1,8,1)
				]).getVehicle()
		def model = initScen(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() > 0
	}

	@Ignore
	def "Valid - 3 compartments, only pickups, only capacity for pickups needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(8,1,1),
						new CompartmentCapacity(8,1,1),
						new CompartmentCapacity(8,1,1)
				]).getVehicle()
		def model = initScen(v, LoadType.PICKUP)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() == 0
	}

	@Ignore
	def "Invalid - 3 compartments, only pickups, only capacity for pickups needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(7,1,1),
						new CompartmentCapacity(8,1,1),
						new CompartmentCapacity(8,1,1)
				]).getVehicle()
		def model = initScen(v, LoadType.PICKUP)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result.getPenalty() > 0
	}


	XFVRPModel initScen(Vehicle v, LoadType l) {
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 1,
				ylat: 1,
				geoId: 1,
				demand: [1, 1, 1],
				loadType: (l == null) ? LoadType.PICKUP : l)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 0,
				ylat: 1,
				geoId: 2,
				demand: [2, 2, 2],
				loadType: (l == null) ? LoadType.DELIVERY : l)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [3,3,3],
				loadType: (l == null) ? LoadType.PICKUP : l)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 0,
				ylat: -1,
				geoId: 4,
				demand: [2,2,2],
				loadType: (l == null) ? LoadType.DELIVERY : l)
				.getNode()

		nd.setIdx(0)
		n1.setIdx(1)
		n2.setIdx(2)
		n3.setIdx(3)
		n4.setIdx(4)

		def nodes = [nd, n1, n2, n3, n4] as Node[]

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v)

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}

}
