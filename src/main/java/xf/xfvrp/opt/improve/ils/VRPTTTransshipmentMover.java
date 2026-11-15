package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Applies the Phase 2 improvement neighborhoods of the VRPTT, which change
 * the service mode of a customer or the transshipment location of a record
 * (spec §2.4):
 * <ul>
 * <li><b>Mode change DIRECT to VIA_TDP</b>: a transshipment record is added.
 * A drop visit is inserted after the customer visit in its route and a take
 * visit of the same amount is inserted into another route.</li>
 * <li><b>Mode change VIA_TDP to DIRECT</b>: the record of a drop visit (drop
 * and take visit) is removed.</li>
 * <li><b>TL re-assignment</b>: drop and take visit of a record are moved to
 * another transshipment location.</li>
 * </ul>
 * All moves are synchronization-safe: they are only kept, if the evaluation
 * (including the SynchronizationAnalyzer) reports no penalty. Otherwise the
 * solution is reverted.
 *
 * @author hschneid
 */
public class VRPTTTransshipmentMover extends XFVRPOptBase {

	private static final float EPSILON = 0.001f;

	/**
	 * The mover is used directly on solutions and is no optimization step of
	 * its own.
	 */
	@Override
	public Solution execute(Solution solution) {
		return solution;
	}

	public boolean randomAddRecord(Solution solution) throws XFVRPException {
		List<int[]> customers = findCustomers(solution);
		if (customers.isEmpty())
			return false;

		List<Node> transshipments = findTransshipments();
		if (transshipments.isEmpty())
			return false;

		for (int attempt = 0; attempt < 5; attempt++) {
			int[] customer = customers.get(rand.nextInt(customers.size()));

			// Prefer the transshipment location, which is nearest to the customer
			Node customerNode = solution.getRoutes()[customer[0]][customer[1]];
			Node tl = (attempt == 0) ? findNearestTransshipment(customerNode, transshipments) : transshipments.get(rand.nextInt(transshipments.size()));

			int takeRouteIdx = rand.nextInt(solution.getRoutes().length);
			Node[] takeRoute = solution.getRoutes()[takeRouteIdx];
			if (takeRoute.length == 0)
				continue;
			int takePos = rand.nextInt(takeRoute.length - 1) + 1;

			if (addRecord(solution, customer[0], customer[1], tl.getGlobalIdx(), takeRouteIdx, takePos))
				return true;
		}
		return false;
	}

	/**
	 * Removes the record of a randomly chosen drop visit.
	 *
	 * @return true, if a record was removed
	 */
	public boolean randomRemoveRecord(Solution solution) throws XFVRPException {
		List<int[]> drops = findVisits(solution, false);
		if (drops.isEmpty())
			return false;

		int[] drop = drops.get(rand.nextInt(drops.size()));
		return removeRecord(solution, drop[0], drop[1]);
	}

	/**
	 * Moves the record of a randomly chosen drop visit to a randomly chosen
	 * other transshipment location.
	 *
	 * @return true, if a record was re-assigned
	 */
	public boolean randomReassignTL(Solution solution) throws XFVRPException {
		List<int[]> drops = findVisits(solution, false);
		if (drops.isEmpty())
			return false;

		List<Node> transshipments = findTransshipments();
		if (transshipments.size() < 2)
			return false;

		int[] drop = drops.get(rand.nextInt(drops.size()));
		int currentTlGlobalIdx = solution.getRoutes()[drop[0]][drop[1]].getGlobalIdx();

		for (int attempt = 0; attempt < transshipments.size(); attempt++) {
			Node newTl = transshipments.get(rand.nextInt(transshipments.size()));
			if (newTl.getGlobalIdx() == currentTlGlobalIdx)
				continue;

			if (reassignRecord(solution, drop[0], drop[1], newTl.getGlobalIdx()))
				return true;
		}
		return false;
	}


	/**
	 * Adds a transshipment record: a drop visit of the transshipment location
	 * with the amount of the customer is inserted after the customer visit and
	 * a take visit of the same amount is inserted into another route. The
	 * record is only kept, if the solution stays synchronization-safe.
	 *
	 * @param solution         Solution to change
	 * @param customerRouteIdx Index of the route of the customer
	 * @param customerPos      Position of the customer in its route
	 * @param tlGlobalIdx      Global index of the transshipment location
	 * @param takeRouteIdx     Index of the route, which takes the amount
	 * @param takePos          Insert position of the take visit
	 * @return true, if the record was added
	 */
	public boolean addRecord(Solution solution, int customerRouteIdx, int customerPos, int tlGlobalIdx,
	                         int takeRouteIdx, int takePos) throws XFVRPException {
		return addRecord(solution, customerRouteIdx, customerPos, tlGlobalIdx, takeRouteIdx, takePos, Float.MAX_VALUE) != null;
	}

