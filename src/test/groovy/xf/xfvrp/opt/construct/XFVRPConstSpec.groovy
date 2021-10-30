package xf.xfvrp.opt.construct

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.base.metric.internal.AcceleratedMetric

class XFVRPConstSpec extends Specification {

	AcceleratedMetric metric = Stub(AcceleratedMetric, constructorArgs: [5])
	def service = new XFVRPConst()

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
	
	def nr = new TestNode(
		externID: "REP",
		globalIdx: 1,
		xlong: 1,
		ylat: 0,
		siteType: SiteType.REPLENISH,
		demand: [0, 0],
		timeWindow: [[0,99],[2,99]]
		).getNode()

	def sol

	def parameter = new XFVRPParameter()

	def "Build Giant Route For Optimization"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def customers = [n[2], n[3], n[4]]

		when:
		def result = service.buildGiantRouteForOptimization(nd, customers, model)
		def giantRoute = result.getGiantRoute()

		then:
		giantRoute[0].externID == nd.externID
		giantRoute[1] == n[2]
		giantRoute[2].externID == nd.externID
		giantRoute[3] == n[3]
		giantRoute[4].externID == nd.externID
		giantRoute[5] == n[4]
		giantRoute[6].externID == nd.externID
	}
	
	def "Build Giant Route For Optimization - empty customers"() {
		def model = initScen()
		service.setModel(model)

		def customers = []

		when:
		def result = service.buildGiantRouteForOptimization(nd, customers, model)
		def giantRoute = result.getGiantRoute()

		then:
		giantRoute[0].externID == nd.externID
	}
	
	def "Build Giant Route For Optimization - with blocks"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		
		n[2].presetBlockIdx = 11
		n[3].presetBlockIdx = 11

		def customers = [n[2], n[3], n[4]]
		
		when:
		def result = service.buildGiantRouteForOptimization(nd, customers, model)
		def giantRoute = result.getGiantRoute()

		then:
		giantRoute[0].externID == nd.externID
		giantRoute[1] == n[2]
		giantRoute[2] == n[3]
		giantRoute[3].externID == nd.externID
		giantRoute[4] == n[4]
		giantRoute[5].externID == nd.externID
	}
	
	def "Find Nearest Depot with multi depots"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		
		def depots = [nd, nd2]
		def customer = n[2]
		
		metric.getDistance(nd, n[2]) >> 15
		metric.getDistance(nd, n[2]) >> 5
		
		when:
		def result = service.findNearestDepot(depots, customer)

		then:
		result == 1
	}
	
	def "Find Nearest Depot with single depot"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		
		def depots = [nd]
		def customer = n[2]
		
		metric.getDistance(nd, n[2]) >> 155
		
		when:
		def result = service.findNearestDepot(depots, customer)

		then:
		result == 0
	}
	
	def "Find Nearest Depot with no depots"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		
		def depots = []
		def customer = n[2]
		
		metric.getDistance(_, n[2]) >> 155
		
		when:
		service.findNearestDepot(depots, customer)

		then:
		thrown XFVRPException
	}
	
	XFVRPModel initScen() {
		def v = new TestVehicle(name: "V1", capacity: [30, 30]).getVehicle()

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
		nd2.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)
		n4.setIdx(5)
		n5.setIdx(6)
		n6.setIdx(7)
		n7.setIdx(8)
		n8.setIdx(9)
		nr.setIdx(10)

		def nodes = [nd, nd2, n1, n2, n3, n4, n5, n6, n7, n8, nr] as Node[]

		return TestXFVRPModel.get(nodes, metric, metric, v, parameter)
	}
}
