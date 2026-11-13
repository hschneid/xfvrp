package xf.xfvrp

import spock.lang.PendingFeature
import spock.lang.Specification
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptTypes

/**
 * End-to-end tests which prove (or disprove) that the optimizer actually
 * USES REPLENISH sites in its solutions, i.e. builds routes which pass
 * a replenishment node to reset vehicle capacity.
 *
 * Scenario:
 *
 *   DEP(0,0)                    C1(14,0)   C2(18,0)   C3(22,0)
 *      \---- REP(10,5) ---------/          |          |
 *        (detour of ~3 distance units)
 *
 * Customers C1..C3 have demand 4 each (total 12), vehicle capacity is 10,
 * so a single feasible route serving all customers MUST visit the
 * replenish node to reset the capacity.
 *
 * Findings (2026-11-12):
 * - FIRST_BEST uses the replenish site (both with and without ILS/RELOCATE):
 *   it produces the single route DEP -> C2 -> C3 -> REP -> C1 -> DEP
 *   (distance ~55.4) instead of an infeasible overloaded route or
 *   a second route. Mechanism: NormalizeSolutionService.addReplenishRoute
 *   creates a "parking" route [DEP, REP, DEP]; the first-best insertion
 *   heuristic evaluates all routes as insertion targets, so customers can
 *   be inserted next to the replenish node, which then becomes part of a
 *   customer-carrying route.
 * - SAVINGS never uses the replenish site: XFVRPSavings.buildRoutes
 *   strips all non-CUSTOMER nodes from every route (see
 *   XFVRPSavings.java, buildRoutes -> filter SiteType.CUSTOMER) and
 *   createSolution only reassembles customer nodes. See the
 *   @PendingFeature test below.
 */
class ReplenishUsageSpec extends Specification {

	def "Replenishment is required and used - opt chain: #optTypes"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0).setMaxNbrRoutes(1)
		xfvrp.addReplenishment().setExternID("REP").setXlong(10).setYlat(5)
		addCustomer(xfvrp, "C1", 14)
		addCustomer(xfvrp, "C2", 18)
		addCustomer(xfvrp, "C3", 22)
		xfvrp.addVehicle().setCapacity([10] as float[])
		xfvrp.setMetric(new EucledianMetric())
		optTypes.each { xfvrp.addOptType(it) }

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()
		servedCustomerIDs(report) == ["C1", "C2", "C3"] as Set
		report.getSummary().getOverload() == 0f
		// The core assertion: a replenish site is part of the actual route plan
		replenishEventIDs(report) == ["REP"] as Set

		where:
		optTypes                                                          | _
		[XFVRPOptTypes.FIRST_BEST]                                        | _
		[XFVRPOptTypes.FIRST_BEST, XFVRPOptTypes.ILS]                     | _
		[XFVRPOptTypes.FIRST_BEST, XFVRPOptTypes.RELOCATE, XFVRPOptTypes.ILS] | _
	}

	@PendingFeature
	def "SAVINGS - replenishment is required and used (currently NOT fulfilled)"() {
		// Documented product finding: the SAVINGS construction heuristic can
		// never use replenishment sites. XFVRPSavings.buildRoutes() filters
		// every route down to SiteType.CUSTOMER nodes (dropping replenish
		// nodes) and createSolution rebuilds routes from customers only.
		// Savings then merges customer routes only while the merged route is
		// feasible WITHOUT a capacity reset, so total demand 12 > capacity 10
		// results in two routes (DEP->C3->C2->DEP, DEP->C1->DEP, distance 72)
		// and the replenish node is never visited.
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0).setMaxNbrRoutes(1)
		xfvrp.addReplenishment().setExternID("REP").setXlong(10).setYlat(5)
		addCustomer(xfvrp, "C1", 14)
		addCustomer(xfvrp, "C2", 18)
		addCustomer(xfvrp, "C3", 22)
		xfvrp.addVehicle().setCapacity([10] as float[])
		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.SAVINGS)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()
		servedCustomerIDs(report) == ["C1", "C2", "C3"] as Set
		report.getSummary().getOverload() == 0f
		replenishEventIDs(report) == ["REP"] as Set
	}

	def "Replenishment is available but not needed - optimizer does not detour to replenish node"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0).setMaxNbrRoutes(1)
		xfvrp.addReplenishment().setExternID("REP").setXlong(10).setYlat(5)
		addCustomer(xfvrp, "C1", 14, 1f)
		addCustomer(xfvrp, "C2", 18, 1f)
		addCustomer(xfvrp, "C3", 22, 1f)
		xfvrp.addVehicle().setCapacity([10] as float[])
		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.SAVINGS)
		xfvrp.addOptType(XFVRPOptTypes.ILS)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()
		servedCustomerIDs(report) == ["C1", "C2", "C3"] as Set
		report.getSummary().getOverload() == 0f
		// No needless detour: no replenish event in any route
		replenishEventIDs(report).isEmpty()
		// Sanity bound: direct route DEP -> C1 -> C2 -> C3 -> DEP is ~44,
		// a detour to REP(10,5) would add ~3.1 or more
		report.getSummary().getDistance() < 46f
	}

	def "Replenish node with time window - usage respects the time window"() {
		given:
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0).setMaxNbrRoutes(1)
		xfvrp.addReplenishment().setExternID("REP").setXlong(10).setYlat(5).setTimeWindow(0, 50)
		addCustomer(xfvrp, "C1", 14)
		addCustomer(xfvrp, "C2", 18)
		addCustomer(xfvrp, "C3", 22)
		xfvrp.addVehicle().setCapacity([10] as float[])
		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.FIRST_BEST)
		xfvrp.addOptType(XFVRPOptTypes.ILS)

		when:
		xfvrp.executeRoutePlanning()
		def report = xfvrp.getReport()

		then:
		report != null
		report.getErrors().getErrorDescriptions().isEmpty()
		servedCustomerIDs(report) == ["C1", "C2", "C3"] as Set
		report.getSummary().getOverload() == 0f
		// Replenish is used (single feasible route required) and its time
		// window is respected
		replenishEventIDs(report) == ["REP"] as Set
		report.getRoutes()
				.collectMany { it.getEvents() }
				.findAll { it.getSiteType() == SiteType.REPLENISH }
				.every { it.getArrival() <= 50f }
	}

	def addCustomer(XFVRP xfvrp, String id, float x, float demand = 4f) {
		xfvrp.addCustomer()
				.setExternID(id)
				.setXlong(x)
				.setYlat(0)
				.setDemand(demand)
	}

	def servedCustomerIDs(report) {
		report.getRoutes()
				.findAll { it.getVehicle() != null }
				.collectMany { it.getEvents() }
				.findAll { it.getSiteType() == SiteType.CUSTOMER }
				.collect { it.getID() }
				.toSet()
	}

	def replenishEventIDs(report) {
		report.getRoutes()
				.findAll { it.getVehicle() != null }
				.collectMany { it.getEvents() }
				.findAll { it.getSiteType() == SiteType.REPLENISH }
				.collect { it.getID() }
				.toSet()
	}
}