	/**
	 * Adds a transshipment record like {@link #addRecord(Solution, int, int,
	 * int, int, int)}, but only keeps it, if the new costs stay within the
	 * given bound.
	 *
	 * @return the new costs or null, if the record was not kept
	 */
	public Quality addRecord(Solution solution, int customerRouteIdx, int customerPos, int tlGlobalIdx,
	                         int takeRouteIdx, int takePos, float maxCost) throws XFVRPException {
		if (customerRouteIdx == takeRouteIdx)
			return null;

		Node[] supplierRoute = solution.getRoutes()[customerRouteIdx];
		Node[] takerRoute = solution.getRoutes()[takeRouteIdx];
		if (supplierRoute.length < 3 || takerRoute.length < 2)
			return null;

		Node customer = supplierRoute[customerPos];
		if (customer == null || customer.getSiteType() != SiteType.CUSTOMER || customer.getDemand().length == 0)
			return null;

		float amount = customer.getDemand()[0];
		if (amount <= EPSILON)
			return null;

		Node tl = findNode(tlGlobalIdx);
		if (tl == null)
			return null;

		int depotId = maxDepotId(solution) + 1;
		Node drop = Util.createIdNode(tl, depotId++);
		drop.setDemands(demandArray(-amount));
		Node take = Util.createIdNode(tl, depotId);
		take.setDemands(demandArray(amount));

		Node[] oldSupplierRoute = solution.getRoutes()[customerRouteIdx];
		Node[] oldTakerRoute = takerRoute;

		solution.setRoute(customerRouteIdx, insert(supplierRoute, customerPos + 1, drop));
		solution.setRoute(takeRouteIdx, insert(takerRoute, takePos, take));

		Quality quality = check(solution);
		if (quality.getPenalty() == 0 && quality.getCost() <= maxCost) {
			return quality;
		}

		solution.setRoute(customerRouteIdx, oldSupplierRoute);
		solution.setRoute(takeRouteIdx, oldTakerRoute);
		solution.resetQualities();
		return null;
	}

	/**
	 * Removes the transshipment record of the given drop visit (mode change to
	 * direct service). The drop visit and its paired take visit are removed.
	 *
	 * @return true, if the record was removed
	 */
	public boolean removeRecord(Solution solution, int dropRouteIdx, int dropPos) throws XFVRPException {
		Node[] dropRoute = solution.getRoutes()[dropRouteIdx];
		Node drop = dropRoute[dropPos];

		if (drop == null || drop.getSiteType() != SiteType.TRANSSHIPMENT || demandOf(drop) >= 0)
			return false;

		int[] takePosArr = findPairedVisit(solution, dropRouteIdx, drop);
		if (takePosArr == null)
			return false;

		Node[] oldDropRoute = dropRoute;
		Node[] oldTakeRoute = solution.getRoutes()[takePosArr[0]];

		solution.setRoute(dropRouteIdx, remove(dropRoute, dropPos));
		solution.setRoute(takePosArr[0], remove(oldTakeRoute, takePosArr[1]));

		if (check(solution).getPenalty() == 0)
			return true;

		solution.setRoute(dropRouteIdx, oldDropRoute);
		solution.setRoute(takePosArr[0], oldTakeRoute);
		solution.resetQualities();
		return false;
	}

	/**
	 * Moves the record of the given drop visit to another transshipment
	 * location. Both the drop visit and its paired take visit are re-created
	 * at the new transshipment location.
	 *
	 * @return true, if the record was re-assigned
	 */
	public boolean reassignRecord(Solution solution, int dropRouteIdx, int dropPos, int newTlGlobalIdx) throws XFVRPException {
		Node[] dropRoute = solution.getRoutes()[dropRouteIdx];
		Node drop = dropRoute[dropPos];

		if (drop == null || drop.getSiteType() != SiteType.TRANSSHIPMENT || demandOf(drop) >= 0)
			return false;

		Node newTl = findNode(newTlGlobalIdx);
		if (newTl == null || newTl.getGlobalIdx() == drop.getGlobalIdx())
			return false;

		int[] takePosArr = findPairedVisit(solution, dropRouteIdx, drop);
		if (takePosArr == null)
			return false;

		float amount = -demandOf(drop);

		Node[] oldDropRoute = dropRoute;
		Node[] oldTakeRoute = solution.getRoutes()[takePosArr[0]];

		int depotId = maxDepotId(solution) + 1;
		Node newDrop = Util.createIdNode(newTl, depotId++);
		newDrop.setDemands(demandArray(-amount));
		Node newTake = Util.createIdNode(newTl, depotId);
		newTake.setDemands(demandArray(amount));

		solution.setRoute(dropRouteIdx, replace(dropRoute, dropPos, newDrop));
		solution.setRoute(takePosArr[0], replace(oldTakeRoute, takePosArr[1], newTake));

		if (check(solution).getPenalty() == 0)
			return true;

		solution.setRoute(dropRouteIdx, oldDropRoute);
		solution.setRoute(takePosArr[0], oldTakeRoute);
		solution.resetQualities();
		return false;
	}

