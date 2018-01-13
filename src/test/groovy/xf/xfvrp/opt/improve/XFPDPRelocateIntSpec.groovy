package xf.xfvrp.opt.improve

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.NormalizeSolutionService
import xf.xfvrp.base.ShipmentConverter
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.fleximport.InternalCustomerData
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.evaluation.EvaluationService

class XFPDPRelocateIntSpec extends Specification {

	def service = new XFPDPRelocate();
	def evalService = new EvaluationService();

	def nd = new TestNode(
	externID: "DEP",
	globalIdx: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def sol;

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Find improvement"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], nd, n[3], n[4], nd] as Node[])

		def currentQuality = evalService.check(sol, model)
		
		when:
		def newQuality = service.improve(sol, currentQuality)
		
		sol = NormalizeSolutionService.normalizeRoute(sol, model)
		def checkedQuality = evalService.check(sol, model)
		def newGiantRoute = sol.getGiantRoute()
		
		then:
		newQuality != null
		checkedQuality != null
		Math.abs(newQuality.getFitness() - checkedQuality.getFitness()) < 0.001
		newQuality.getPenalty() == 0
		Math.abs(newQuality.getCost() - 8) < 0.001
		newGiantRoute[0].getGlobalIdx() == nd.getGlobalIdx()
		newGiantRoute[1].getGlobalIdx() == nd.getGlobalIdx()
		newGiantRoute[2] == n[1]
		newGiantRoute[3] == n[2]
		newGiantRoute[4] == n[3]
		newGiantRoute[5] == n[4]
		newGiantRoute[6].getGlobalIdx() == nd.getGlobalIdx()
	}
	
	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: 1,
				ylat: 0,
				geoId: 1,
				demand: [2, 2],
				timeWindow: [[0,99]],
				shipID: "A",
				loadType: LoadType.PICKUP)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: 2,
				ylat: 0,
				geoId: 2,
				demand: [-2, -2],
				timeWindow: [[0,99]],
				shipID: "A",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 3,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "B",
				loadType: LoadType.PICKUP)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 4,
				ylat: 0,
				geoId: 1,
				demand: [-1, -1],
				timeWindow: [[0,99]],
				shipID: "B",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 5,
				externID: "5",
				xlong: 5,
				ylat: 0,
				geoId: 2,
				demand: [3, 3],
				timeWindow: [[0,99]],
				shipID: "C",
				loadType: LoadType.PICKUP)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 6,
				externID: "6",
				xlong: 6,
				ylat: 0,
				geoId: 3,
				demand: [-3, -3],
				timeWindow: [[0,99]],
				shipID: "C",
				loadType: LoadType.DELIVERY)
				.getNode()

		def customers =	[
			new InternalCustomerData(externID: "1", shipID: "A"),
			new InternalCustomerData(externID: "2", shipID: "A"),
			new InternalCustomerData(externID: "3", shipID: "B"),
			new InternalCustomerData(externID: "4", shipID: "B"),
			new InternalCustomerData(externID: "5", shipID: "C"),
			new InternalCustomerData(externID: "6", shipID: "C")
		] as List<InternalCustomerData>

		nd.setIdx(0);
		n1.setIdx(1);
		n2.setIdx(2);
		n3.setIdx(3);
		n4.setIdx(4);
		n5.setIdx(5);
		n6.setIdx(6);

		def nodes = [nd, n1, n2, n3, n4, n5, n6] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);
		new ShipmentConverter().convert(nodes, customers)

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}

}
