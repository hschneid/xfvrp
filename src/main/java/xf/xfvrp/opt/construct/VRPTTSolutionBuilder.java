package xf.xfvrp.opt.construct;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.ils.VRPTTTransshipmentMover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Construction heuristic for the Vehicle Routing Problem with Trailers and
 * Transshipments (VRPTT), Phase 1 (MVP).
 * <p>
 * The service modes of each customer (spec §2.1) are enumerated by the
 * {@link VRPTTTaskEnumerator}. All derived tasks are inserted sequentially
 * in a randomized order into the current solution, where for each customer
 * the cheapest feasible way of service is chosen:
 * <ul>
 * <li>DIRECT(c) is inserted as a single customer visit,</li>
 * <li>VIA_TDP(c, t) is inserted as a block of a transshipment location visit
 * directly followed by the customer visit.</li>
 * </ul>
 * Feasibility is checked by the evaluation service, which includes the
 * synchronization constraints at transshipment locations
 * (SynchronizationAnalyzer). If no feasible insertion can be found, the
 * customer is tried again on an additional route.
 * <p>
 * For a model without transshipment locations, this builder degenerates to
 * a first-best insertion heuristic.
 *
 * @author hschneid
 */
public class VRPTTSolutionBuilder extends XFVRPOptBase {

	private final VRPTTTaskEnumerator taskEnumerator = new VRPTTTaskEnumerator();
	private final VRPTTTransshipmentMover mover = new VRPTTTransshipmentMover();

	@Override
	public Solution execute(Solution input) throws XFVRPException {
		XFVRPModel model = input.getModel();

		List<Node> transshipments = getTransshipments(model);
		List<Node> customers = getServiceableCustomers(model);

		// Init with empty route (attention for multiple depots)
		Solution solution = initNewSolution(model);

		// Randomized ordering of customer insertions
		Collections.shuffle(customers, rand);

		int[] maxIdx = {2};
		for (Node customer : customers) {
			List<VRPTTTask> tasks = taskEnumerator.enumerate(customer, transshipments);

			boolean inserted = insertTasks(solution, tasks, maxIdx);
			if (!inserted) {
				// If no feasible insertion can be found, add empty route and retry
				NormalizeSolutionService.normalizeRoute(solution);
				insertTasks(solution, tasks, maxIdx);
			}
		}

		NormalizeSolutionService.normalizeRouteWithCleanup(solution);

		// Phase 2: Create transshipment records, where the service mode change
		// to VIA_TDP does not worsen the costs. The records free load capacity
		// of the suppliers, which the improvement phase can use.
		createRecords(solution, customers, transshipments);

		return solution;
	}

	/**
	 * Greedy pass over all customers: For each customer, the cheapest
	 * transshipment record (drop visit after the customer visit, take visit in
	 * another route) is created, as long as it does not worsen the costs.
	 * Records with equal costs are also taken, because they free load capacity
	 * of the supplier route.
	 */
	private void createRecords(Solution solution, List<Node> customers, List<Node> transshipments) throws XFVRPException {
		if (transshipments.isEmpty())
			return;

		mover.setModel(model);

		for (Node customer : customers) {
			int[] customerPos = findCustomer(solution, customer.getGlobalIdx());
			if (customerPos == null)
				continue;

			float bestCost = check(solution).getCost();

			for (Node tl : transshipments) {
				for (int routeIdx = 0; routeIdx < solution.getRoutes().length; routeIdx++) {
					if (routeIdx == customerPos[0] || solution.getRoutes()[routeIdx].length < 2)
						continue;

					for (int pos = 1; pos < solution.getRoutes()[routeIdx].length; pos++) {
						Quality q = mover.addRecord(solution, customerPos[0], customerPos[1], tl.getGlobalIdx(), routeIdx, pos, bestCost);
						if (q != null)
							bestCost = q.getCost();

						// The customer position can have shifted by the inserted visit
						customerPos = findCustomer(solution, customer.getGlobalIdx());
						if (customerPos == null)
							break;
					}
					if (customerPos == null)
						break;
				}
				if (customerPos == null)
					break;
			}
		}
	}

	/**
	 * Searches the position of the customer with the given global index.
	 */
	private int[] findCustomer(Solution solution, int globalIdx) {
		Node[][] routes = solution.getRoutes();
		for (int routeIdx = 0; routeIdx < routes.length; routeIdx++)
			for (int pos = 1; pos < routes[routeIdx].length - 1; pos++)
				if (routes[routeIdx][pos].getSiteType() == SiteType.CUSTOMER && routes[routeIdx][pos].getGlobalIdx() == globalIdx)
					return new int[]{routeIdx, pos};
		return null;
	}

