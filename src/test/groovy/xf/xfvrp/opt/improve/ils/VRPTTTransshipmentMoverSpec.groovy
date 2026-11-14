package xf.xfvrp.opt.improve.ils

import spock.lang.Specification
import xf.xfvrp.base.LoadType
import xf.xfvrp.base.Node
import xf.xfvrp.base.SiteType
import xf.xfvrp.base.Util
import xf.xfvrp.base.Vehicle
import xf.xfvrp.base.XFVRPModel
import xf.xfvrp.base.XFVRPParameter
import xf.xfvrp.base.compartment.CompartmentType
import xf.xfvrp.base.metric.EucledianMetric
import xf.xfvrp.base.monitor.StatusManager
import xf.xfvrp.opt.Solution
import xf.xfvrp.opt.evaluation.EvaluationService
import xf.xfvrp.opt.init.ModelBuilder

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Checks the Phase 2 improvement neighborhoods of the VRPTT: mode change
 * (DIRECT to VIA_TDP by adding a transshipment record), its reversal
 * (removing a record) and the re-assignment of a record to another
 * transshipment location. All moves have to stay synchronization-safe.
 *
 * @author lhackste
 */
class VRPTTTransshipmentMoverSpec extends Specification {

	/** Model indices of the transshipment locations */
	private static final int TL1 = 3, TL2 = 4, TL_FAR = 5

	def "Adding a record creates a drop and a take visit"() {
		given:
		def solution = twoRouteSolution(2)

		when:
		def added = mover().addRecord(solution, 0, 1, TL1, 1, 1)

		then:
		added

		def supplier = solution.getRoutes()[0]
		supplier[2].getSiteType() == SiteType.TRANSSHIPMENT
		supplier[2].getDemand()[0] == -5f
		supplier[2].getGlobalIdx() == TL1

		def taker = solution.getRoutes()[1]
		taker[1].getSiteType() == SiteType.TRANSSHIPMENT
		taker[1].getDemand()[0] == 5f
		taker[1].getGlobalIdx() == TL1
	}

	def "Added record is synchronization-safe"() {
		given:
		def solution = twoRouteSolution(2)

		when:
		mover().addRecord(solution, 0, 1, TL1, 1, 1)
		def q = new EvaluationService().check(solution)

		then:
		q.getPenalty() == 0
	}

	def "Record, which overloads the taker, is rejected"() {
		given:
		// The taker route serves a customer with demand 8. Taking another 5
		// exceeds the vehicle capacity of 10.
		def solution = twoRouteSolution(8)

		when:
		def added = mover().addRecord(solution, 0, 1, TL1, 1, 1)

		then:
		!added

		// The routes are unchanged
		solution.getRoutes()[0].length == 4
		solution.getRoutes()[1].length == 3
	}

	def "Removing a record restores the mode change to direct service"() {
		given:
		def solution = twoRouteSolution(2)
		mover().addRecord(solution, 0, 1, TL1, 1, 1)

		when:
		def removed = mover().removeRecord(solution, 0, 2)

		then:
		removed

		solution.getRoutes()[0].length == 4
		solution.getRoutes()[1].length == 3
		new EvaluationService().check(solution).getPenalty() == 0
	}

	def "Re-assigning a record moves drop and take to another TL"() {
		given:
		def solution = twoRouteSolution(2)
		mover().addRecord(solution, 0, 1, TL1, 1, 1)

		when:
		def reassigned = mover().reassignRecord(solution, 0, 2, TL2)

		then:
		reassigned

		solution.getRoutes()[0][2].getGlobalIdx() == TL2
		solution.getRoutes()[0][2].getDemand()[0] == -5f
		solution.getRoutes()[1][1].getGlobalIdx() == TL2
		solution.getRoutes()[1][1].getDemand()[0] == 5f
		new EvaluationService().check(solution).getPenalty() == 0
	}

	def "Re-assignment is reverted, if it violates the synchronization"() {
		given:
		def solution = twoRouteSolution(2)
		mover().addRecord(solution, 0, 1, TL1, 1, 1)

		when:
		// The far TL can only be reached long after its time window is closed
		def reassigned = mover().reassignRecord(solution, 0, 2, TL_FAR)

		then:
		!reassigned
		solution.getRoutes()[0][2].getGlobalIdx() == TL1
	}

	//////////////////////////////////////////

	/** Vehicle capacity of the fleet */
	private static final float CAPACITY = 10f

	/** Demand of the transshipped customer */
	private static final float DEMAND = 5f

	private XFVRPModel model
	private Node[] modelNodes

	/**
	 * Creates a solution with two routes: route 0 serves a customer with
	 * {@code DEMAND} (the potential transshipment customer), route 1 serves
	 * another customer with the given demand.
	 */
	private Solution twoRouteSolution(float otherDemand) {
		model = model(otherDemand)

		def solution = new Solution(model)
		solution.addRoute([
				Util.createIdNode(modelNodes[0], 0),
				Util.createIdNode(modelNodes[1], 1),
				Util.createIdNode(modelNodes[6], 2),
				Util.createIdNode(modelNodes[0], 3)
		] as Node[])
		solution.addRoute([
				Util.createIdNode(modelNodes[0], 3),
				Util.createIdNode(modelNodes[2], 4),
				Util.createIdNode(modelNodes[0], 5)
		] as Node[])
		return solution
	}

	private VRPTTTransshipmentMover mover() {
		def m = new VRPTTTransshipmentMover()
		m.setModel(model)
		return m
	}

	private XFVRPModel model(float otherDemand) {
		def depot = node(0, "DEP", SiteType.DEPOT, 0, 0, Integer.MAX_VALUE, 0, LoadType.UNDEF)

		def transshipCustomer = node(1, "C1", SiteType.CUSTOMER, 10, 0, 200, DEMAND, LoadType.PICKUP)
		def secondCustomer = node(6, "C3", SiteType.CUSTOMER, 11, 1, 200, 1f, LoadType.PICKUP)
		def otherCustomer = node(2, "C2", SiteType.CUSTOMER, 90, 0, 200, otherDemand, LoadType.PICKUP)

		def tl1 = node(TL1, "TL1", SiteType.TRANSSHIPMENT, 10, 10, 1000, 0, LoadType.UNDEF)
		def tl2 = node(TL2, "TL2", SiteType.TRANSSHIPMENT, 50, 50, 1000, 0, LoadType.UNDEF)
		def tlFar = node(TL_FAR, "TLFAR", SiteType.TRANSSHIPMENT, 90, 90, 5, 0, LoadType.UNDEF)

		modelNodes = [depot, transshipCustomer, otherCustomer, tl1, tl2, tlFar, secondCustomer] as Node[]

		def vehicle = new Vehicle(0, "FLEET", 1, [CAPACITY] as float[], Float.MAX_VALUE, 100, 0, 0, 0, 0, 0, 0, 0)

		return new ModelBuilder().build(
				modelNodes,
				[CompartmentType.MIXED] as CompartmentType[],
				vehicle,
				new EucledianMetric(),
				new XFVRPParameter(),
				new StatusManager()
		)
	}

	private Node node(int globalIdx, String name, SiteType siteType, float x, float y, float twClose, float demand, LoadType loadType) {
		return new Node(
				globalIdx, name, siteType,
				x, y, -1,
				(demand != 0) ? ([demand] as float[]) : ((siteType == SiteType.DEPOT) ? ([0f] as float[]) : new float[0]),
				new float[][]{new float[]{0, twClose}},
				0, 0, loadType, 0, "", 0
		)
	}
}
