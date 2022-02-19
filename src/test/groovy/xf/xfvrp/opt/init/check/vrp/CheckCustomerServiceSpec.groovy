package xf.xfvrp.opt.init.check.vrp

import spock.lang.Specification
import util.instances.TestNode
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.base.InvalidReason
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.opt.init.solution.vrp.SolutionBuilderDataBag

class CheckCustomerServiceSpec extends Specification {

	def modelStub = Stub XFVRPModel
	def service = new CheckCustomerService()

	def "Check demands - Okay"() {
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				demand: [1, 2, 3] as float[]
				).getNode()

		def v = new TestVehicle(capacity: [3, 3, 3] as float[]).getVehicle()
		def model = TestXFVRPModel.get([], v)

		when:
		def result = service.checkDemands(customer, model)
		then:
		result
		customer.getInvalidReason() == InvalidReason.NONE
	}

	def "Check demands - Less capacity"() {
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				demand: [1, 2, 3] as float[]
				).getNode()

		def v = new TestVehicle(capacity: [3, 1, 3] as float[]).getVehicle()
		def model = TestXFVRPModel.get([], v)

		when:
		def result = service.checkDemands(customer, model)
		then:
		!result
		customer.getInvalidReason() == InvalidReason.CAPACITY
	}

	def "Check demands - Different number of capacities"() {
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				demand: [1, 2, 3] as float[]
				).getNode()

		def v = new TestVehicle(capacity: [3, 3] as float[]).getVehicle()
		def model = TestXFVRPModel.get([], v)

		when:
		def result = service.checkDemands(customer, model)
		then:
		result
		customer.getInvalidReason() == InvalidReason.NONE
	}

	def "Check time windows - Okay"() {
		def depot = new TestNode(
				externID: "DEP",
				siteType: SiteType.DEPOT,
				timeWindow: [[0, 20]] as float[][]
				).getNode()
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				timeWindow: [[10, 20], [30, 40]] as float[][],
				serviceTime: 5
				).getNode()

		def v = new TestVehicle(maxRouteDuration: 20).getVehicle()
		modelStub.getTime(_, _) >> 5
		modelStub.getVehicle() >> v
		modelStub.getNodes() >> [depot]
		modelStub.getNbrOfDepots() >> 1

		when:
		def result = service.checkTimeWindows(customer, modelStub)
		then:
		result
		customer.getInvalidReason() == InvalidReason.NONE
	}

	def "Check time windows - Miss of route duration"() {
		def depot = new TestNode(
				externID: "DEP",
				siteType: SiteType.DEPOT,
				timeWindow: [[0, 20]] as float[][]
				).getNode()
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				timeWindow: [[10, 20], [30, 40]] as float[][],
				serviceTime: 5
				).getNode()

		def v = new TestVehicle(maxRouteDuration: 10).getVehicle()
		modelStub.getTime(_, _) >> 5
		modelStub.getVehicle() >> v
		modelStub.getNodes() >> [depot]
		modelStub.getNbrOfDepots() >> 1

		when:
		def result = service.checkTimeWindows(customer, modelStub)
		then:
		!result
		customer.getInvalidReason() == InvalidReason.TRAVEL_TIME
	}

	def "Check time windows - Miss of customer close time"() {
		def depot = new TestNode(
				externID: "DEP",
				siteType: SiteType.DEPOT,
				timeWindow: [[0, 30]] as float[][]
				).getNode()
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				timeWindow: [[2, 4], [5, 8]] as float[][],
				serviceTime: 5
				).getNode()

		def v = new TestVehicle(maxRouteDuration: 25).getVehicle()
		modelStub.getTime(_, _) >> 9
		modelStub.getVehicle() >> v
		modelStub.getNodes() >> [depot]
		modelStub.getNbrOfDepots() >> 1

		when:
		def result = service.checkTimeWindows(customer, modelStub)
		then:
		!result
		customer.getInvalidReason() == InvalidReason.TIME_WINDOW
	}

	def "Check time windows - Miss of depot close time"() {
		def depot = new TestNode(
				externID: "DEP",
				siteType: SiteType.DEPOT,
				timeWindow: [[0, 15]] as float[][]
				).getNode()
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				timeWindow: [[10, 20], [30, 40]] as float[][],
				serviceTime: 5
				).getNode()

		def v = new TestVehicle(maxRouteDuration: 20).getVehicle()
		modelStub.getTime(_, _) >> 5
		modelStub.getVehicle() >> v
		modelStub.getNodes() >> [depot]
		modelStub.getNbrOfDepots() >> 1

		when:
		def result = service.checkTimeWindows(customer, modelStub)
		then:
		!result
		customer.getInvalidReason() == InvalidReason.TIME_WINDOW
	}

	def "Check time windows - Multi depots and miss at first"() {
		def depot = new TestNode(
				externID: "DEP",
				siteType: SiteType.DEPOT,
				timeWindow: [[0, 15]] as float[][]
				).getNode()
		def depot2 = new TestNode(
				externID: "DEP2",
				siteType: SiteType.DEPOT,
				timeWindow: [[0, 30]] as float[][]
				).getNode()

		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				timeWindow: [[10, 20], [30, 40]] as float[][],
				serviceTime: 5
				).getNode()

		def v = new TestVehicle(maxRouteDuration: 20).getVehicle()
		modelStub.getTime(_, _) >> 5
		modelStub.getVehicle() >> v
		modelStub.getNodes() >> [depot, depot2]
		modelStub.getNbrOfDepots() >> 2

		when:
		def result = service.checkTimeWindows(customer, modelStub)
		then:
		result
		customer.getInvalidReason() == InvalidReason.NONE
	}

	def "Check customer - Okay"() {
		def depot = new TestNode(
				externID: "DEP",
				siteType: SiteType.DEPOT,
				timeWindow: [[0, 20]] as float[][]
				).getNode()
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				timeWindow: [[10, 20], [30, 40]] as float[][],
				serviceTime: 5,
				demand: [1, 2, 3] as float[],
				presetBlockIdx: 1,
				presetBlockRank: 2,
				presetBlockPos: 2

				).getNode()

		def dataBag = new SolutionBuilderDataBag()
		dataBag.knownSequencePositions = [1] as Set<Integer>

		def v = new TestVehicle(maxRouteDuration: 20, capacity: [3, 3, 3] as float[]).getVehicle()
		modelStub.getTime(_, _) >> 5
		modelStub.getVehicle() >> v
		modelStub.getNodes() >> [depot]
		modelStub.getNbrOfDepots() >> 1

		when:
		def result = service.checkCustomer(customer, modelStub, dataBag)
		then:
		result
		customer.getInvalidReason() == InvalidReason.NONE
	}
	
	def "Check customer - Time window missed"() {
		def depot = new TestNode(
				externID: "DEP",
				siteType: SiteType.DEPOT,
				timeWindow: [[0, 15]] as float[][]
				).getNode()
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				timeWindow: [[10, 20], [30, 40]] as float[][],
				serviceTime: 5,
				demand: [1, 2, 3] as float[],
				presetBlockIdx: 1,
				presetBlockRank: 2,
				presetBlockPos: 2

				).getNode()

		def dataBag = new SolutionBuilderDataBag()
		dataBag.knownSequencePositions = [1] as Set<Integer>

		def v = new TestVehicle(maxRouteDuration: 20, capacity: [3, 3, 3] as float[]).getVehicle()
		modelStub.getTime(_, _) >> 5
		modelStub.getVehicle() >> v
		modelStub.getNodes() >> [depot]
		modelStub.getNbrOfDepots() >> 1

		when:
		def result = service.checkCustomer(customer, modelStub, dataBag)
		then:
		!result
		customer.getInvalidReason() == InvalidReason.TIME_WINDOW
	}
	
	def "Check customer - Capacity missed"() {
		def depot = new TestNode(
				externID: "DEP",
				siteType: SiteType.DEPOT,
				timeWindow: [[0, 20]] as float[][]
				).getNode()
		def customer = new TestNode(
				externID: "1",
				siteType: SiteType.CUSTOMER,
				timeWindow: [[10, 20], [30, 40]] as float[][],
				serviceTime: 5,
				demand: [1, 2, 3] as float[],
				presetBlockIdx: 1,
				presetBlockRank: 2,
				presetBlockPos: 2

				).getNode()

		def dataBag = new SolutionBuilderDataBag()
		dataBag.knownSequencePositions = [1] as Set<Integer>

		def v = new TestVehicle(maxRouteDuration: 20, capacity: [3, 1, 3] as float[]).getVehicle()
		modelStub.getTime(_, _) >> 5
		modelStub.getVehicle() >> v
		modelStub.getNodes() >> [depot]
		modelStub.getNbrOfDepots() >> 1

		when:
		def result = service.checkCustomer(customer, modelStub, dataBag)
		then:
		!result
		customer.getInvalidReason() == InvalidReason.CAPACITY
	}
}
