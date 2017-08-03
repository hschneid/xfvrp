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

class XFVRPSwapSegmentWithInvertSpec extends Specification {

	def service = new XFVRPSwapSegmentWithInvert();

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

	def "Search - No invert - Equal"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[8], n[9], nd2, n[6], n[7], n[4], n[5], nd2] as Node[])

		when:
		def impList = service.search(sol.getGiantRoute())

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 3 && f[1] == 8 && f[2] == 1 && f[3] == 1 && f[4] == 0}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 3 && f[1] == 8 && f[2] == 1 && f[3] == 1 && f[4] == 0}).collect(Collectors.toList()).get(0)[5] - 7.683) < 0.001f
	}
	
	def "Search - No invert - Not Equal"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4], n[8], n[9], nd2, n[6], n[7], n[5], nd2] as Node[])

		when:
		def impList = service.search(sol.getGiantRoute())

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 4 && f[1] == 9 && f[2] == 1 && f[3] == 0 && f[4] == 0}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 4 && f[1] == 9 && f[2] == 1 && f[3] == 0 && f[4] == 0}).collect(Collectors.toList()).get(0)[5] - 8.738) < 0.001f
	}
	
	def "Search - Invert - Equal"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[8], n[9], nd2, n[6], n[7], n[5], n[4], nd2] as Node[])

		when:
		def impList = service.search(sol.getGiantRoute())

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 3 && f[1] == 8 && f[2] == 1 && f[3] == 1 && f[4] == 2}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 3 && f[1] == 8 && f[2] == 1 && f[3] == 1 && f[4] == 2}).collect(Collectors.toList()).get(0)[5] - 7.767) < 0.001f
	}

	def "Search - Invert - Not Equal"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4], n[9], n[8], nd2, n[6], n[7], n[5], nd2] as Node[])

		when:
		def impList = service.search(sol.getGiantRoute())

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 4 && f[1] == 9 && f[2] == 1 && f[3] == 0 && f[4] == 1}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 4 && f[1] == 9 && f[2] == 1 && f[3] == 0 && f[4] == 1}).collect(Collectors.toList()).get(0)[5] - 8.418) < 0.001f
	}

	def "Search - Invert - Not Equal - With deactivated invert"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		service.setInvertationMode(false)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4], n[9], n[8], nd2, n[6], n[7], n[5], nd2] as Node[])

		when:
		def impList = service.search(sol.getGiantRoute())

		then:
		impList != null
		impList.stream().filter({f -> f[4] == 1}).count() == 0
	}
	
	def "Search - Invert - Not Equal - With activated equal-condition"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		service.setEqualSegmentLength(true)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4], n[9], n[8], nd2, n[6], n[7], n[5], nd2] as Node[])

		when:
		def impList = service.search(sol.getGiantRoute())

		then:
		impList != null
		impList.stream().filter({f -> f[2] != f[3]}).count() == 0
	}

	def "Search - Invert - Not Equal - No improvement"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[3], n[2], n[6], n[7], n[8], n[9], n[5], n[4], nd] as Node[])

		when:
		def impList = service.search(sol.getGiantRoute())

		then:
		impList.size() == 0
	}

	def "Change with Both Invert"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		service.setInvertationMode(false)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[9], n[8], nd2, n[6], n[7], n[5], n[4], nd2] as Node[])

		def changeMove = [3, 8, 1, 1, 3] as float[]

		when:
		service.change(sol, changeMove)

		def newGiantRoute = sol.getGiantRoute()

		then:
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[2]
		newGiantRoute[2] == n[3]
		newGiantRoute[3] == n[4]
		newGiantRoute[4] == n[5]
		newGiantRoute[5] == nd2
		newGiantRoute[6] == n[6]
		newGiantRoute[7] == n[7]
		newGiantRoute[8] == n[8]
		newGiantRoute[9] == n[9]
		newGiantRoute[10] == nd2
	}
	
	def "Change with Both Invert - Not equal"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		service.setInvertationMode(false)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[9], n[8], n[7], nd2, n[6], n[4], n[5], nd2] as Node[])

		def changeMove = [3, 8, 2, 1, 1] as float[]

		when:
		service.change(sol, changeMove)

		def newGiantRoute = sol.getGiantRoute()

		then:
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[2]
		newGiantRoute[2] == n[3]
		newGiantRoute[3] == n[4]
		newGiantRoute[4] == n[5]
		newGiantRoute[5] == nd2
		newGiantRoute[6] == n[6]
		newGiantRoute[7] == n[7]
		newGiantRoute[8] == n[8]
		newGiantRoute[9] == n[9]
		newGiantRoute[10] == nd2
	}

	def "Change without Invert"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		service.setInvertationMode(false)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[8], n[9], nd2, n[6], n[7], n[4], n[5], nd2] as Node[])

		def changeMove = [3, 8, 1, 1, 0] as float[]

		when:
		service.change(sol, changeMove)

		def newGiantRoute = sol.getGiantRoute()

		then:
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[2]
		newGiantRoute[2] == n[3]
		newGiantRoute[3] == n[4]
		newGiantRoute[4] == n[5]
		newGiantRoute[5] == nd2
		newGiantRoute[6] == n[6]
		newGiantRoute[7] == n[7]
		newGiantRoute[8] == n[8]
		newGiantRoute[9] == n[9]
		newGiantRoute[10] == nd2
	}

	def "Reverse Change"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		service.setInvertationMode(false)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4], n[5], nd2, n[6], n[7], n[8], n[9], nd2] as Node[])

		def changeMove = [3, 8, 1, 1, 2] as float[]

		when:
		service.reverseChange(sol, changeMove)

		def newGiantRoute = sol.getGiantRoute()

		then:
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[2]
		newGiantRoute[2] == n[3]
		newGiantRoute[3] == n[8]
		newGiantRoute[4] == n[9]
		newGiantRoute[5] == nd2
		newGiantRoute[6] == n[6]
		newGiantRoute[7] == n[7]
		newGiantRoute[8] == n[5]
		newGiantRoute[9] == n[4]
		newGiantRoute[10] == nd2
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
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 3,
				externID: "2",
				xlong: -2,
				ylat: 1,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 4,
				externID: "3",
				xlong: -2,
				ylat: -1,
				geoId: 4,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 5,
				externID: "4",
				xlong: -2,
				ylat: -2,
				geoId: 5,
				demand: [1, 1],
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
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n6 = new TestNode(
				globalIdx: 7,
				externID: "6",
				xlong: 2,
				ylat: 1,
				geoId: 7,
				demand: [1, 1],
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
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n8 = new TestNode(
				globalIdx: 9,
				externID: "8",
				xlong: 2,
				ylat: -2,
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
