package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*

class EvaluationServiceCapacitySpec extends Specification {

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

	def "Delivery - 2 capacity, all clear"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4.828) < 0.01
	}

	def "Delivery - 2 capacity, first fail"() {
		def v = new TestVehicle(name: "V1", capacity: [1, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.01
	}

	def "Delivery - 2 capacity, second fail"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 2]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.01
	}

	def "Pickup - 2 capacity, all clear"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.PICKUP)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4.828) < 0.01
	}

	def "Pickup - 2 capacity, first fail"() {
		def v = new TestVehicle(name: "V1", capacity: [1, 3]).getVehicle()
		def model = initScen1(v, LoadType.PICKUP)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.01
	}

	def "Pickup - 2 capacity, second fail"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 2]).getVehicle()
		def model = initScen1(v, LoadType.PICKUP)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.01
	}

	def "Pickup/Delivery - all clear"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen2(v)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[1], n[3], n[2], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4.828) < 0.01
	}

	def "Pickup/Delivery - wrong route order"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen2(v)
		def n = model.getNodes()

		
		// Pickup 3 at    depot (truck 3)
		// Unload 1 at 1. node  (truck=2)
		// Pickup 3 at 2. node  (truck=5) <--- Error
		// Unload 2 at 3. node  (truck=3)
		def sol = Helper.set(model, [nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.01
	}

	def "Replenish - homogenus - all clear"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen3(v)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[2], n[4], nr, n[6], n[3], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 6.828) < 0.01
	}

	def "Replenish - homogenus - pickup fail"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen3(v)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[2], n[4], nr, n[3], n[5], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 7.414) < 0.01
	}

	def "Replenish - homogenus - delivery fail"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen3(v)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[2], n[4], n[6], nr, n[3], nd] as Node[])

		when:
		def result = service.check(sol)
		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 7.414) < 0.01
	}
	
	def "Replenish - hetero - all clear"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen3(v)
		def n = model.getNodes()

		
		def sol = Helper.set(model, [nd, n[4], n[5], n[2], nr, n[6], n[3], nd] as Node[])

		when:
		def result = service.check(sol)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 9.478) < 0.01
	}


	XFVRPModel initScen1(Vehicle v, LoadType loadType) {
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 1,
				ylat: 1,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: loadType)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 0,
				ylat: 1,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: loadType)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: loadType)
				.getNode()

		nd.setIdx(0)
		n1.setIdx(1)
		n2.setIdx(2)
		n3.setIdx(3)

		def nodes = [nd, n1, n2, n3]

		return TestXFVRPModel.get(nodes, v)
	}

	XFVRPModel initScen2(Vehicle v) {
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 1,
				ylat: 1,
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
				demand: [3, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [2, 1],
				timeWindow: [[0,2],[2,6]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0)
		n1.setIdx(1)
		n2.setIdx(2)
		n3.setIdx(3)

		def nodes = [nd, n1, n2, n3]

		return TestXFVRPModel.get(nodes, v)
	}

	XFVRPModel initScen3(Vehicle v) {
		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 1,
				ylat: 1,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,9],[2,9]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 0,
				ylat: 1,
				geoId: 2,
				demand: [3, 1],
				timeWindow: [[0,9],[2,9]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [2, 1],
				timeWindow: [[0,9],[2,9]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 0,
				ylat: -1,
				geoId: 4,
				demand: [2, 1],
				timeWindow: [[0,9],[2,9]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 5,
				externID: "5",
				xlong: -1,
				ylat: 0,
				geoId: 5,
				demand: [1, 1],
				timeWindow: [[0,9],[2,9]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0)
		nr.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)
		n4.setIdx(5)
		n5.setIdx(6)

		def nodes = [nd, nr, n1, n2, n3, n4, n5]

		return TestXFVRPModel.get(nodes, v)
	}

}
