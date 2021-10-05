package xf.xfvrp.opt

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import xf.xfvrp.base.*
import xf.xfvrp.base.metric.Metric
import xf.xfvrp.base.metric.internal.AcceleratedMetric
import xf.xfvrp.base.monitor.StatusManager
import xf.xfvrp.base.preset.BlockNameConverter
import xf.xfvrp.opt.evaluation.Context
import xf.xfvrp.opt.fleetmix.DefaultMixedFleetHeuristic
import xf.xfvrp.report.Event
import xf.xfvrp.report.RouteReport

class FullRouteMixedFleetHeuristicSpec extends Specification {

	def service = new DefaultMixedFleetHeuristic()

	def testVehicle
	def routeReport


	def setup() {
		testVehicle = new TestVehicle(
				fixCost: 11,
				varCost: 5
				)
		routeReport = new RouteReport(testVehicle.getVehicle())
		routeReport.getSummary().duration = 1234
		routeReport.getSummary().pickups = [555]
		routeReport.getSummary().deliveries = [666]
		routeReport.getSummary().delay = 0
	}

	def "Get unused nodes - normal"() {
		def nodes = [
			new TestNode(externID: 'n0', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n3', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n4', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n5', siteType: SiteType.CUSTOMER).getNode()
		] as List<Node>

		def context = new Context()

		def routeReport1 = new RouteReport(testVehicle.getVehicle())
		routeReport1.add(new Event(nodes[1]), context)
		routeReport1.add(new Event(nodes[2]), context)

		def routeReport2 = new RouteReport(testVehicle.getVehicle())
		routeReport2.add(new Event(nodes[3]), context)
		routeReport2.add(new Event(nodes[4]), context)

		def routes = [routeReport1, routeReport2] as List<RouteReport>

		when:
		def result = service.getUnusedNodes(routes, nodes)

		then:
		result != null
		result.size() == 2
		result.contains(nodes[0])
		result.contains(nodes[5])
	}

	def "Get unused nodes - no routes"() {
		def nodes = [
			new TestNode(externID: 'n0', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode(),
		] as List<Node>

		def routes = [] as List<RouteReport>

		when:
		def result = service.getUnusedNodes(routes, nodes)

		then:
		result != null
		result.size() == 2
		result.contains(nodes[0])
		result.contains(nodes[1])
	}

	def "Get unused nodes - no nodes"() {
		def nodes = [] as List<Node>

		def routeReport1 = new RouteReport(testVehicle.getVehicle())

		def routeReport2 = new RouteReport(testVehicle.getVehicle())

		def routes = [routeReport1, routeReport2] as List<RouteReport>

		when:
		def result = service.getUnusedNodes(routes, nodes)

		then:
		result != null
		result.size() == 0
	}

	def "Reconstruct giant route - normal"() {
		def nodes = [
			new TestNode(externID: 'n0', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n3', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n4', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n5', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()
		]
		def model = Stub XFVRPModel
		model.getNodes() >> nodes
		model.getVehicle() >> testVehicle.getVehicle()
		def context = new Context()

		def routeReport1 = new RouteReport(testVehicle.getVehicle())
		routeReport1.add(new Event(nodes[6]), context)
		routeReport1.add(new Event(nodes[1]), context)
		routeReport1.add(new Event(nodes[2]), context)
		routeReport1.add(new Event(nodes[6]), context)

		def routeReport2 = new RouteReport(testVehicle.getVehicle())
		routeReport2.add(new Event(nodes[6]), context)
		routeReport2.add(new Event(nodes[3]), context)
		routeReport2.add(new Event(nodes[4]), context)
		routeReport2.add(new Event(nodes[6]), context)

		def routeReport3 = new RouteReport(testVehicle.getVehicle())
		routeReport3.add(new Event(nodes[6]), context)
		routeReport3.add(new Event(nodes[6]), context)

		def routes = [routeReport1, routeReport3, routeReport2] as List<RouteReport>

		when:
		def result = service.reconstructGiantRoute(routes, model)
		def gT = result.solution.getGiantRoute()

		then:
		result != null
		gT != null
		gT[0].externID == 'nD'
		gT[1].externID == 'n1'
		gT[2].externID == 'n2'
		gT[3].externID == 'nD'
		gT[4].externID == 'nD'
		gT[5].externID == 'n3'
		gT[6].externID == 'n4'
		gT[7].externID == 'nD'
	}

	def "Reconstruct giant route - empty routes"() {
		def nodes = [
			new TestNode(externID: 'n0', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n3', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n4', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n5', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()
		]
		def model = Stub XFVRPModel
		model.getNodes() >> nodes
		model.getVehicle() >> testVehicle.getVehicle()


		def routes = [] as List<RouteReport>

		when:
		def result = service.reconstructGiantRoute(routes, model)
		def gT = result.solution.getGiantRoute()

		then:
		result != null
		gT.length == 0
	}

	def "Build Giant Route For Invalid Nodes - normal"() {
		def statusManager = Stub StatusManager
		def depot = new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()
		def n1 = new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode()
		def n2 = new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode()

		when:
		def result = service.buildGiantRouteForInvalidNodes([n1, n2] as List<Node>, depot, statusManager)
		def gT = result.getGiantRoute()

		then:
		result != null
		gT != null
		gT.length == 5
		gT[0].externID == 'nD'
		gT[1].externID == 'n1'
		gT[2].externID == 'nD'
		gT[3].externID == 'n2'
		gT[4].externID == 'nD'
	}

	def "Build Giant Route For Invalid Nodes - with Blocks"() {
		def statusManager = Stub StatusManager
		def depot = new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()
		def n1 = new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER, presetBlockIdx: 2).getNode()
		def n2 = new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER, presetBlockIdx: BlockNameConverter.DEFAULT_BLOCK_IDX).getNode()
		def n3 = new TestNode(externID: 'n3', siteType: SiteType.CUSTOMER, presetBlockIdx: 2).getNode()

		when:
		def result = service.buildGiantRouteForInvalidNodes([n1, n2, n3] as List<Node>, depot, statusManager)
		def gT = result.getGiantRoute()

		then:
		result != null
		gT != null
		gT.length == 6
		gT[0].externID == 'nD'
		gT[1].externID == 'n2'
		gT[2].externID == 'nD'
		gT[3].externID == 'n1'
		gT[4].externID == 'n3'
		gT[5].externID == 'nD'
	}

	def "Build Giant Route For Invalid Nodes - no unplanned"() {
		def statusManager = Stub StatusManager
		def depot = new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()

		when:
		def result = service.buildGiantRouteForInvalidNodes([] as List<Node>, depot, statusManager)
		def gT = result.getGiantRoute()

		then:
		result != null
		gT != null
		gT.length == 0
	}

	def "Insert Unplanned Nodes - normal"() {
		def parameter = Stub XFVRPParameter
		def statusManager = Stub StatusManager
		def metric = Stub Metric
		metric.getDistanceAndTime(_, _, _) >> [1, 1]

		def nodes = [
			new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode(),
			new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER, invalidReason: InvalidReason.TIME_WINDOW).getNode(),
			new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode()
		]

		when:
		def result = service.insertUnplannedNodes(nodes, metric, parameter, statusManager)
		def gT = result.getSolution().getGiantRoute()

		then:
		result != null
		gT != null
		result.getModel().getVehicle().name.contains("INVALID")
		gT[0].externID == 'nD'
		gT[1].externID == 'n1'
		gT[2].externID == 'nD'
		gT[3].externID == 'n2'
		gT[4].externID == 'nD'
		nodes[0].idx == 0
		nodes[1].idx == 1
		nodes[2].idx == 2
		nodes[1].invalidReason == InvalidReason.TIME_WINDOW
		nodes[2].invalidReason == InvalidReason.UNPLANNED
	}

	def "Insert Unplanned Nodes - no customer"() {
		def parameter = Stub XFVRPParameter
		def statusManager = Stub StatusManager
		def metric = Stub Metric
		metric.getDistanceAndTime(_, _, _) >> [1, 1]

		def nodes = [
			new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()
		]

		when:
		def result = service.insertUnplannedNodes(nodes, metric, parameter, statusManager)

		then:
		result == null
	}

	def "Execute - normal"() {
		def parameter = Stub XFVRPParameter
		def statusManager = Stub StatusManager
		def metric = Stub Metric
		def iMetric = Spy(AcceleratedMetric, constructorArgs: [1])
		metric.getDistanceAndTime(_, _, _) >> [1, 1]
		def solution = Stub Solution

		def nodes = [
			new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode(),
			new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n3', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n4', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n5', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n6', siteType: SiteType.CUSTOMER).getNode()
		] as Node[]

		solution.getGiantRoute() >> [nodes[0], nodes[0]]

		def vehicles = [
			new TestVehicle(name: 'V1', fixCost: 11, varCost: 5).getVehicle(),
			new TestVehicle(name: 'V2', fixCost: 11, varCost: 5).getVehicle(),
			new TestVehicle(name: 'V3', fixCost: 14, varCost: 6).getVehicle()
		] as Vehicle[]
		def context = new Context()

		def routeReport1 = new RouteReport(vehicles[0])
		routeReport1.add(new Event(nodes[0]), context)
		routeReport1.add(new Event(nodes[1]), context)
		routeReport1.add(new Event(nodes[2]), context)
		routeReport1.add(new Event(nodes[0]), context)

		def routeReport2 = new RouteReport(vehicles[0])
		routeReport2.add(new Event(nodes[0]), context)
		routeReport2.add(new Event(nodes[3]), context)
		routeReport2.add(new Event(nodes[4]), context)
		routeReport2.add(new Event(nodes[0]), context)

		def routeReport3 = new RouteReport(vehicles[1])
		routeReport3.add(new Event(nodes[0]), context)
		routeReport3.add(new Event(nodes[5]), context)
		routeReport3.add(new Event(nodes[0]), context)
		

		service.getSelector().getBestRoutes(vehicles[0], _) >> [routeReport1, routeReport2]
		service.getSelector().getBestRoutes(vehicles[1], _) >> [routeReport3]
		service.getSelector().getBestRoutes(vehicles[2], _) >> []

		when:
		def result = service.execute(nodes, vehicles, {routingDataBag ->
			def model = new XFVRPModel(nodes, iMetric, iMetric, routingDataBag.vehicle, parameter)
			return new XFVRPSolution(solution, model)
		}, metric, parameter, statusManager)

		def s1 = result.stream().filter({f -> f.getModel().getVehicle().name == 'V1'}).findFirst().get()
		def s2 = result.stream().filter({f -> f.getModel().getVehicle().name == 'V2'}).findFirst().get()
		def s3 = result.stream().filter({f -> f.getModel().getVehicle().name == 'INVALID'}).findFirst().get()

		then:
		result != null
		result.size() == 3
		s1.getSolution().getGiantRoute()[0].externID == 'nD'
		s1.getSolution().getGiantRoute()[1].externID == 'n1'
		s1.getSolution().getGiantRoute()[2].externID == 'n2'
		s1.getSolution().getGiantRoute()[3].externID == 'nD'
		s1.getSolution().getGiantRoute()[4].externID == 'nD'
		s1.getSolution().getGiantRoute()[5].externID == 'n3'
		s1.getSolution().getGiantRoute()[6].externID == 'n4'
		s1.getSolution().getGiantRoute()[7].externID == 'nD'
		s2.getSolution().getGiantRoute()[0].externID == 'nD'
		s2.getSolution().getGiantRoute()[1].externID == 'n5'
		s2.getSolution().getGiantRoute()[2].externID == 'nD'
		s3.getSolution().getGiantRoute()[0].externID == 'nD'
		s3.getSolution().getGiantRoute()[1].externID == 'n6'
		s3.getSolution().getGiantRoute()[2].externID == 'nD'
	}
	
	def "Execute - no vehicles"() {
		def parameter = Stub XFVRPParameter
		def statusManager = Stub StatusManager
		def metric = Stub Metric
		def iMetric = Spy(AcceleratedMetric, constructorArgs: [1])
		metric.getDistanceAndTime(_, _, _) >> [1, 1]
		def solution = Stub Solution

		def nodes = [
			new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode(),
			new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n3', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n4', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n5', siteType: SiteType.CUSTOMER).getNode(),
			new TestNode(externID: 'n6', siteType: SiteType.CUSTOMER).getNode()
		] as Node[]

		solution.getGiantRoute() >> [nodes[0], nodes[0]]

		def vehicles = [] as Vehicle[]

		when:
		def result = service.execute(nodes, vehicles, {routingDataBag ->
			def model = new XFVRPModel(nodes, iMetric, iMetric, routingDataBag.vehicle, parameter)
			return new XFVRPSolution(solution, model)
		}, metric, parameter, statusManager)

		then:
		result != null
		result.size() == 1
		result.get(0).model.getVehicle().name.contains('INVALID')
	}
}
