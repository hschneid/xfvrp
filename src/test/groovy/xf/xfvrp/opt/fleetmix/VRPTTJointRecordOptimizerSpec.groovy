package xf.xfvrp.opt.fleetmix

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
 * Copyright (c) 2026 Lars Hackstein
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Checks the Phase 3 joint record optimizer: a transshipment record between
 * the routes of two different vehicles is only accepted, if both solutions
 * stay feasible and the joint costs do not exceed the given bound. The
 * whole optimization must not worsen the joint plan.
 *
 * @author lhackste
 */
class VRPTTJointRecordOptimizerSpec extends Specification {

	def "Cross-vehicle record is accepted, if both routes stay feasible and the joint costs stay within the bound"() {
		given:
		def plan = new JointPlan()

		when:
		def added = optimizer().addRecord(plan.solutions, 0, 0, 1, plan.tlGlobalIdx, 1, 0, 1, Float.MAX_VALUE)

		then:
		added != null

		// Drop visit after the customer of the supplier route
		plan.supplier.getRoutes()[0][2].getSiteType() == SiteType.TRANSSHIPMENT
		plan.supplier.getRoutes()[0][2].getDemand()[0] == -5f

		// Take visit before the customer of the taker route
		plan.taker.getRoutes()[0][1].getSiteType() == SiteType.TRANSSHIPMENT
		plan.taker.getRoutes()[0][1].getDemand()[0] == 5f

		// Supplier and taker are different vehicles
		plan.supplier.getModel().getVehicle().getName() != plan.taker.getModel().getVehicle().getName()
	}

	def "Cross-vehicle record is rejected, if the joint costs exceed the bound"() {
		given:
		def plan = new JointPlan()
		def costsBefore = jointCosts(plan.solutions)

		when:
		def added = optimizer().addRecord(plan.solutions, 0, 0, 1, plan.tlGlobalIdx, 1, 0, 1, 0f)

		then:
		// The detours of the record increase the costs, so a bound of 0 rejects it
		added == null
		jointCosts(plan.solutions) == costsBefore
		plan.supplier.getRoutes()[0].length == 4
		plan.taker.getRoutes()[0].length == 3
	}

	def "Cross-vehicle record is rejected, if the taker would be overloaded"() {
		given:
		def plan = new JointPlan()
		// The taker can only carry 4, taking 5 overloads it
		plan.taker.getModel().getVehicle().setCapacity([4f] as float[])

		when:
		def added = optimizer().addRecord(plan.solutions, 0, 0, 1, plan.tlGlobalIdx, 1, 0, 1, Float.MAX_VALUE)

		then:
		added == null
		plan.supplier.getRoutes()[0].length == 4
		plan.taker.getRoutes()[0].length == 3
	}

	def "Optimize keeps the joint plan, if no record can improve it"() {
		given:
		def plan = new JointPlan()
		def costsBefore = jointCosts(plan.solutions)

		when:
		optimizer().optimize(plan.solutions, new StatusManager())

		then:
		jointCosts(plan.solutions) <= costsBefore + 0.001f
	}

	//////////////////////////////////////////

	/** Vehicle capacity of both vehicles */
	private static final float CAPACITY = 10f

	private VRPTTJointRecordOptimizer optimizer() {
		return new VRPTTJointRecordOptimizer()
	}

	private float jointCosts(List<Solution> solutions) {
		float sum = 0
		for (Solution s : solutions)
			sum += new EvaluationService().check(s, recordContext(s, solutions), contextModels(s, solutions)).getCost()
		return sum
	}

	private Node[][] recordContext(Solution own, List<Solution> all) {
		return all.findAll { !it.is(own) }.collectMany { it.getRoutes().toList() } as Node[][]
	}

	private XFVRPModel[] contextModels(Solution own, List<Solution> all) {
		return all.findAll { !it.is(own) }.collectMany { s -> s.getRoutes().collect { s.getModel() } } as XFVRPModel[]
	}

	/**
	 * A joint plan of two per-vehicle solutions: the supplier solution
	 * (vehicle A) serves customer C1 (demand 5) and can hand the collected
	 * amount over at the TL to the taker solution (vehicle B), which serves
	 * its own customer C2.
	 */
	static class JointPlan {

		Solution supplier
		Solution taker
		int tlGlobalIdx

		JointPlan() {
			def depotA = node(0, "DEP", SiteType.DEPOT, 0, 0, Integer.MAX_VALUE, 0, LoadType.UNDEF)
			def customerA = node(1, "C1", SiteType.CUSTOMER, 10, 0, 200, 5f, LoadType.PICKUP)
			def tlA = node(2, "TL1", SiteType.TRANSSHIPMENT, 10, 5, 1000, 0, LoadType.UNDEF)
			def customerA2 = node(4, "C3", SiteType.CUSTOMER, 12, 1, 200, 1f, LoadType.PICKUP)
			def modelNodesA = [depotA, customerA, customerA2, tlA] as Node[]
			def modelA = model(modelNodesA, "VEH_A")

			def depotB = node(0, "DEP", SiteType.DEPOT, 0, 0, Integer.MAX_VALUE, 0, LoadType.UNDEF)
			def customerB = node(3, "C2", SiteType.CUSTOMER, 90, 0, 200, 2f, LoadType.PICKUP)
			def tlB = node(2, "TL1", SiteType.TRANSSHIPMENT, 10, 5, 1000, 0, LoadType.UNDEF)
			def modelNodesB = [depotB, customerB, tlB] as Node[]
			def modelB = model(modelNodesB, "VEH_B")

			tlGlobalIdx = 2

			supplier = new Solution(modelA)
			supplier.addRoute([
					Util.createIdNode(modelNodesA[0], 0),
					Util.createIdNode(modelNodesA[1], 1),
					Util.createIdNode(modelNodesA[2], 2),
					Util.createIdNode(modelNodesA[0], 3)
			] as Node[])

			taker = new Solution(modelB)
			taker.addRoute([
					Util.createIdNode(modelNodesB[0], 3),
					Util.createIdNode(modelNodesB[1], 4),
					Util.createIdNode(modelNodesB[0], 5)
			] as Node[])
		}

		List<Solution> getSolutions() {
			return [supplier, taker]
		}

		private XFVRPModel model(Node[] nodes, String vehicleName) {
			def vehicle = new Vehicle(0, vehicleName, 1, [CAPACITY] as float[], Float.MAX_VALUE, 100, 0, 0, 0, 0, 0, 0, 0)

			return new ModelBuilder().build(
					nodes,
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
}
