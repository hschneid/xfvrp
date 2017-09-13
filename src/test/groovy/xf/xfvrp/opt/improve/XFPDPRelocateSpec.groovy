package xf.xfvrp.opt.improve

import java.util.stream.Collectors

import spock.lang.Specification
import xf.xfpdp.opt.XFPDPRelocate
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.evaluation.TestNode
import xf.xfvrp.opt.evaluation.TestVehicle

class XFPDPRelocateSpec extends Specification {

	def service = new XFPDPRelocate();

	def nd = new TestNode(
	externID: "DEP",
	globalIdx: 0,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def nd2 = new TestNode(
	externID: "DEP2",
	globalIdx: 4,
	xlong: 0,
	ylat: -2,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def sol;

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Search single depot - Find improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], nd, n[3], nd] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchSingleDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 3 && f[1] == 1}).count() == 1
		impList.stream().filter({f -> f[0] == 1 && f[1] == 3}).count() == 1
		impList.stream().filter({f -> f[0] == 3 && f[1] == 2}).count() == 1
		impList.stream().filter({f -> f[0] == 1 && f[1] == 4}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 3 && f[1] == 1}).collect(Collectors.toList()).get(0)[2] - 1.618) < 0.001f
		Math.abs(impList.stream().filter({f -> f[0] == 1 && f[1] == 3}).collect(Collectors.toList()).get(0)[2] - 1.618) < 0.001f
		Math.abs(impList.stream().filter({f -> f[0] == 3 && f[1] == 2}).collect(Collectors.toList()).get(0)[2] - 1.618) < 0.001f
		Math.abs(impList.stream().filter({f -> f[0] == 1 && f[1] == 4}).collect(Collectors.toList()).get(0)[2] - 1.618) < 0.001f
	}

	def "Search single depot - Find No improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], nd, n[4], nd] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchSingleDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() == 0
	}

	def "Search multi depot - Find improve"() {
		def model = initScenMultiDepot()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], nd, n[3], nd2, n[4], nd2] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchMultiDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 3 && f[1] == 1}).count() == 1
		impList.stream().filter({f -> f[0] == 1 && f[1] == 3}).count() == 1
		impList.stream().filter({f -> f[0] == 3 && f[1] == 2}).count() == 1
		impList.stream().filter({f -> f[0] == 1 && f[1] == 4}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 3 && f[1] == 1}).collect(Collectors.toList()).get(0)[2] - 1.414f) < 0.001f
		Math.abs(impList.stream().filter({f -> f[0] == 1 && f[1] == 3}).collect(Collectors.toList()).get(0)[2] - 1.414f) < 0.001f
		Math.abs(impList.stream().filter({f -> f[0] == 3 && f[1] == 2}).collect(Collectors.toList()).get(0)[2] - 1.414f) < 0.001f
		Math.abs(impList.stream().filter({f -> f[0] == 1 && f[1] == 4}).collect(Collectors.toList()).get(0)[2] - 1.414f) < 0.001f
	}

	def "Search multi depot - Find No improve"() {
		def model = initScenMultiDepot()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], nd2, n[4], nd2] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchMultiDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() == 0
	}

	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 1,
				externID: "1",
				xlong: -1,
				ylat: 0,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "A",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: -1,
				ylat: 0.5f,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "A",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "B",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: -1,
				ylat: 0,
				geoId: 1,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "B",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n5 = new TestNode(
				globalIdx: 5,
				externID: "5",
				xlong: -1,
				ylat: 0.5f,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "C",
				loadType: LoadType.DELIVERY)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 6,
				externID: "6",
				xlong: 1,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				shipID: "C",
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0);
		nd2.setIdx(1);
		n1.setIdx(2);
		n2.setIdx(3);
		n3.setIdx(4);

		def nodes = [nd, nd2, n1, n2, n3] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}

}
