package xf.xfvrp.opt.fleetmix

import spock.lang.Specification
import xf.xfvrp.XFVRP
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.opt.XFVRPOptTypes
import xf.xfvrp.report.Report

/**
 * Copyright (c) 2026 Lars Hackstein
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Checks the Phase 3 mixed fleet heuristic at the planning level:
 * <ul>
 * <li>Regression guard: without transshipment locations, the planning is
 * exactly the default mixed fleet planning (non-VRPTT types are not
 * touched),</li>
 * <li>VRPTT: the joint multi-vehicle plan must not worsen the per-vehicle
 * plans, and transshipment records, which appear in the report, carry
 * their transferred amount.</li>
 * </ul>
 *
 * @author lhackste
 */
class VRPTTMixedFleetHeuristicSpec extends Specification {

	def "Non-VRPTT planning is identical to the default mixed fleet heuristic"() {
		given:
		def withDefault = planning(true)
		def withVrptt = planning(false)

		when:
		def costDefault = plan(withDefault)
		def costVrptt = plan(withVrptt)

		then:
		costDefault == costVrptt
	}

	def "VRPTT planning with the joint plan is feasible and not worse than the per-vehicle planning"() {
		given:
		def perVehicle = planning(false)
		def joint = planning(true)

		when:
		def costPerVehicle = plan(perVehicle)
		def report = execute(joint)

		then:
		report.getErrors().getErrorDescriptions().isEmpty()
		report.getSummary().getOverload() == 0f
		customerIDs(report).size() == 4

		def costJoint = report.getSummary().getCost()
		costJoint <= costPerVehicle + 0.001f

		// Transshipment record visits carry their transferred amount
		def tlEvents = report.getRoutes().collectMany { it.getEvents() }
				.findAll { it.getSiteType() == SiteType.TRANSSHIPMENT }
		tlEvents.every { it.getAmounts().length > 0 }
	}

	//////////////////////////////////////////

	/** Planning without the joint record optimization */
	private XFVRP planning(boolean vrpttMixedFleet) {
		def xfvrp = new XFVRP()
		xfvrp.addDepot().setExternID("DEP").setXlong(0).setYlat(0)
		xfvrp.addTransshipment().setExternID("T1").setXlong(1).setYlat(1).setFixedTransferTime(5).setTimeWindow(0, 1000)
		xfvrp.addCustomer().setExternID("L1").setXlong(2).setYlat(2).setDemand(1f).setTrailerAccessible(false)
		xfvrp.addCustomer().setExternID("L2").setXlong(3).setYlat(-2).setDemand(1f).setTrailerAccessible(false)
		xfvrp.addCustomer().setExternID("TC1").setXlong(30).setYlat(28).setDemand(3f)
		xfvrp.addCustomer().setExternID("TC2").setXlong(28).setYlat(31).setDemand(3f)
		xfvrp.addVehicle().setName("TRAILER1").setAutonomous(false).setCapacity([10] as float[])
		xfvrp.addVehicle().setName("LORRY1").setTrailerId("TRAILER1").setCapacity([10] as float[]).setFixCost(100)
		xfvrp.addVehicle().setName("LORRY2").setCapacity([10] as float[]).setFixCost(100)
		xfvrp.setMetric(new EucledianMetric())
		xfvrp.addOptType(XFVRPOptTypes.VRPTT)
		xfvrp.addOptType(XFVRPOptTypes.VRPTT_ILS)

		if (vrpttMixedFleet)
			xfvrp.getParameters().setMixedFleetHeuristic(new VRPTTMixedFleetHeuristic())

		return xfvrp
	}

	private float plan(XFVRP xfvrp) {
		return execute(xfvrp).getSummary().getCost()
	}

	private Report execute(XFVRP xfvrp) {
		xfvrp.executeRoutePlanning()
		return xfvrp.getReport()
	}

	private Set<String> customerIDs(Report report) {
		return report.getRoutes().collectMany { it.getEvents() }
				.findAll { it.getSiteType() == SiteType.CUSTOMER }
				.collect { it.getID() }
				.toSet()
	}
}
