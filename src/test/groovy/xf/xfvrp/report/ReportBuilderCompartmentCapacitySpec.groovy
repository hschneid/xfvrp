package xf.xfvrp.report

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.fleximport.CompartmentCapacity
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.XFVRPSolution
import xf.xfvrp.report.build.ReportBuilder

class ReportBuilderCompartmentCapacitySpec extends Specification {

	def service = new ReportBuilder()

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
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4] ,nd] as Node[])

		when:
		def result = service.getReport(new XFVRPSolution(sol, model))

		then:
		result.getSummary().getOverloads()[0] == 0
		result.getSummary().getOverloads()[1] == 0
		result.getSummary().getOverloads()[2] == 0
		result.getRoutes().size() == 1
		result.getRoutes().get(0).getSummary().getDeliveries()[0] == 4
		result.getRoutes().get(0).getSummary().getDeliveries()[1] == 4
		result.getRoutes().get(0).getSummary().getDeliveries()[2] == 4
		result.getRoutes().get(0).getSummary().getPickups()[0] == 4
		result.getRoutes().get(0).getSummary().getPickups()[1] == 4
		result.getRoutes().get(0).getSummary().getPickups()[2] == 4
	}

	def "Valid - 3 compartments, for mixed routes only mixed capacity needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(6),
						new CompartmentCapacity(1, 2, 6),
						new CompartmentCapacity(1, 2, 6)
				]).getVehicle()
		def model = initScen(v, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.getReport(new XFVRPSolution(sol, model))

		then:
		result.getSummary().getOverloads()[0] == 0
		result.getSummary().getOverloads()[1] == 0
		result.getSummary().getOverloads()[2] == 0
		result.getRoutes().size() == 1
		result.getRoutes().get(0).getSummary().getDeliveries()[0] == 4
		result.getRoutes().get(0).getSummary().getDeliveries()[1] == 4
		result.getRoutes().get(0).getSummary().getDeliveries()[2] == 4
		result.getRoutes().get(0).getSummary().getPickups()[0] == 4
		result.getRoutes().get(0).getSummary().getPickups()[1] == 4
		result.getRoutes().get(0).getSummary().getPickups()[2] == 4
	}


	def "Invalid - 3 compartments and mixed is too less"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(1, 1, 5),
						new CompartmentCapacity(1,1,6),
						new CompartmentCapacity(1,1,5)
				]).getVehicle()
		def model = initScen(v, null)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.getReport(new XFVRPSolution(sol, model))

		then:
		result.getSummary().getOverloads()[0] == 3
		result.getSummary().getOverloads()[1] == 0
		result.getSummary().getOverloads()[2] == 3
		result.getSummary().getOverload(v) == 3
		result.getSummary().getOverloads(v)[0] == 3
		result.getSummary().getOverloads(v)[1] == 0
		result.getSummary().getOverloads(v)[2] == 3
		result.getRoutes()[0].getSummary().getOverloads()[0] == 3
		result.getRoutes()[0].getSummary().getOverloads()[1] == 0
		result.getRoutes()[0].getSummary().getOverloads()[2] == 3
	}

	def "Valid - 3 compartments, only deliveries, only capacity for delivery needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(1,8,1),
						new CompartmentCapacity(1, 8, 1),
						new CompartmentCapacity(1, 8, 1)
				]).getVehicle()
		def model = initScen(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.getReport(new XFVRPSolution(sol, model))

		then:
		result.getSummary().getOverloads()[0] == 0
		result.getSummary().getOverloads()[1] == 0
		result.getSummary().getOverloads()[2] == 0
		result.getRoutes().size() == 1
		result.getRoutes().get(0).getSummary().getDeliveries()[0] == 8
		result.getRoutes().get(0).getSummary().getDeliveries()[1] == 8
		result.getRoutes().get(0).getSummary().getDeliveries()[2] == 8
		result.getRoutes().get(0).getSummary().getPickups()[0] == 0
		result.getRoutes().get(0).getSummary().getPickups()[1] == 0
		result.getRoutes().get(0).getSummary().getPickups()[2] == 0
	}


	def "Invalid - 3 compartments, only deliveries, only capacity for delivery needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(1,8,1),
						new CompartmentCapacity(1, 7, 1),
						new CompartmentCapacity(1, 8, 1)
				]).getVehicle()
		def model = initScen(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.getReport(new XFVRPSolution(sol, model))

		then:
		result.getSummary().getOverloads()[0] == 0
		result.getSummary().getOverloads()[1] == 2
		result.getSummary().getOverloads()[2] == 0
	}

	def "Valid - 3 compartments, only pickups, only capacity for pickups needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(8,1,1),
						new CompartmentCapacity(8, 1, 1),
						new CompartmentCapacity(8, 1, 1)
				]).getVehicle()
		def model = initScen(v, LoadType.PICKUP)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.getReport(new XFVRPSolution(sol, model))

		then:
		result.getSummary().getOverloads()[0] == 0
		result.getSummary().getOverloads()[1] == 0
		result.getSummary().getOverloads()[2] == 0
		result.getRoutes().size() == 1
		result.getRoutes().get(0).getSummary().getDeliveries()[0] == 0
		result.getRoutes().get(0).getSummary().getDeliveries()[1] == 0
		result.getRoutes().get(0).getSummary().getDeliveries()[2] == 0
		result.getRoutes().get(0).getSummary().getPickups()[0] == 8
		result.getRoutes().get(0).getSummary().getPickups()[1] == 8
		result.getRoutes().get(0).getSummary().getPickups()[2] == 8
	}


	def "Invalid - 3 compartments, only pickups, only capacity for pickups needs to be considered"() {
		def v = new TestVehicle(name: "V1",
				compartmentCapacity: [
						new CompartmentCapacity(7,1,1),
						new CompartmentCapacity(7, 1, 1),
						new CompartmentCapacity(8, 1, 1)
				]).getVehicle()
		def model = initScen(v, LoadType.PICKUP)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd] as Node[])

		when:
		def result = service.getReport(new XFVRPSolution(sol, model))

		then:
		result.getSummary().getOverloads()[0] == 2
		result.getSummary().getOverloads()[1] == 2
		result.getSummary().getOverloads()[2] == 0
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
