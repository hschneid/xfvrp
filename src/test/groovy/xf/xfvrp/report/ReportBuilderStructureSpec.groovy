package xf.xfvrp.report

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.XFVRPSolution
import xf.xfvrp.report.build.ReportBuilder

class ReportBuilderStructureSpec extends Specification {

	def service = new ReportBuilder();

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

	def sol;

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Feasability - Starts not with DEPOT"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([n[2], nd, n[3], n[4], nd] as Node[])

		def solution = new XFVRPSolution(sol, model);

		when:
		def result = service.getReport(solution)

		then:
		thrown IllegalStateException
	}

	def "Feasability - Ends not with DEPOT"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4]] as Node[])

		def solution = new XFVRPSolution(sol, model);

		when:
		def result = service.getReport(solution)

		then:
		thrown IllegalStateException
	}
	
	def "Feasability - NullPointer in Route"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], null, n[4]] as Node[])

		def solution = new XFVRPSolution(sol, model);

		when:
		def result = service.getReport(solution)

		then:
		thrown IllegalStateException
	}
	
	def "Feasability - No customer"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, nd] as Node[])

		def solution = new XFVRPSolution(sol, model);

		when:
		def result = service.getReport(solution)

		then:
		result != null
		result.getSummary().getNbrOfUsedVehicles() == 0
	}
	
	def "Feasability - No nodes"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([] as Node[])

		def solution = new XFVRPSolution(sol, model);

		when:
		def result = service.getReport(solution)

		then:
		thrown IllegalStateException
	}
	
	def "Feasability - Null route"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute(null)

		def solution = new XFVRPSolution(sol, model);

		when:
		def result = service.getReport(solution)

		then:
		thrown IllegalStateException
	}

	def "Ignore empty routes"() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()
		def model = initScen1(v, LoadType.DELIVERY)
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4], nd] as Node[])

		def sol2 = new Solution()
		sol2.setGiantRoute([nd, nd, nr, nr, n[2], n[3], n[4], nr, nd, nd] as Node[])

		def solution1 = new XFVRPSolution(sol, model);
		def solution2 = new XFVRPSolution(sol2, model);
		
		when:
		def result2 = service.getReport(solution2)

		then:
		result2 != null
		result2.getSummary().getNbrOfUsedVehicles() == 1
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
		nr.setIdx(1);
		n1.setIdx(2);
		n2.setIdx(3);
		n3.setIdx(4);

		def nodes = [nd, nr, n1, n2, n3] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}
}
