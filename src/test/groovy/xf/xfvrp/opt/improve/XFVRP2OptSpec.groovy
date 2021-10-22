package xf.xfvrp.opt.improve

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.improve.giantroute.XFVRP2Opt

import java.util.stream.Collectors

class XFVRP2OptSpec extends Specification {

	def service = new XFVRP2Opt()

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

	def sol

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Search single depot - Find improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[5], n[4], nd, nd, n[3], nd] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchSingleDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 2 && f[1] == 6}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 2 && f[1] == 6}).collect(Collectors.toList()).get(0)[2] - 1.390) < 0.001f
	}

	def "Search single depot - Find No improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], n[4], n[5], nd] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchSingleDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() == 0
	}
	
	def "Search multi depot - Find improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[4], nd2, n[3], n[5], nd2] as Node[])

		def impList = [] as List<float[]>

		when:
		service.searchMultiDepot(sol.getGiantRoute(), impList)

		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 2 && f[1] == 4}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 2 && f[1] == 4}).collect(Collectors.toList()).get(0)[2] - 8.385) < 0.001f
	}

	def "Search multi depot - Find No improve"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[3], nd2, n[4], n[5], nd2] as Node[])

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
				loadType: LoadType.DELIVERY)
				.getNode()
		def n2 = new TestNode(
				globalIdx: 2,
				externID: "2",
				xlong: -1,
				ylat: -1,
				geoId: 2,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n3 = new TestNode(
				globalIdx: 3,
				externID: "3",
				xlong: 4,
				ylat: 0,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()
		def n4 = new TestNode(
				globalIdx: 4,
				externID: "4",
				xlong: 4,
				ylat: 1,
				geoId: 3,
				demand: [1, 1],
				timeWindow: [[0,99]],
				loadType: LoadType.DELIVERY)
				.getNode()

		nd.setIdx(0)
		nd2.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)
		n4.setIdx(5)

		def nodes = [nd, nd2, n1, n2, n3, n4] as Node[]

		return TestXFVRPModel.get(Arrays.asList(nodes), v)
	}
}
