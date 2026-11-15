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
import xf.xfvrp.base.Quality
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
 * Checks the Phase 3 joint multi-vehicle plan: a transshipment record may
 * span two routes of DIFFERENT vehicles (different per-vehicle solutions).
 * The evaluation of one solution has to find the record partner in the
 * other vehicle's route (load and temporal consistency), and the record
 * has to be penalized, if the partner is missing or inconsistent.
 *
 * @author lhackste
 */
class VRPTTJointPlanSpec extends Specification {

	def "Cross-vehicle record is consistent, if the partner visit is in another vehicle's route"() {
		given:
		def plan = jointPlan(5f, 1000)

		when:
		def qSupplier = check(plan.supplier)
		def qTaker = check(plan.taker)

		then:
		qSupplier.getPenalty() == 0
		qTaker.getPenalty() == 0
	}

	def "Cross-vehicle record is penalized, if it is evaluated without the partner solution"() {
		given:
		def plan = jointPlan(5f, 1000)

		when:
		def qSupplier = new EvaluationService().check(plan.supplier)

		then:
		// The drop visit finds no take visit in its own solution
		qSupplier.getPenalty() > 0
	}

	def "Cross-vehicle record is penalized, if the amounts do not match"() {
		given:
		def plan = jointPlan(5f, 1000)
		// The take visit carries a different amount than the drop visit
		plan.taker.getRoutes()[0][1].setDemands([4.5f] as float[])

		when:
		def qSupplier = check(plan.supplier)
		def qTaker = check(plan.taker)

		then:
		qSupplier.getPenalty() > 0
		qTaker.getPenalty() > 0
	}

	def "Cross-vehicle record is penalized, if the transfer time window is violated"() {
		given:
		def plan = jointPlan(5f, 5)

		when:
		def qTaker = check(plan.taker)

		then:
		// The supplier arrives at the TL long after its time window is closed
		qTaker.getPenalty() > 0
	}

	def "Cross-vehicle record is penalized, if the taker is overloaded"() {
		given:
		def plan = jointPlan(5f, 1000)
		// The taker vehicle can only carry 4, so taking 5 overloads it
		plan.taker.model.getVehicle().setCapacity([4f] as float[])

		when:
		def qTaker = check(plan.taker)

		then:
		qTaker.getPenalty() > 0
	}

	def "Record visit at the end of a route is not penalized"() {
		given:
		def plan = jointPlan(5f, 1000)
		// The take visit carries the load home: a record visit at the end of
		// a route is no pass-through violation
		plan.setSupplierRoute([plan.depotA(), plan.customerA(), plan.dropVisit(), plan.depotA()])

		when:
		def qSupplier = check(plan.supplier)

		then:
		qSupplier.getPenalty() == 0
	}

	//////////////////////////////////////////

	/**
	 * Evaluates the given solution with the other solution of the joint plan
	 * as transshipment record context.
	 */
	private Quality check(Solution own) {
		return new EvaluationService().check(own, other(own).getRoutes(), contextModels(own))
	}

	private Solution other(Solution own) {
		return own.is(plan.supplier) ? plan.taker : plan.supplier
	}

	private XFVRPModel[] contextModels(Solution own) {
		Solution o = other(own)
		return o.getRoutes().collect { o.getModel() }
	}

	private JointPlan getPlan() {
		return plan
	}

	private JointPlan plan

	private JointPlan jointPlan(float takeAmount, float tlTimeWindowClose) {
		plan = new JointPlan(takeAmount, tlTimeWindowClose)
		return plan
	}

	/**
	 * A joint plan of two per-vehicle solutions: the supplier solution
	 * (vehicle A) serves customer C1 (demand 5) and drops the collected
	 * amount at the TL; the taker solution (vehicle B) takes the same amount
	 * at the TL, before it serves its own customer C2.
	 */
	private static class JointPlan {

		private XFVRPModel modelA
		private XFVRPModel modelB
		private Node[] modelNodesA
		private Node[] modelNodesB
		private Solution supplier
		private Solution taker

		JointPlan(float takeAmount, float tlTimeWindowClose) {
			def depotA = node(0, "DEP", SiteType.DEPOT, 0, 0, Integer.MAX_VALUE, 0, LoadType.UNDEF)
			def customerA = node(1, "C1", SiteType.CUSTOMER, 10, 0, 200, 5f, LoadType.PICKUP)
			def customerA2 = node(4, "C3", SiteType.CUSTOMER, 12, 1, 200, 1f, LoadType.PICKUP)
			def tlA = node(2, "TL1", SiteType.TRANSSHIPMENT, 10, 5, tlTimeWindowClose, 0, LoadType.UNDEF)
			modelNodesA = [depotA, customerA, customerA2, tlA] as Node[]
			modelA = model(modelNodesA, "VEH_A", 10f)

			def depotB = node(0, "DEP", SiteType.DEPOT, 0, 0, Integer.MAX_VALUE, 0, LoadType.UNDEF)
			def customerB = node(3, "C2", SiteType.CUSTOMER, 90, 0, 200, 2f, LoadType.PICKUP)
			def tlB = node(2, "TL1", SiteType.TRANSSHIPMENT, 10, 5, tlTimeWindowClose, 0, LoadType.UNDEF)
			modelNodesB = [depotB, customerB, tlB] as Node[]
			modelB = model(modelNodesB, "VEH_B", 10f)

			this.depotANode = depotA
			this.customerANode = customerA
			this.tlANode = tlA

			supplier = new Solution(modelA)
			setSupplierRoute([
					Util.createIdNode(depotA, 0),
					Util.createIdNode(customerA, 1),
					visit(tlA, -takeAmount, 6),
					Util.createIdNode(customerA2, 8),
					Util.createIdNode(depotA, 2)
			])

			taker = new Solution(modelB)
			taker.addRoute([
					Util.createIdNode(modelNodesB[0], 3),
					visit(tlB, takeAmount, 7),
					Util.createIdNode(modelNodesB[1], 4),
					Util.createIdNode(modelNodesB[0], 5)
			] as Node[])
		}

		/** Sets the routes of the supplier solution */
		void setSupplierRoute(List nodes) {
			supplier.setRoute(0, nodes as Node[])
		}

		Node depotA() {
			return Util.createIdNode(depotANode, 9)
		}

		Node customerA() {
			return Util.createIdNode(customerANode, 10)
		}

		Node dropVisit() {
			def drop = Util.createIdNode(tlANode, 11)
			drop.setDemands([-5f] as float[])
			return drop
		}

		private Node depotANode
		private Node customerANode
		private Node tlANode

		private Node visit(Node tl, float amount, int depotId) {
			def visit = Util.createIdNode(tl, depotId)
			visit.setDemands([amount] as float[])
			return visit
		}

		private XFVRPModel model(Node[] nodes, String vehicleName, float capacity) {
			def vehicle = new Vehicle(0, vehicleName, 1, [capacity] as float[], Float.MAX_VALUE, 100, 0, 0, 0, 0, 0, 0, 0)

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
