package xf.xfvrp.opt.construct

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.NormalizeSolutionService
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.evaluation.EvaluationService

class XFVRPSavingsIntSpec extends Specification {

	def service = new XFVRPSavings();
	def evalService = new EvaluationService();

	def nd = new TestNode(
	externID: "DEP",
	globalIdx: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def nd2 = new TestNode(
	externID: "DEP2",
	globalIdx: 5,
	xlong: 3,
	ylat: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def sol;

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Find improvement for single depot"() {
		def model = initSDScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], nd, n[2], nd, n[3], nd, n[4], nd] as Node[])

		def currentQuality = evalService.check(sol, model)
		
		when:
		sol = service.execute(sol)
		sol = NormalizeSolutionService.normalizeRoute(sol, model)
		
		def checkedQuality = evalService.check(sol, model)
		def newGiantRoute = sol.getGiantRoute()
		
		then:
		checkedQuality != null
		checkedQuality.getPenalty() == 0
		Math.abs(checkedQuality.getCost() - 9.656) < 0.001
		newGiantRoute[0].getGlobalIdx() == nd.getGlobalIdx()
		newGiantRoute[1] == n[4]
		newGiantRoute[2] == n[2]
		newGiantRoute[3] == n[1]
		newGiantRoute[4] == n[3]
		newGiantRoute[5].getGlobalIdx() == nd.getGlobalIdx()
		newGiantRoute[6].getGlobalIdx() == nd.getGlobalIdx()
	}
	
	def "Find no improvement"() {
		def model = initSDScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[4], n[2], n[1], n[3], nd] as Node[])

		def currentQuality = evalService.check(sol, model)
		
		when:
		sol = service.execute(sol)
		sol = NormalizeSolutionService.normalizeRoute(sol, model)
		
		def checkedQuality = evalService.check(sol, model)
		def newGiantRoute = sol.getGiantRoute()
		
		then:
		checkedQuality != null
		checkedQuality.getPenalty() == 0
		Math.abs(checkedQuality.getCost() - 9.656) < 0.001
		newGiantRoute[0].getGlobalIdx() == nd.getGlobalIdx()
		newGiantRoute[1] == n[4]
		newGiantRoute[2] == n[2]
		newGiantRoute[3] == n[1]
		newGiantRoute[4] == n[3]
		newGiantRoute[5].getGlobalIdx() == nd.getGlobalIdx()
		newGiantRoute[6].getGlobalIdx() == nd.getGlobalIdx()
	}
	
	XFVRPModel initSDScen() {
		def v = new TestVehicle(name: "V1", capacity: [4, 4]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: -2,
				ylat: 2,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 2,
				ylat: 2,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: -1,
				ylat: 1,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 1,
				ylat: 1,
				geoId: 4,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 6,
				externID: "5",
				xlong: 2,
				ylat: 1,
				geoId: 5,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 7,
				externID: "6",
				xlong: 3,
				ylat: 1,
				geoId: 6,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0);
		n1.setIdx(1);
		n2.setIdx(2);
		n3.setIdx(3);
		n4.setIdx(4);
		n5.setIdx(5);
		n6.setIdx(6);

		def nodes = [nd, n1, n2, n3, n4, n5, n6] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}
}
