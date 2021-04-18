package xf.xfvrp.opt.improve.routebased.swap

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

class XFVRPSwapSearchUtilSpec extends Specification {

	def nd = new TestNode(
	externID: "DEP",
	globalIdx: 0,
	xlong: -5,
	ylat: 3.5,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def nd2 = new TestNode(
	externID: "DEP2",
	globalIdx: 1,
	xlong: 5,
	ylat: 3.5,
	siteType: SiteType.DEPOT,
	demand: [0, 0],
	timeWindow: [[0,99],[2,99]]
	).getNode()

	def sol

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()
	def impList = new PriorityQueue<>(
			(o1, o2) -> Float.compare(o2[7], o1[7])
	)

	def "Search - find all improving steps - all allowed"() {
		def model = initScen()
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[6], n[7], n[8], n[9], nd2, n[2], n[3], n[4], n[5], nd2] as Node[])
		impList.clear()

		when:
		XFVRPSwapSearchUtil.search(
				model,
				sol.getRoutes(),
				impList,
				4,
				false,
				true
		)

		then:
		impList.size() == 142
		Math.abs(impList.stream().find({f -> f.toList().subList(0,7) == [0,1,1,1,3,3,0]})[7] - 7.65) < 0.001f
	}

	def "Search - find all improving steps in 1 route - all allowed"() {
		def model = initScen()
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[7], n[3], n[6], n[2], n[9], n[8], n[4], n[5], nd] as Node[])
		impList.clear()

		when:
		XFVRPSwapSearchUtil.search(
				model,
				sol.getRoutes(),
				impList,
				3,
				false,
				true
		)

		then:
		impList.size() > 0
	}

	def "Search - no invert allowed"() {
		def model = initScen()
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[6], n[7], n[8], n[9], nd2, n[2], n[3], n[4], n[5], nd2] as Node[])
		impList.clear()

		when:
		XFVRPSwapSearchUtil.search(
				model,
				sol.getRoutes(),
				impList,
				4,
				false,
				false
		)

		then:
		impList.size() == 51
		impList.stream().filter({f -> f[6] != 0}).count() == 0
		Math.abs(impList.stream().find({f -> f.toList().subList(0,7) == [0,1,1,1,3,3,0]})[7] - 7.65) < 0.001f
	}

	def "Search - same segment length"() {
		def model = initScen()
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[6], n[7], n[8], n[9], nd2, n[2], n[3], n[4], n[5], nd2] as Node[])
		impList.clear()

		when:
		XFVRPSwapSearchUtil.search(
				model,
				sol.getRoutes(),
				impList,
				4,
				true,
				true
		)

		then:
		impList.size() == 38
		impList.stream().filter({f -> f[4] != f[5]}).count() == 0
		Math.abs(impList.stream().find({f -> f.toList().subList(0,7) == [0,1,1,1,3,3,0]})[7] - 7.65) < 0.001f
	}

	def "Search - segment length = 2"() {
		def model = initScen()
		def n = model.getNodes()

		sol = new Solution()
		sol.setGiantRoute([nd, n[6], n[7], n[8], n[9], nd2, n[2], n[3], n[4], n[5], nd2] as Node[])
		impList.clear()

		when:
		XFVRPSwapSearchUtil.search(
				model,
				sol.getRoutes(),
				impList,
				2,
				false,
				true
		)

		then:
		impList.size() == 36
		impList.stream().filter({f -> f[4] > 1}).count() == 0
		impList.stream().filter({f -> f[5] > 1}).count() == 0
		impList.stream().find({f -> f.toList().subList(0,7) == [0,1,1,1,3,3,0]}) == null
	}

	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [3, 3]).getVehicle()

		def n1 = new TestNode(
				globalIdx: 2,
				externID: "1",
				xlong: -1,
				ylat: 5,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 3,
				externID: "2",
				xlong: -1,
				ylat: 4,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 4,
				externID: "3",
				xlong: -1,
				ylat: 3,
				geoId: 4,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 5,
				externID: "4",
				xlong: -1,
				ylat: 2,
				geoId: 5,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()

		def n5 = new TestNode(
				globalIdx: 6,
				externID: "5",
				xlong: 1,
				ylat: 5,
				geoId: 6,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 7,
				externID: "6",
				xlong: 1,
				ylat: 4,
				geoId: 7,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n7 = new TestNode(
				globalIdx: 8,
				externID: "7",
				xlong: 1,
				ylat: 3,
				geoId: 8,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n8 = new TestNode(
				globalIdx: 9,
				externID: "8",
				xlong: 1,
				ylat: 2,
				geoId: 9,
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
		n5.setIdx(6);
		n6.setIdx(7);
		n7.setIdx(8);
		n8.setIdx(9);

		def nodes = [nd, nd2, n1, n2, n3, n4, n5, n6, n7, n8] as Node[];

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v);

		return new XFVRPModel(nodes, iMetric, iMetric, v, parameter)
	}
}
