package xf.xfvrp.opt.construct

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator
import xf.xfvrp.opt.Solution

class XFVRPSavingsSpec extends Specification {

	def service = new XFVRPSavings()

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

	def "Prepare"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[
						[n[2], n[7], n[3]],
						[n[4], n[5]]
				] as Node[][]
		)

		when:
		service.prepare(dataBag)
		def nodeList = dataBag.getNodeList()

		then:
		dataBag.getRouteIdxForStartNode()[2] == 0
		dataBag.getRouteIdxForStartNode()[3] == -1
		dataBag.getRouteIdxForEndNode()[3] == 0
		dataBag.getRouteIdxForStartNode()[4] == 1
		dataBag.getRouteIdxForEndNode()[5] == 1
		dataBag.getRouteIdxForStartNode()[7] == -1
		dataBag.getRouteIdxForEndNode()[7] == -1
		nodeList.get(0) == n[2]
		nodeList.get(1) == n[3]
		nodeList.get(2) == n[4]
		nodeList.get(3) == n[5]
	}

	def "Create savings"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def depot = nd

		def dataBag = new SavingsDataBag(null)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, -1, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, -1, -1, -1, -1]

		when:
		service.createSavingsMatrix(depot, dataBag)
		def savings = dataBag.getSavingsMatrix()

		then:
		savings.size() == 6
		savings.stream().filter({f -> f[0] == 2 && f[1] == 4 && Math.abs(f[2] - 1537) < 1}).count() == 1
		savings.stream().filter({f -> f[0] == 2 && f[1] == 5 && Math.abs(f[2] - 2118) < 1}).count() == 1
		savings.stream().filter({f -> f[0] == 3 && f[1] == 5 && Math.abs(f[2] - 1537) < 1}).count() == 1
		savings.stream().filter({f -> f[0] == 4 && f[1] == 2 && Math.abs(f[2] - 1537) < 1}).count() == 1
		savings.stream().filter({f -> f[0] == 5 && f[1] == 2 && Math.abs(f[2] - 2118) < 1}).count() == 1
		savings.stream().filter({f -> f[0] == 5 && f[1] == 3 && Math.abs(f[2] - 1537) < 1}).count() == 1
	}

	def "Merge routes without invertation"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[[n[2], n[7], n[3]],
				 [n[4], n[5]],
				 [n[6]]
				] as Node[][]
		)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, 2, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, 2, -1, -1, -1]

		when:
		def result = service.mergeRoutes(n[3].getIdx(), n[4].getIdx(), dataBag)

		then:
		result.length == 5
		result[0] == n[2]
		result[1] == n[7]
		result[2] == n[3]
		result[3] == n[4]
		result[4] == n[5]
	}

	def "Merge routes with invertation"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[
						[n[2], n[7], n[3]],
						[n[4], n[5]],
						[n[6]]
				] as Node[][]
		)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, 2, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, 2, -1, -1, -1]

		when:
		def result = service.mergeRoutes(n[6].getIdx(), n[5].getIdx(), dataBag)

		then:
		result.length == 3
		result[0] == n[6]
		result[1] == n[5]
		result[2] == n[4]
	}

	def "Add depots"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def routeWithoutDepots = [n[4], n[7], n[2]] as Node[]

		when:
		def result = service.addDepots(routeWithoutDepots, nd, nd2)

		then:
		result.length == 5
		result[0] == nd
		result[1] == n[4]
		result[2] == n[7]
		result[3] == n[2]
		result[4] == nd2
	}

	def "Is Saving available - Yes"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[
						[n[2], n[7], n[3]],
						[n[4], n[5]],
						[n[6]]
				] as Node[][]
		)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, 2, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, 2, -1, -1, -1]

		when:
		def result = service.isSavingAvailable(2, 5, dataBag)
		def result2 = service.isSavingAvailable(5, 2, dataBag)

		then:
		result
		result2
	}

	def "Is Saving available - No"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[
						[n[2], n[7], n[3]],
						[n[4], n[5]],
						[n[6]]
				] as Node[][]
		)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, 2, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, 2, -1, -1, -1]

		when:
		def result = service.isSavingAvailable(2, 7, dataBag)
		def result2 = service.isSavingAvailable(7, 2, dataBag)
		def result3 = service.isSavingAvailable(2, 3, dataBag)

		then:
		!result
		!result2
		!result3
	}

	def "Build routes"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)
		def sol = new Solution(model)
		sol.routes = [
				[nd, nd],
				[nd, n[2], n[7], n[3], nd],
				[nd, n[5], nr, n[4], nd],
				[nd, n[6], nd],
				[nd, nr, nd]
		] as Node[][]

		when:
		def result = service.buildRoutes(sol)
		def routes = result.getRoutes()

		then:
		routes.length == 3
		routes[0][0] == n[2]
		routes[0][1] == n[7]
		routes[0][2] == n[3]
		routes[1][0] == n[5]
		routes[1][1] == n[4]
		routes[2][0] == n[6]
	}

	def "Update routes - Invert B"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[
						[n[2], n[7], n[3]],
						[n[4], n[5]],
						[n[6]]
				] as Node[][]
		)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, 2, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, 2, -1, -1, -1]

		def newRoute = [n[2], n[7], n[3], n[5], n[4]] as Node[]

		when:
		service.updateRoutes(newRoute, 3, 5, dataBag)
		def routes = dataBag.getRoutes()

		then:
		routes[0][0] == n[2]
		routes[0][1] == n[7]
		routes[0][2] == n[3]
		routes[0][3] == n[5]
		routes[0][4] == n[4]
		routes[1] == null
		routes[2][0] == n[6]
		dataBag.getRouteIdxForStartNode()[2] == 0
		dataBag.getRouteIdxForStartNode()[3] == -1
		dataBag.getRouteIdxForEndNode()[3] == -1
		dataBag.getRouteIdxForStartNode()[5] == -1
		dataBag.getRouteIdxForEndNode()[5] == -1
		dataBag.getRouteIdxForStartNode()[4] == -1
		dataBag.getRouteIdxForEndNode()[4] == 0
	}

	def "Update routes - Invert A"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[
						[n[2], n[7], n[3]],
						[n[4], n[5]],
						[n[6]]
				] as Node[][]
		)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, 2, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, 2, -1, -1, -1]

		def newRoute = [n[3], n[7], n[2], n[4], n[5]] as Node[]

		when:
		service.updateRoutes(newRoute, 2, 4, dataBag)
		def routes = dataBag.getRoutes()

		then:
		routes[0][0] == n[3]
		routes[0][1] == n[7]
		routes[0][2] == n[2]
		routes[0][3] == n[4]
		routes[0][4] == n[5]
		routes[1] == null
		routes[2][0] == n[6]
		dataBag.getRouteIdxForStartNode()[3] == 0
		dataBag.getRouteIdxForStartNode()[2] == -1
		dataBag.getRouteIdxForEndNode()[2] == -1
		dataBag.getRouteIdxForStartNode()[4] == -1
		dataBag.getRouteIdxForEndNode()[4] == -1
		dataBag.getRouteIdxForStartNode()[5] == -1
		dataBag.getRouteIdxForEndNode()[5] == 0
	}

	def "Update routes - None invert"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[
						[n[2], n[7], n[3]],
						[n[4], n[5]],
						[n[6]]
				] as Node[][]
		)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, 2, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, 2, -1, -1, -1]

		def newRoute = [n[2], n[7], n[3], n[4], n[5]] as Node[]

		when:
		service.updateRoutes(newRoute, 3, 4, dataBag)
		def routes = dataBag.getRoutes()

		then:
		routes[0][0] == n[2]
		routes[0][1] == n[7]
		routes[0][2] == n[3]
		routes[0][3] == n[4]
		routes[0][4] == n[5]
		routes[1] == null
		routes[2][0] == n[6]
		dataBag.getRouteIdxForStartNode()[2] == 0
		dataBag.getRouteIdxForStartNode()[3] == -1
		dataBag.getRouteIdxForEndNode()[3] == -1
		dataBag.getRouteIdxForStartNode()[4] == -1
		dataBag.getRouteIdxForEndNode()[4] == -1
		dataBag.getRouteIdxForStartNode()[5] == -1
		dataBag.getRouteIdxForEndNode()[5] == 0
	}

	def "Update routes - Both invert"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[
						[n[2], n[7], n[3]],
						[n[4], n[5]],
						[n[6]]
				] as Node[][]
		)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, 2, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, 2, -1, -1, -1]

		def newRoute = [n[3], n[7], n[2], n[5], n[4]] as Node[]

		when:
		service.updateRoutes(newRoute, 2, 5, dataBag)
		def routes = dataBag.getRoutes()

		then:
		routes[0][0] == n[3]
		routes[0][1] == n[7]
		routes[0][2] == n[2]
		routes[0][3] == n[5]
		routes[0][4] == n[4]
		routes[1] == null
		routes[2][0] == n[6]
		dataBag.getRouteIdxForStartNode()[3] == 0
		dataBag.getRouteIdxForStartNode()[2] == -1
		dataBag.getRouteIdxForEndNode()[2] == -1
		dataBag.getRouteIdxForStartNode()[5] == -1
		dataBag.getRouteIdxForEndNode()[5] == -1
		dataBag.getRouteIdxForStartNode()[4] == -1
		dataBag.getRouteIdxForEndNode()[4] == 0
	}

	def "Apply Saving"() {
		def model = initScen()
		def n = model.getNodes()
		service.setModel(model)

		def dataBag = new SavingsDataBag(
				[
						[n[2], n[3]],
						[n[4], n[5]],
						[n[6]]
				] as Node[][]
		)
		dataBag.nodeList = [n[2], n[3], n[4], n[5]]
		dataBag.routeIdxForStartNode = [-1, -1, 0, -1, 1, -1, 2, -1, -1, -1]
		dataBag.routeIdxForEndNode = [-1, -1, -1, 0, -1, 1, 2, -1, -1, -1]
		dataBag.savingsMatrix = [[2, 4, 10.2] as float[], [3, 4, 12.2] as float[]] as ArrayList<float[]>

		when:
		def result = service.applyNextSaving(nd, dataBag)
		def routes = dataBag.getRoutes()

		then:
		result
		routes[0][0] == n[2]
		routes[0][1] == n[3]
		routes[0][2] == n[4]
		routes[0][3] == n[5]
		routes[1] == null
		routes[2][0] == n[6]
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

		def nodes = [nd, nd2, n1, n2, n3, n4, n5, n6, n7, n8, nr]

		return TestXFVRPModel.get(nodes, v)
	}
}