	/**
	 * Tries to insert the customer of the given ways of service into the
	 * current solution. All insertions are evaluated and the cheapest feasible
	 * one is applied.
	 *
	 * @return true, if the customer was inserted by one of the tasks
	 */
	private boolean insertTasks(Solution solution, List<VRPTTTask> tasks, int[] maxIdx) throws XFVRPException {
		// Get all feasible insertion points for all tasks on current routes
		List<float[]> insertPoints = evaluate(solution, tasks);

		// Sort for lowest insertion costs (reverse orientation)
		insertPoints.sort((a, b) -> (int) ((b[0] - a[0]) * 1000f));

		// For all found feasible insertion points, starting with the cheapest
		for (int i = insertPoints.size() - 1; i >= 0; i--) {
			float[] val = insertPoints.get(i);
			int taskIdx = (int) val[1];
			int routeIdx = (int) val[2];
			int pos = (int) val[3];

			VRPTTTask task = tasks.get(taskIdx);
			Node[] oldRoute = insertBlock(solution, task, routeIdx, pos, maxIdx);

			Quality qq = check(solution, routeIdx, routeIdx);
			if (qq.getPenalty() == 0) {
				solution.fixateQualities();
				return true;
			}

			// Reverse change
			solution.setRoute(
					routeIdx,
					oldRoute
			);
			solution.resetQualities();
		}

		return false;
	}

	/**
	 * Evaluates all insertion points in the current solution for all given
	 * ways of service.
	 */
	private List<float[]> evaluate(Solution solution, List<VRPTTTask> tasks) {
		List<float[]> insertPoints = new ArrayList<>();

		for (int taskIdx = 0; taskIdx < tasks.size(); taskIdx++) {
			VRPTTTask task = tasks.get(taskIdx);

			for (int routeIdx = 0; routeIdx < solution.getRoutes().length; routeIdx++) {
				Node[] route = solution.getRoutes()[routeIdx];
				for (int pos = 1; pos < route.length; pos++) {
					insertPoints.add(new float[]{
							getEffortOfInsertion(task, route, pos),
							taskIdx,
							routeIdx,
							pos
					});
				}
			}
		}

		return insertPoints;
	}

	/**
	 * Calculates the additional distance of inserting the block of the given
	 * task (customer visit or block of TL visit + customer visit) at the
	 * given position.
	 */
	private float getEffortOfInsertion(VRPTTTask task, Node[] route, int pos) {
		Node predecessor = route[pos - 1];
		Node successor = route[pos];

		// Replaced direct connection between predecessor and successor
		float effort = -getDistanceForOptimization(predecessor, successor);

		if (task.getMode() == VRPTTTask.Mode.DIRECT) {
			effort += getDistanceForOptimization(predecessor, task.getCustomer()) +
					getDistanceForOptimization(task.getCustomer(), successor);
		} else {
			Node transshipment = task.getTransshipment();
			effort += getDistanceForOptimization(predecessor, transshipment) +
					getDistanceForOptimization(transshipment, task.getCustomer()) +
					getDistanceForOptimization(task.getCustomer(), successor);
		}

		return effort;
	}

	private int getBlockSize(VRPTTTask task) {
		return (task.getMode() == VRPTTTask.Mode.VIA_TDP) ? 2 : 1;
	}

	/**
	 * Inserts the block of the given task (customer visit or block of TL visit
	 * + customer visit) into a route at a certain position.
	 */
	private Node[] insertBlock(Solution solution, VRPTTTask task, int routeIdx, int pos, int[] maxIdx) {
		Node[] route = solution.getRoutes()[routeIdx];
		int blockSize = getBlockSize(task);

		Node[] block;
		if (blockSize == 2) {
			Node transshipment = Util.createIdNode(task.getTransshipment(), maxIdx[0]++);
			block = new Node[]{transshipment, task.getCustomer()};
		} else {
			block = new Node[]{task.getCustomer()};
		}

		Node[] newRoute = new Node[route.length + blockSize];
		System.arraycopy(route, 0, newRoute, 0, pos);
		System.arraycopy(block, 0, newRoute, pos, blockSize);
		System.arraycopy(route, pos, newRoute, pos + blockSize, route.length - pos);

		solution.setRoute(routeIdx, newRoute);

		return route;
	}

	/**
	 * Returns all transshipment locations of the model.
	 */
	private List<Node> getTransshipments(XFVRPModel model) {
		List<Node> transshipments = new ArrayList<>();
		for (Node node : model.getNodes())
			if (node.getSiteType() == SiteType.TRANSSHIPMENT)
				transshipments.add(node);
		return transshipments;
	}

	/**
	 * Returns all customers, which are allowed to be served by the model
	 * vehicle. Customers, which are blocked for this vehicle type, are left
	 * unplanned by this builder.
	 */
	private List<Node> getServiceableCustomers(XFVRPModel model) {
		List<Node> customers = new ArrayList<>();
		for (Node node : model.getNodes())
			if (node.getSiteType() == SiteType.CUSTOMER &&
					(node.getPresetBlockVehicleList().isEmpty() ||
							node.getPresetBlockVehicleList().contains(model.getVehicle().getIdx())))
				customers.add(node);
		return customers;
	}

	/**
	 * Initialize a new solution with a single empty route per depot.
	 */
	private Solution initNewSolution(XFVRPModel model) {
		Node[] route = new Node[2];
		route[0] = Util.createIdNode(model.getNodes()[0], 0);
		route[1] = Util.createIdNode(model.getNodes()[0], 1);

		Solution solution = new Solution(model);
		solution.addRoute(route);
		return NormalizeSolutionService.normalizeRoute(solution);
	}
}
