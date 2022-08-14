package xf.xfvrp.opt

import spock.lang.Specification
import util.instances.Helper
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.*
import xf.xfvrp.base.compartment.CompartmentInitializer
import xf.xfvrp.base.compartment.CompartmentLoadBuilder
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.Metric
import xf.xfvrp.base.monitor.StatusManager
import xf.xfvrp.base.preset.BlockNameConverter
import xf.xfvrp.opt.evaluation.Context
import xf.xfvrp.opt.fleetmix.DefaultMixedFleetHeuristic
import xf.xfvrp.report.Event
import xf.xfvrp.report.RouteReport

class FullRouteMixedFleetHeuristicSpec extends Specification {

	def service = new DefaultMixedFleetHeuristic()
	def compartmentLoadBuilder = new CompartmentLoadBuilder()

	def testVehicle

	def setup() {
		testVehicle = new TestVehicle(
				fixCost: 11,
				varCost: 5,
				capacity: [0]
		)
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

		def model = TestXFVRPModel.get(nodes, testVehicle.getVehicle())

		def context = new Context()
		context.amountsOfRoute = compartmentLoadBuilder.createCompartmentLoads(model.getCompartments());

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
		def model = TestXFVRPModel.get(nodes, testVehicle.getVehicle())

		def routeReport1 = new RouteReport(testVehicle.getVehicle())

		def routeReport2 = new RouteReport(testVehicle.getVehicle())

		def routes = [routeReport1, routeReport2] as List<RouteReport>

		when:
		def result = service.getUnusedNodes(routes, nodes)

		then:
		result != null
		result.size() == 0
	}

	def "Reconstruct solution - normal"() {
		def nodes = [
				new TestNode(externID: 'n0', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n3', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n4', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n5', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()
		]
		def model = TestXFVRPModel.get(nodes, testVehicle.getVehicle())

		def context = new Context()
		context.amountsOfRoute = compartmentLoadBuilder.createCompartmentLoads(model.getCompartments());

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
		def result = service.reconstructSolution(routes, model)
		def gT = Helper.get(result)

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

	def "Reconstruct solution - empty routes"() {
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
		def result = service.reconstructSolution(routes, model)
		def gT = Helper.get(result)

		then:
		result != null
		gT.length == 0
	}

	def "Build solution For Invalid Nodes - normal"() {
		def statusManager = Stub StatusManager
		def depot = new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()
		def nodes = [
				new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode()
		]
		def allNodes = [depot] + nodes
		def model = TestXFVRPModel.get(allNodes, testVehicle.getVehicle())

		when:
		def result = service.buildSolutionForInvalidNodes(nodes, depot, model, statusManager)
		def gT = Helper.get(result)

		then:
		result != null
		result.model == model
		gT[0].externID == 'nD'
		gT[1].externID == 'n1'
		gT[2].externID == 'nD'
		gT[3].externID == 'nD'
		gT[4].externID == 'n2'
		gT[5].externID == 'nD'
	}

	def "Build solution For Invalid Nodes - with Blocks"() {
		def statusManager = Stub StatusManager
		def depot = new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()
		def nodes = [
				new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER, presetBlockIdx: 2).getNode(),
				new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER, presetBlockIdx: BlockNameConverter.DEFAULT_BLOCK_IDX).getNode(),
				new TestNode(externID: 'n3', siteType: SiteType.CUSTOMER, presetBlockIdx: 2).getNode()
		]
		def allNodes = [depot] + nodes
		def model = TestXFVRPModel.get(allNodes, testVehicle.getVehicle())

		when:
		def result = service.buildSolutionForInvalidNodes(nodes, depot, model, statusManager)
		def gT = Helper.get(result)

		then:
		result != null
		gT[0].externID == 'nD'
		gT[1].externID == 'n2'
		gT[2].externID == 'nD'
		gT[3].externID == 'nD'
		gT[4].externID == 'n1'
		gT[5].externID == 'n3'
		gT[6].externID == 'nD'
	}

	def "Build solution For Invalid Nodes - no unplanned"() {
		def statusManager = Stub StatusManager
		def depot = new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode()
		def model = TestXFVRPModel.get([depot], testVehicle.getVehicle())

		when:
		def result = service.buildSolutionForInvalidNodes([] as List<Node>, depot, model, statusManager)
		def gT = Helper.get(result)

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
		def vehicles = [testVehicle.getVehicle()] as Vehicle[]
		def types = []
		CompartmentInitializer.check(nodes.toArray(new Node[0]), types, vehicles)

		when:
		def result = service.insertUnplannedNodes(
				nodes,
				types.toArray(new CompartmentType[0]),
				metric,
				parameter,
				statusManager
		)
		def gT = Helper.get(result)

		then:
		result != null
		gT != null
		result.getModel().getVehicle().name.contains("INVALID")
		gT[0].externID == 'nD'
		gT[1].externID == 'n1'
		gT[2].externID == 'nD'
		gT[3].externID == 'nD'
		gT[4].externID == 'n2'
		gT[5].externID == 'nD'
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
		def vehicles = [testVehicle.getVehicle()] as Vehicle[]
		def types = []
		CompartmentInitializer.check(nodes.toArray(new Node[0]), types, vehicles)

		when:
		def result = service.insertUnplannedNodes(nodes, types.toArray(new CompartmentType[0]), metric, parameter, statusManager)

		then:
		result == null
	}

	def "Execute - no vehicles"() {
		def parameter = Stub XFVRPParameter
		def statusManager = Stub StatusManager
		def metric = Stub Metric
		metric.getDistanceAndTime(_, _, _) >> [1, 1]

		def nodes = [
				new TestNode(externID: 'nD', siteType: SiteType.DEPOT).getNode(),
				new TestNode(externID: 'n1', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n2', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n3', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n4', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n5', siteType: SiteType.CUSTOMER).getNode(),
				new TestNode(externID: 'n6', siteType: SiteType.CUSTOMER).getNode()
		] as Node[]

		def vehicles = [] as Vehicle[]
		def types = []
		CompartmentInitializer.check(nodes, types, vehicles)

		when:
		def result = service.execute(nodes, types.toArray(new CompartmentType[0]), vehicles,
				{routingDataBag ->
					new Solution(TestXFVRPModel.get(nodes, routingDataBag.vehicle))
				},
				metric, parameter, statusManager
		)

		then:
		result != null
		result.size() == 1
		result.get(0).model.getVehicle().name.contains('INVALID')
	}
}
