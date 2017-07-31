package xf.xfvrp.opt.improve

import java.util.stream.Collectors

import spock.lang.Specification
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

class XFVRPPathMoveSpec extends Specification {

	def service = new XFVRPPathMove();

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

	def "Search single depot - No invert - Find improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nd, n[4], n[5], nd] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchSingleDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 4 && f[1] == 2 && f[2] == 1 && f[3] == 0}).count() == 1
		impList.stream().filter({f -> f[0] == 1 && f[1] == 5 && f[2] == 1 && f[3] == 0}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 4 && f[1] == 2 && f[2] == 1 && f[3] == 0}).collect(Collectors.toList()).get(0)[4] - 4) < 0.001f
		Math.abs(impList.stream().filter({f -> f[0] == 1 && f[1] == 5 && f[2] == 1 && f[3] == 0}).collect(Collectors.toList()).get(0)[4] - 4.828) < 0.001f
	}

	def "Search single depot - Find No improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[4], n[2], n[3], n[5], nd] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchSingleDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() == 0
	}
	
	def "Search multi depot - No invert - Find improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd2, n[2], n[3], nd, n[4], n[5], nd] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchMultiDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 4 && f[1] == 1 && f[2] == 1 && f[3] == 1}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 4 && f[1] == 1 && f[2] == 1 && f[3] == 1}).collect(Collectors.toList()).get(0)[4] - 4.563) < 0.001f
	}


	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()

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
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0);
		nd2.setIdx(1);
		n1.setIdx(2);
		n2.setIdx(3);
		n3.setIdx(4);
		n4.setIdx(5);

		def nodes = [nd, nd2, n1, n2, n3, n4] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}
}
