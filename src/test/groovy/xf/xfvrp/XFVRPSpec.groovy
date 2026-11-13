package xf.xfvrp

import spock.lang.Specification
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.exception.XFVRPException
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptTypes
import xf.xfvrp.report.Event

class XFVRPSpec extends Specification {

	def "CVRP with savings construction - all customers are served"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0)
		customers().each { d ->
			xfvrp.addCustomer().setExternID(d).setXlong(1).setYlat(1).setDemand(1f)
		}
		xfvrp.addVehicle().setCapacity([4] as float[])
		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.SAVINGS)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()
		servedCustomerIDs(report) == ["C1", "C2", "C3", "C4"] as Set
		report.getSummary().getDistance() > 0
		report.getSummary().getOverload() == 0f
	}

	def "CVRP with first-best construction and relocate/ILS improvement - all customers are served"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0)
		customers().each { d ->
			xfvrp.addCustomer().setExternID(d).setXlong(1).setYlat(1).setDemand(1f)
		}
		xfvrp.addVehicle().setCapacity([4] as float[])
		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.FIRST_BEST)
		xfvrp.addOptType(XFVRPOptTypes.RELOCATE)
		xfvrp.addOptType(XFVRPOptTypes.ILS)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()
		servedCustomerIDs(report) == ["C1", "C2", "C3", "C4"] as Set
		report.getSummary().getDistance() > 0
		report.getSummary().getOverload() == 0f
	}

	def "Compartment capacity is respected"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0)
		xfvrp.addCompartment(CompartmentType.DELIVERY)
		["C1", "C2", "C3"].each { id ->
			xfvrp.addCustomer().setExternID(id).setXlong(1).setYlat(1).setDemand([1f] as float[])
		}
		xfvrp.addVehicle().setCapacity([2] as float[])
		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.FIRST_BEST)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		servedCustomerIDs(report) == ["C1", "C2", "C3"] as Set
		report.getSummary().getOverload() == 0f
	}

	def "Execution without depot is rejected"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addCustomer().setExternID("C1").setXlong(1).setYlat(1).setDemand(1f)
		xfvrp.addVehicle().setCapacity([1] as float[])
		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.SAVINGS)

		when:
		xfvrp.executeRoutePlanning()

		then:
		thrown(XFVRPException)
	}

	def "Report before any planning is null"() {
		expect:
		new XFVRP().getReport() == null
	}

	def customers() {
		return ["C1", "C2", "C3", "C4"]
	}

	def servedCustomerIDs(report) {
		return report.getRoutes()
				.findAll { it.getVehicle() != null }
				.collectMany { it.getEvents() }
				.findAll { it.getSiteType() == SiteType.CUSTOMER }
				.collect { it.getID() }
				.toSet()
	}
}
