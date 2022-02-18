package xf.xfvrp.opt.construct.insert

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator

class XFPDPFirstBestInsertSpec extends Specification {

	def service = new XFPDPFirstBestInsert()

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

	def parameter = new XFVRPParameter()

	def "Insert Shipment"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def giantRoute = [nd, n[2], n[3], nd] as Node[]
		def newGiantRoute = [nd, n[2], n[3], nd, [], []] as Node[]

		def insertPickup = n[4]
		def insertDelivery = n[5]
		def pickPos = 2
		def delPos = 3

		when:
		service.insertShipment(giantRoute, newGiantRoute, insertPickup, insertDelivery, pickPos, delPos)

		then:
		newGiantRoute.length == 6
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[2]
		newGiantRoute[2] == n[4]
		newGiantRoute[3] == n[3]
		newGiantRoute[4] == n[5]
		newGiantRoute[5] == nd
	}

	def "Remove Shipment"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def giantRoute = [nd, n[2], n[4], n[3], n[5], nd] as Node[]
		def reducedGiantRoute = [nd, n[2], n[4], n[3]] as Node[]

		def removedPick = n[4]
		def removedDeli = n[5]

		when:
		def newGiantRoute = service.removeShipment(giantRoute, reducedGiantRoute, removedPick, removedDeli)

		then:
		reducedGiantRoute.length == 4
		reducedGiantRoute[0] == nd
		reducedGiantRoute[1] == n[2]
		reducedGiantRoute[2] == n[3]
		reducedGiantRoute[3] == nd
		reducedGiantRoute.length == 4
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[2]
		newGiantRoute[2] == n[3]
		newGiantRoute[3] == nd
	}

	def "Init Route"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		when:
		def giantRoute = service.initRoute()

		then:
		giantRoute.length == 3
		giantRoute[0].globalIdx == nd.globalIdx
		giantRoute[1].globalIdx == nd2.globalIdx
		giantRoute[2].globalIdx == nd2.globalIdx
	}

	def "Get Shipments"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		when:
		def shipments = service.getShipments()

		then:
		shipments.size() == 4
		shipments.stream().filter({f -> f[0] == n[2] && f[1] == n[3]}).count() == 1
		shipments.stream().filter({f -> f[0] == n[4] && f[1] == n[5]}).count() == 1
		shipments.stream().filter({f -> f[0] == n[6] && f[1] == n[7]}).count() == 1
		shipments.stream().filter({f -> f[0] == n[8] && f[1] == n[9]}).count() == 1
	}

	def "Get Routes"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def giantRoute = [nd, n[2], n[6], nd2, n[4], n[3], nd, n[8], nd, nd2] as Node[]

		when:
		def routes = service.getRoutes(giantRoute)

		then:
		routes.size() == 4
		routes[0][0] == 0
		routes[0][1] == 3
		routes[1][0] == 3
		routes[1][1] == 6
		routes[2][0] == 6
		routes[2][1] == 8
		routes[3][0] == 8
		routes[3][1] == 9
	}

	def "Extract Route"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def giantRoute = [nd, n[2], n[6], nd2, n[4], n[3], nd, n[8], nd, nd2] as Node[]

		when:
		def route = service.createEvaluationRoute(giantRoute, [3, 6] as int[])

		then:
		route.size() == 6
		route[0] == giantRoute[3]
		route[1] == null
		route[2] == null
		route[3] == giantRoute[4]
		route[4] == giantRoute[5]
		route[5] == giantRoute[6]
	}

	def "Evaluate"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def giantRoute = [nd, n[2], n[3], nd] as Node[]
		def shipment = [n[4], n[5]] as Node[]

		when:
		def result = service.evaluate(giantRoute, shipment)

		then:
		result.size() == 6
		result.stream().filter({f -> f[0] == 1 && f[1] == 1 && Math.abs(f[2] - 4.17) <= 0.01}).count() == 1
		result.stream().filter({f -> f[0] == 1 && f[1] == 2 && Math.abs(f[2] - 8.17) <= 0.01}).count() == 1
		result.stream().filter({f -> f[0] == 1 && f[1] == 3 && Math.abs(f[2] - 6) <= 0.001}).count() == 1
		result.stream().filter({f -> f[0] == 2 && f[1] == 2 && Math.abs(f[2] - 6) <= 0.001}).count() == 1
		result.stream().filter({f -> f[0] == 2 && f[1] == 3 && Math.abs(f[2] - 7.82) <= 0.01}).count() == 1
		result.stream().filter({f -> f[0] == 3 && f[1] == 3 && Math.abs(f[2] - 3.82) <= 0.01}).count() == 1
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

		def nodes = [nd, nd2, n1, n2, n3, n4, n5, n6, n7, n8]

		parameter.setWithPDP(true)

		return TestXFVRPModel.get(nodes, v, parameter)
	}
}
