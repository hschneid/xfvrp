package xf.xfvrp.opt.construct.insert

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator

class XFVRPFirstBestInsertSpec extends Specification {

	def service = new XFVRPFirstBestInsert()

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

	def "Insert Customer"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def giantRoute = [nd, n[2], n[3], nd] as Node[]
		def newGiantRoute = [nd, n[2], n[3], nd, []] as Node[]

		def insertCustomer = n[4]
		def pos = 2

		when:
		service.insertCustomer(giantRoute, newGiantRoute, insertCustomer, pos)

		then:
		newGiantRoute[0] == nd
		newGiantRoute[1] == n[2]
		newGiantRoute[2] == n[4]
		newGiantRoute[3] == n[3]
		newGiantRoute[4] == nd
	}

	def "Remove Customer"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def giantRoute = [nd, n[2], n[4], n[3], nd] as Node[]
		def reducedGiantRoute = [nd, n[2], n[4], n[3]] as Node[]

		def removedCustomer = n[4]

		when:
		def newGiantRoute = service.removeCustomer(giantRoute, reducedGiantRoute, removedCustomer)

		then:
		reducedGiantRoute[0] == nd
		reducedGiantRoute[1] == n[2]
		reducedGiantRoute[2] == n[3]
		reducedGiantRoute[3] == nd
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

	def "Get Customers"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		when:
		def customers = service.getCustomers()

		then:
		customers.size() == 8
		customers[0] == n[2]
		customers[1] == n[3]
		customers[2] == n[4]
		customers[3] == n[5]
		customers[4] == n[6]
		customers[5] == n[7]
		customers[6] == n[8]
		customers[7] == n[9]
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
		route.size()== 5
		route[0] == giantRoute[3]
		route[1] == null
		route[2] == giantRoute[4]
		route[3] == giantRoute[5]
		route[4] == giantRoute[3]
	}

	def "Evaluate"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def giantRoute = [nd, n[2], n[3], nd, n[4], nd] as Node[]
		def insertCustomer = n[5]

		when:
		def result = service.evaluate(giantRoute, insertCustomer)

		then:
		result.size() == 5
		result[0][0] == 1
		result[1][0] == 2
		result[2][0] == 3
		result[3][0] == 4
		result[4][0] == 5
		Math.abs(result[0][1] - 4) <= 0.001
		Math.abs(result[1][1] - 6) <= 0.001
		Math.abs(result[2][1] - 3.822) <= 0.001
		Math.abs(result[3][1] - 1.822) <= 0.001
		Math.abs(result[4][1] - 1.822) <= 0.001
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
		nd2.setIdx(1)
		n1.setIdx(2)
		n2.setIdx(3)
		n3.setIdx(4)
		n4.setIdx(5)
		n5.setIdx(6)
		n6.setIdx(7)
		n7.setIdx(8)
		n8.setIdx(9)

		def nodes = [nd, nd2, n1, n2, n3, n4, n5, n6, n7, n8] as Node[]

		def iMetric = new AcceleratedMetricTransformator().transform(metric, nodes, v)

		return TestXFVRPModel.get(nodes, iMetric, iMetric, v, parameter)
	}
}
