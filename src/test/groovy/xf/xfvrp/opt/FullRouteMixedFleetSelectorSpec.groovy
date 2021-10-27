package xf.xfvrp.opt

import spock.lang.Specification
import util.instances.TestVehicle
import util.instances.TestXFVRPModel
import xf.xfvrp.opt.fleetmix.MixedFleetSelector
import xf.xfvrp.report.Report
import xf.xfvrp.report.RouteReport

class FullRouteMixedFleetSelectorSpec extends Specification {

	def service = new MixedFleetSelector()

	def testVehicle
	def routeReport


	def setup() {
		testVehicle = new TestVehicle(
				fixCost: 11,
				varCost: 5
				)
		def model = TestXFVRPModel.get([], testVehicle.getVehicle())
		routeReport = new RouteReport(testVehicle.getVehicle())
		routeReport.getSummary().duration = 1234
		routeReport.getSummary().pickups[0] = 555
		routeReport.getSummary().deliveries[0] = 666
		routeReport.getSummary().delay = 0
	}

	def "Get quality - normal"() {
		when:
		def result = service.getQuality(routeReport)

		then:
		result != null
		result.route == routeReport
		Math.abs(result.quality - ((11 + (5 * 1234)) / (555 + 666))) < 0.001
	}

	def "Get quality - with delay"() {
		routeReport.getSummary().delay = 1
		when:
		def result = service.getQuality(routeReport)

		then:
		result != null
		result.route == routeReport
		result.quality == Float.MAX_VALUE
	}
	
	def "Get quality - empty route"() {
		routeReport.getSummary().duration = 0
		routeReport.getSummary().pickups[0] = 0
		routeReport.getSummary().deliveries[0] = 0
		routeReport.getSummary().delay = 0
		
		when:
		def result = service.getQuality(routeReport)

		then:
		result != null
		result.route == routeReport
		result.quality == 0
	}
	
	def "Get best routes - normal"() {
		testVehicle.nbrOfAvailableVehicles = 2
		
		def solution = Stub Solution
		solution.getGiantRoute() >> []

		def model = TestXFVRPModel.get([], testVehicle.getVehicle())
		def report = new Report(solution, model)
		def routeReport1 = new RouteReport(testVehicle.getVehicle())
		routeReport1.getSummary().duration = 1234
		routeReport1.getSummary().pickups[0] = 555
		routeReport1.getSummary().deliveries[0] = 666
		routeReport1.getSummary().delay = 0
		routeReport1.getSummary().nbrOfEvents = 1
		
		def routeReport2 = new RouteReport(testVehicle.getVehicle())
		routeReport2.getSummary().duration = 1000
		routeReport2.getSummary().pickups[0] = 555
		routeReport2.getSummary().deliveries[0] = 666
		routeReport2.getSummary().delay = 0
		routeReport2.getSummary().nbrOfEvents = 1
		
		def routeReport3 = new RouteReport(testVehicle.getVehicle())
		routeReport3.getSummary().duration = 800
		routeReport3.getSummary().pickups[0] = 555
		routeReport3.getSummary().deliveries[0] = 666
		routeReport3.getSummary().delay = 0
		routeReport3.getSummary().nbrOfEvents = 1
		
		def routeReport4 = new RouteReport(testVehicle.getVehicle())
		routeReport4.getSummary().duration = 800
		routeReport4.getSummary().pickups[0] = 555
		routeReport4.getSummary().deliveries[0] = 666
		routeReport4.getSummary().delay = 1
		routeReport4.getSummary().nbrOfEvents = 1
		
		report.add(routeReport1)
		report.add(routeReport2)
		report.add(routeReport3)
		report.add(routeReport4)
		
		when:
		def result = service.getBestRoutes(testVehicle.getVehicle(), report)
		
		then:
		result != null
		result.size() == 2
		result.contains(routeReport2)
		result.contains(routeReport3)
	}
	
	def "Get best routes - empty report"() {
		testVehicle.nbrOfAvailableVehicles = 2
		
		def solution = Stub Solution
		solution.getGiantRoute() >> []
		
		def report = new Report(solution, null)
		
		when:
		def result = service.getBestRoutes(testVehicle.getVehicle(), report)
		
		then:
		result != null
		result.size() == 0
	}

}
