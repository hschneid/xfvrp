package xf.xfvrp.opt.evaluation

import spock.lang.Specification
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.EvaluationService
import xf.xfvrp.opt.Solution

class EvaluationServiceCapacitySpec extends Specification {

	def service = new EvaluationService();

	def nd = new TestNode(
		externID: "DEP",
		siteType: SiteType.DEPOT,
		demand: [0, 0],
		timeWindow: [[0,2],[2,6]]
		).getNode()
	
	def sol;
	
	def parameter = new XFVRPParameter()
	
	def metric = new EucledianMetric()
	
	def "Delivery - 2 capacity, all clear"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()
		
		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4.828) < 0.001
	}
	
	def "Delivery - 2 capacity, first fail"() {
		def v = new TestVehicle(name: "V1", capacity: [1, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()
		
		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.001
	}

	def "Delivery - 2 capacity, second fail"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 2]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()
		
		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.001

	}

	def "Pickup - 2 capacity, all clear"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.PICKUP)
		def n = model.getNodes()
		
		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4.828) < 0.001
	}
	
	def "Pickup - 2 capacity, first fail"() {
		def v = new TestVehicle(name: "V1", capacity: [1, 3]).getVehicle()
		def model = initScen1(v, LoadType.PICKUP)
		def n = model.getNodes()
		
		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.001
	}

	def "Pickup - 2 capacity, second fail"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 2]).getVehicle()
		def model = initScen1(v, LoadType.PICKUP)
		def n = model.getNodes()
		
		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.001
	}

	def "Pickup/Delivery - 2 capacity, all clear"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen2(v)
		def n = model.getNodes()
		
		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[3], n[2], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() == 0
		Math.abs(result.getCost() - 4.828) < 0.001
	}
	
	def "Pickup/Delivery - 2 capacity, wrong route order"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen2(v)
		def n = model.getNodes()
		
		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], nd] as Node[])

		when:
		def result = service.check(sol, model)

		then:
		result != null
		result.getPenalty() > 0
		Math.abs(result.getCost() - 4.828) < 0.001
	}

	def "Pickup/Delivery - 2 capacity, second fail"() {
	}
	
	def "Pickup/Delivery - Vehicle more capacities with node demands"() {
	}

	def "Pickup/Delivery - Vehicle less capacities with node demands"() {
	}

	def "Pickup - Replenish all clear"() {
	}

	def "Pickup - Replenish with fail"() {
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
		
		nd.setIdx(0);
		n1.setIdx(1);
		n2.setIdx(2);
		n3.setIdx(3);
		
		def nodes = [nd, n1, n2, n3] as Node[];
		
		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);
		
		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
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
		
		nd.setIdx(0);
		n1.setIdx(1);
		n2.setIdx(2);
		n3.setIdx(3);
		
		def nodes = [nd, n1, n2, n3] as Node[];
		
		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);
		
		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}
}
