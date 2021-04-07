package xf.xfvrp.opt.construct.insert

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

class XFPDPFirstBestInsertSpecInt extends Specification {

	def service = new XFPDPFirstBestInsert();

	def nd = new TestNode(
	externID: "DEP",
	globalIdx: 0,
	xlong: -1,
	ylat: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def nd2 = new TestNode(
	externID: "DEP2",
	globalIdx: 1,
	xlong: 1,
	ylat: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def sol;

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Opt"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		
		def solution = new Solution()

		when:
		def newSol = service.execute(solution)
		def route = newSol.getGiantRoute()

		then:
		route[0].globalIdx == nd.globalIdx
		route[1] == n[2]
		route[2] == n[3]
		route[3] == n[4]
		route[4] == n[5]
		route[5].globalIdx == nd.globalIdx
		route[6].globalIdx == nd2.globalIdx
		route[7] == n[6]
		route[8] == n[7]
		route[9] == n[8]
		route[10] == n[9]
		route[11].globalIdx == nd2.globalIdx
		route[12].globalIdx == nd2.globalIdx
	}
	
	def "Opt with reinsert"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		parameter.setNbrOfILSLoops(3)
		
		def solution = new Solution()

		when:
		def newSol = service.execute(solution)
		def route = newSol.getGiantRoute()

		then:
		route[0].globalIdx == nd.globalIdx
		route[1] == n[2]
		route[2] == n[3]
		route[3] == n[4]
		route[4] == n[5]
		route[5].globalIdx == nd.globalIdx
		route[6].globalIdx == nd2.globalIdx
		route[7] == n[6]
		route[8] == n[7]
		route[9] == n[8]
		route[10] == n[9]
		route[11].globalIdx == nd2.globalIdx
		route[12].globalIdx == nd2.globalIdx
	}

	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 2,
				externID: "1",
				xlong: -2,
				ylat: 2,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "A",
				loadType: LoadType.PICKUP)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 3,
				externID: "2",
				xlong: -2,
				ylat: 1,
				geoId: 3,
				demand: [-1, -1],
				timeWindow: [[0,99]],
				shipID: "A",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 4,
				externID: "3",
				xlong: -2,
				ylat: -1,
				geoId: 4,
				demand: [1, 1],
				shipID: "B",
				timeWindow: [[0,99]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 5,
				externID: "4",
				xlong: -2,
				ylat: -2,
				geoId: 5,
				demand: [-1, -1],
				shipID: "B",
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 6,
				externID: "5",
				xlong: 2,
				ylat: 2,
				geoId: 6,
				demand: [1, 1],
				shipID: "C",
				timeWindow: [[0,99]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 7,
				externID: "6",
				xlong: 2,
				ylat: 1,
				geoId: 7,
				demand: [-1, -1],
				shipID: "C",
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n7 = new TestNode(
				globalIdx: 8,
				externID: "7",
				xlong: 2,
				ylat: -1,
				geoId: 8,
				demand: [1, 1],
				shipID: "D",
				timeWindow: [[0,99]],
				loadType: LoadType.PICKUP)
				.getNode()
		def n8 = new TestNode(
				globalIdx: 9,
				externID: "8",
				xlong: 2,
				ylat: -2,
				geoId: 9,
				demand: [-1, -1],
				shipID: "D",
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		nd.setIdx(0);
		nd2.setIdx(1);
		n1.setIdx(2);
		n2.setIdx(3);
		n3.setIdx(4);
		n4.setIdx(5);
		n5.setIdx(6);
		n6.setIdx(7);
		n7.setIdx(8);
		n8.setIdx(9);

		def nodes = [nd, nd2, n1, n2, n3, n4, n5, n6, n7, n8] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}
}
