package xf.xfvrp.opt.improve

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.evaluation.EvaluationService
import xf.xfvrp.opt.improve.giantroute.XFVRP3CyclicTransfer

import java.util.stream.Collectors

class XFVRP3CyclicTransferSpec extends Specification {

	def service = new XFVRP3CyclicTransfer()
	
	def s = new EvaluationService()

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

	def sol

	def parameter = new XFVRPParameter()

	def metric = new EucledianMetric()

	def "Search - found"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[7], n[3], n[2], nd, n[5], n[6], n[4], n[8], nd] as Node[])

		when:
		def impList = service.search(sol.getGiantRoute())
		
		then:
		impList.size() > 0
		impList.stream().filter({f -> f[0] == 2 && f[1] == 8 && f[2] == 4}).count() == 1
		Math.abs(impList.stream().filter({f -> f[0] == 2 && f[1] == 8 && f[2] == 4}).collect(Collectors.toList()).get(0)[3] - 12.178) < 0.001f
	}

	def "Search - no more found"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[2], n[1], n[5], n[6], n[7], n[8], n[4], n[3], nd] as Node[])

		when:
		def impList = service.search(sol.getGiantRoute())
		
		then:
		impList.size() == 0
	}

		
	def "Change"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[7], n[3], n[2], nd, n[5], n[6], n[4], n[8], nd] as Node[])

		def changeMove = [2, 8, 4] as float[]

		when:
		service.change(sol, changeMove)

		def newGiantRoute = sol.getGiantRoute()

		then:
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[1]
		newGiantRoute[2] == n[2]
		newGiantRoute[3] == n[3]
		newGiantRoute[4] == n[4]
		newGiantRoute[5] == nd
		newGiantRoute[6] == n[5]
		newGiantRoute[7] == n[6]
		newGiantRoute[8] == n[7]
		newGiantRoute[9] == n[8]
		newGiantRoute[10] == nd
	}

	def "Reverse Change Both Invert"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		sol = new Solution()
		sol.setGiantRoute([nd, n[1], n[2], n[3], n[4], nd, n[5], n[6], n[7], n[8], nd] as Node[])

		def changeMove = [2, 8, 4] as float[]

		when:
		service.reverseChange(sol, changeMove)

		def newGiantRoute = sol.getGiantRoute()

		then:
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[1]
		newGiantRoute[2] == n[7]
		newGiantRoute[3] == n[3]
		newGiantRoute[4] == n[2]
		newGiantRoute[5] == nd
		newGiantRoute[6] == n[5]
		newGiantRoute[7] == n[6]
		newGiantRoute[8] == n[4]
		newGiantRoute[9] == n[8]
		newGiantRoute[10] == nd
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
		nd.setIdx(0)
		n1.setIdx(1)
		n2.setIdx(2)
		n3.setIdx(3)
		n4.setIdx(4)
		n5.setIdx(5)
		n6.setIdx(6)
		n7.setIdx(7)
		n8.setIdx(8)

		def nodes = [nd, n1, n2, n3, n4, n5, n6, n7, n8] as Node[]

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v)

		return TestXFVRPModel.get(nodes, iMetric, iMetric, v, parameter)
	}
}
