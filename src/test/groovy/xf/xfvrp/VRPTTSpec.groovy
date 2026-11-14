package xf.xfvrp

import spock.lang.Specification
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.metric.DirectMatrixMetric
import xf.xfvrp.opt.XFVRPOptTypes

class VRPTTSpec extends Specification {

	def "Trailer customer is served via transshipment location and TL appears in report"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0)
		xfvrp.addTransshipment().setExternID("T1").setFixedTransferTime(5)
		xfvrp.addCustomer().setExternID("C1").setDemand(1f)
		xfvrp.addVehicle().setName("TRAILER1").setAutonomous(false).setCapacity([5] as float[])
		xfvrp.addVehicle().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10] as float[])
		// Going via T1 (1 + 1 + 10) is cheaper than going directly to C1 (10 + 10)
		xfvrp.setMetric(metric(3, [
				(0): [(1): 1f, (2): 10f],
				(1): [(0): 1f, (2): 1f],
				(2): [(0): 10f, (1): 1f]
		]))
		xfvrp.addOptType(XFVRPOptTypes.VRPTT)
		xfvrp.addOptType(XFVRPOptTypes.VRPTT_ILS)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()
		servedCustomerIDs(report) == ["C1"] as Set
		report.getSummary().getOverload() == 0f

		// TL appears in report. The Phase 2 mode change can create transshipment
		// records, so drop and take visits of the same TL may appear additionally.
		def tlEvents = allEvents(report).findAll { it.getSiteType() == SiteType.TRANSSHIPMENT }
		tlEvents.size() >= 1
		tlEvents.every { it.getID() == "T1" }
	}

	def "Temporal synchronization is respected at the transshipment location"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0)
		xfvrp.addTransshipment().setExternID("T1").setFixedTransferTime(5).setTimeWindow(0, 50)
		xfvrp.addCustomer().setExternID("C1").setDemand(1f)
		xfvrp.addVehicle().setName("TRAILER1").setAutonomous(false).setCapacity([5] as float[])
		xfvrp.addVehicle().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10] as float[])
		xfvrp.setMetric(metric(3, [
				(0): [(1): 1f, (2): 10f],
				(1): [(0): 1f, (2): 1f],
				(2): [(0): 10f, (1): 1f]
		]))
		xfvrp.addOptType(XFVRPOptTypes.VRPTT)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()

		def tlEvent = allEvents(report).find { it.getSiteType() == SiteType.TRANSSHIPMENT }
		tlEvent != null
		tlEvent.getID() == "T1"

		// Arrival within the time window of the transshipment location
		tlEvent.getArrival() >= 0f
		tlEvent.getArrival() <= 50f

		// The transfer duration is consumed by the transferring vehicle
		tlEvent.getService() == 5f
		tlEvent.getDeparture() == tlEvent.getArrival() + 5f
	}

	def "Lorry customers are never visited by a trailer capable route"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0)
		xfvrp.addTransshipment().setExternID("T1").setXlong(10).setYlat(0)
		xfvrp.addCustomer().setExternID("TC1").setXlong(5).setYlat(5).setDemand(1f)
		xfvrp.addCustomer().setExternID("LC1").setXlong(5).setYlat(-5).setDemand(1f).setTrailerAccessible(false)
		xfvrp.addCustomer().setExternID("LC2").setXlong(6).setYlat(-6).setDemand(1f).setTrailerAccessible(false)
		xfvrp.addVehicle().setName("TRAILER1").setAutonomous(false).setCapacity([5] as float[])
		xfvrp.addVehicle().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10] as float[])
		xfvrp.addVehicle().setName("LORRY2").setCapacity([5] as float[])
		xfvrp.setMetric(new xf.xfvrp.base.metric.EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.VRPTT)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()
		servedCustomerIDs(report) == ["TC1", "LC1", "LC2"] as Set

		// The trailer capable lorry serves no lorry customer
		def trailerCapableEvents = eventsOf(report, "LORRY1")
		trailerCapableEvents.findAll { it.getSiteType() == SiteType.CUSTOMER }.collect { it.getID() }.toSet() == ["TC1"] as Set

		// The lorry customers are served by the pure lorry
		def pureLorryCustomerIDs = eventsOf(report, "LORRY2").findAll { it.getSiteType() == SiteType.CUSTOMER }.collect { it.getID() }
		pureLorryCustomerIDs.toSet() == ["LC1", "LC2"] as Set
	}

	def "Trailer capable lorry is skipped, if no unplanned customer is allowed for it"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0)
		xfvrp.addTransshipment().setExternID("T1").setXlong(10).setYlat(0)
		xfvrp.addCustomer().setExternID("TC1").setXlong(5).setYlat(5).setDemand(1f)
		xfvrp.addCustomer().setExternID("LC1").setXlong(5).setYlat(-5).setDemand(1f).setTrailerAccessible(false)
		// Two trailer capable lorries with a large capacity and a pure lorry.
		// The first lorry serves all trailer customers, so that only lorry
		// customers are unplanned for the second trailer capable lorry.
		xfvrp.addVehicle().setName("TRAILER1").setAutonomous(false).setCapacity([5] as float[])
		xfvrp.addVehicle().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([100] as float[])
		xfvrp.addVehicle().setName("LORRY2").setTrailerId("TRAILER1").setCapacity([100] as float[])
		xfvrp.addVehicle().setName("LORRY3").setCapacity([50] as float[])
		xfvrp.setMetric(new xf.xfvrp.base.metric.EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.VRPTT)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()
		servedCustomerIDs(report) == ["TC1", "LC1"] as Set
	}

	//////////////////////////////////////////

	private Set<String> servedCustomerIDs(report) {
		return allEvents(report)
				.findAll { it.getSiteType() == SiteType.CUSTOMER }
				.collect { it.getID() }
				.toSet()
	}

	private def allEvents(report) {
		return report.getRoutes().collectMany { it.getEvents() }
	}

	private def eventsOf(report, String vehicleName) {
		return report.getRoutes()
				.findAll { it.getVehicle().getName() == vehicleName }
				.collectMany { it.getEvents() }
	}

	/**
	 * Builds a symmetric matrix metric with default distances of 10, where the
	 * given distances are overridden (time = distance).
	 */
	private xf.xfvrp.base.metric.DirectMatrixMetric metric(int size, Map<Integer, Map<Integer, Float>> distances) {
		def m = new xf.xfvrp.base.metric.DirectMatrixMetric(size, 1)
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				float d = (i == j) ? 0f : 10f
				if (distances[i]?.containsKey(j))
					d = distances[i][j]
				m.add(i, j, d, d)
			}
		}
		return m
	}
}