	/**
	 * Searches the take visit of the given drop visit: a TL visit of the same
	 * transshipment location with the opposite amount in another route.
	 *
	 * @return array with route index and visit position or null
	 */
	private int[] findPairedVisit(Solution solution, int dropRouteIdx, Node drop) throws XFVRPException {
		float takeAmount = -demandOf(drop);

		Node[][] routes = solution.getRoutes();
		for (int routeIdx = 0; routeIdx < routes.length; routeIdx++) {
			if (routeIdx == dropRouteIdx || routes[routeIdx].length == 0)
				continue;

			for (int pos = 1; pos < routes[routeIdx].length - 1; pos++) {
				Node visit = routes[routeIdx][pos];

				if (visit.getSiteType() != SiteType.TRANSSHIPMENT)
					continue;

				if (visit.getGlobalIdx() != drop.getGlobalIdx())
					continue;

				if (Math.abs(demandOf(visit) - takeAmount) > EPSILON)
					continue;

				return new int[]{routeIdx, pos};
			}
		}
		return null;
	}

	/**
	 * Searches the model node with the given global index.
	 */
	private Node findNode(int globalIdx) {
		for (Node node : model.getNodes())
			if (node.getGlobalIdx() == globalIdx)
				return node;
		return null;
	}

	private List<int[]> findCustomers(Solution solution) {
		List<int[]> customers = new ArrayList<>();
		Node[][] routes = solution.getRoutes();
		for (int routeIdx = 0; routeIdx < routes.length; routeIdx++)
			for (int pos = 1; pos < routes[routeIdx].length - 1; pos++) {
				Node node = routes[routeIdx][pos];
				if (node.getSiteType() == SiteType.CUSTOMER && node.getDemand().length > 0 && node.getDemand()[0] > EPSILON)
					customers.add(new int[]{routeIdx, pos});
			}
		return customers;
	}

	private List<int[]> findVisits(Solution solution, boolean takeVisits) {
		List<int[]> visits = new ArrayList<>();
		Node[][] routes = solution.getRoutes();
		for (int routeIdx = 0; routeIdx < routes.length; routeIdx++)
			for (int pos = 1; pos < routes[routeIdx].length - 1; pos++) {
				Node node = routes[routeIdx][pos];
				if (node.getSiteType() != SiteType.TRANSSHIPMENT)
					continue;

				float demand = (node.getDemand().length > 0) ? node.getDemand()[0] : 0;
				if (takeVisits ? demand > 0 : demand < 0)
					visits.add(new int[]{routeIdx, pos});
			}
		return visits;
	}

	private List<Node> findTransshipments() {
		List<Node> transshipments = new ArrayList<>();
		for (Node node : model.getNodes())
			if (node.getSiteType() == SiteType.TRANSSHIPMENT)
				transshipments.add(node);
		return transshipments;
	}

	private Node findNearestTransshipment(Node customer, List<Node> transshipments) {
		Node nearest = null;
		float nearestDistance = Float.MAX_VALUE;
		for (Node tl : transshipments) {
			float distance = model.getDistance(customer, tl);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = tl;
			}
		}
		return nearest;
	}

	/**
	 * Creates a demand array for the compartments of the model, where the
	 * first compartment carries the given amount.
	 */
	private float[] demandArray(float amount) {
		float[] demand = new float[Math.max(1, model.getCompartments().length)];
		demand[0] = amount;
		return demand;
	}

	private float demandOf(Node node) {
		return (node.getDemand().length > 0) ? node.getDemand()[0] : 0;
	}

	private Node[] insert(Node[] route, int pos, Node node) {
		Node[] newRoute = new Node[route.length + 1];
		System.arraycopy(route, 0, newRoute, 0, pos);
		newRoute[pos] = node;
		System.arraycopy(route, pos, newRoute, pos + 1, route.length - pos);
		return newRoute;
	}

	private Node[] remove(Node[] route, int pos) {
		Node[] newRoute = new Node[route.length - 1];
		System.arraycopy(route, 0, newRoute, 0, pos);
		System.arraycopy(route, pos + 1, newRoute, pos, route.length - pos - 1);
		return newRoute;
	}

	private Node[] replace(Node[] route, int pos, Node node) {
		Node[] newRoute = new Node[route.length];
		System.arraycopy(route, 0, newRoute, 0, route.length);
		newRoute[pos] = node;
		return newRoute;
	}

	/**
	 * Returns the highest depotId of all visits of the solution. New visit
	 * copies get a unique id beyond it.
	 */
	private int maxDepotId(Solution solution) {
		int maxId = 0;
		for (Node[] route : solution.getRoutes())
			for (Node node : route)
				maxId = Math.max(maxId, node.getDepotId());
		return maxId;
	}
}
