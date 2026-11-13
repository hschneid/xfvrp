package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2026 Lars Hackstein
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Analyzes the synchronization constraints of a route, which contains
 * transshipment locations (TLs), for the Vehicle Routing Problem with
 * Trailers and Transshipments (VRPTT).
 * <p>
 * <b>Pass-through visits (Phase 1):</b> a visit of a transshipment location
 * without transferred amount is only useful, if load is transferred there.
 * Therefore at least one customer has to be served between a TL visit and
 * the next TL or depot visit.
 * <p>
 * <b>Transshipment records (Phase 2):</b> load transfer between two different
 * vehicles is expressed by a pair of TL visits in different routes: the
 * supplier route visits the TL with a negative amount (drop) and the taker
 * route visits the same TL with the same positive amount (take). For each
 * record the analyzer checks
 * <ul>
 * <li><b>Load consistency:</b> the dropped amount equals the taken amount,
 * and a drop may not exceed the load, which the supplier collected before,</li>
 * <li><b>Temporal consistency:</b> both vehicles meet at the TL and the
 * transfer finishes before the TL closes (the vehicle, which arrives first,
 * waits at the TL).</li>
 * </ul>
 * Record penalties are charged to the route, which is currently evaluated:
 * take-side penalties (unmatched take, temporal violation) to the taker
 * route, drop-side penalties (unmatched drop, over-dropping) to the supplier
 * route. Because the taker-side checks read the current structure of all
 * routes, a re-evaluation of a route also refreshes the record penalties of
 * its records (see {@link EvaluationService}).
 *
 * @author hschneid
 */
public class SynchronizationAnalyzer {

	private static final float EPSILON = 0.001f;

	/**
	 * Checks the transshipment events of the given route and adds a penalty to
	 * the quality for each violation of a synchronization constraint. Routes
	 * without transshipment locations are not changed.
	 *
	 * @param route     Active nodes of the evaluated route (first and last node are depots)
	 * @param allRoutes All routes of the solution, where transshipment records are searched
	 * @param model     The model (for temporal consistency); may be null to skip the temporal check
	 * @param quality   Quality object, where the penalties are added to
	 */
	public static void analyze(Node[] route, Node[][] allRoutes, XFVRPModel model, Quality quality) {
		analyze(route, allRoutes, model, null, quality);
	}

	/**
	 * Checks the transshipment events like
	 * {@link #analyze(Node[], Node[][], XFVRPModel, Quality)}, but with the
	 * models of all routes (joint multi-vehicle plan, Phase 3): the temporal
	 * consistency of a record is simulated with the model of each involved
	 * route, because foreign routes are evaluated with another vehicle.
	 *
	 * @param route       Active nodes of the evaluated route (first and last node are depots)
	 * @param allRoutes   All routes of the joint plan, where transshipment records are searched
	 * @param model       The model of the evaluated route (for temporal consistency); may be null to skip the temporal check
	 * @param routeModels Models of the routes, aligned with allRoutes (null entries fall back to the evaluated model); may be null
	 * @param quality     Quality object, where the penalties are added to
	 */
	public static void analyze(Node[] route, Node[][] allRoutes, XFVRPModel model, XFVRPModel[] routeModels, Quality quality) {
		if (!containsTransshipment(route))
			return;

		if (allRoutes == null)
			allRoutes = new Node[][]{route};

		// Drops, which were already matched with a take visit of this route,
		// so that one drop serves at most one take
		Set<Node> usedDrops = new HashSet<>();

		for (int i = 1; i < route.length - 1; i++) {
			if (route[i].getSiteType() != SiteType.TRANSSHIPMENT)
				continue;

			float demand = demandOf(route[i]);

			if (demand == 0) {
				// Load consistency: load, which is brought to the TL, has to be
				// taken from the TL and served afterwards.
				checkPassThrough(route, i, quality);
			} else if (demand < 0) {
				checkDrop(route, allRoutes, i, demand, quality);
			} else {
				checkTake(route, allRoutes, model, routeModels, i, demand, usedDrops, quality);
			}
		}
	}

	/**
	 * Returns the route indices, which share a transshipment record with one
	 * of the given routes. These routes have to be re-evaluated, if the given
	 * routes changed, so that the record penalties stay consistent.
	 */
	public static List<Integer> findRecordPartners(Node[][] routes, int routeIdxA, int routeIdxB) {
		List<Integer> partners = new ArrayList<>();
		for (int i = 0; i < routes.length; i++) {
			if (i == routeIdxA || i == routeIdxB)
				continue;

			if (sharesRecordWith(routes[i], routes[routeIdxA]) ||
					(routeIdxA != routeIdxB && sharesRecordWith(routes[i], routes[routeIdxB])))
				partners.add(i);
		}
		return partners;
	}

	private static void checkPassThrough(Node[] route, int tlPos, Quality quality) {
		float transferredAmount = 0;
		int nbrOfServedCustomers = 0;
		for (int j = tlPos + 1; j < route.length - 1 && route[j].getSiteType() == SiteType.CUSTOMER; j++) {
			transferredAmount += Math.abs(route[j].getDemand()[0]);
			nbrOfServedCustomers++;
		}

		if (nbrOfServedCustomers == 0 || transferredAmount <= 0)
			quality.addPenalty(1, Quality.PENALTY_REASON_SYNCHRONIZATION);
	}

	/**
	 * Checks a drop visit (supplier side): The dropped amount may not exceed
	 * the load, which was collected on the route before (load consistency),
	 * and another route has to take the amount (otherwise the load would be
	 * left at the TL).
	 */
	private static void checkDrop(Node[] route, Node[][] allRoutes, int tlPos, float demand, Quality quality) {
		if (collectedLoadBefore(route, tlPos) + EPSILON < -demand)
			quality.addPenalty(1, Quality.PENALTY_REASON_SYNCHRONIZATION);

		if (findMatchingVisit(null, allRoutes, route[tlPos], -demand, null) == null)
			quality.addPenalty(1, Quality.PENALTY_REASON_SYNCHRONIZATION);
	}

	/**
	 * Checks a take visit (taker side): Another route has to drop the same
	 * amount at the same TL, and the transfer between both vehicles has to
	 * finish before the TL closes.
	 */
	private static void checkTake(Node[] route, Node[][] allRoutes, XFVRPModel model, XFVRPModel[] routeModels,
	                              int tlPos, float demand, Set<Node> usedDrops, Quality quality) {
		int[] dropPosArr = findMatchingVisit(route, allRoutes, route[tlPos], -demand, usedDrops);

		if (dropPosArr == null) {
			quality.addPenalty(1, Quality.PENALTY_REASON_SYNCHRONIZATION);
			return;
		}

		usedDrops.add(allRoutes[dropPosArr[0]][dropPosArr[1]]);

		if (model != null && !isTemporallyConsistent(
				route, tlPos, model,
				allRoutes[dropPosArr[0]], dropPosArr[1], modelFor(routeModels, dropPosArr[0], model)))
			quality.addPenalty(1, Quality.PENALTY_REASON_SYNCHRONIZATION);
	}

	/**
	 * Searches for a TL visit of the same TL with the opposite amount (drop
	 * for a take, take for a drop). The evaluated route can be excluded from
	 * the search (no transshipment within one route), and already used drops
	 * are not returned again (one drop serves one take).
	 *
	 * @return array with route index and visit position or null, if no
	 *         matching visit exists
	 */
	private static int[] findMatchingVisit(Node[] excludedRoute, Node[][] allRoutes, Node tlVisit, float oppositeAmount, Set<Node> usedDrops) {
		for (int routeIdx = 0; routeIdx < allRoutes.length; routeIdx++) {
			Node[] otherRoute = allRoutes[routeIdx];
			if (otherRoute == excludedRoute || otherRoute.length == 0)
				continue;

			for (int j = 1; j < otherRoute.length - 1; j++) {
				Node visit = otherRoute[j];
				if (visit.getSiteType() != SiteType.TRANSSHIPMENT)
					continue;

				if (visit.getGlobalIdx() != tlVisit.getGlobalIdx())
					continue;

				if (Math.abs(demandOf(visit) - oppositeAmount) > EPSILON)
					continue;

				if (usedDrops != null && usedDrops.contains(visit))
					continue;

				return new int[]{routeIdx, j};
			}
		}
		return null;
	}

	/**
	 * Checks, if the transfer between both vehicles finishes within the time
	 * window of the TL. Both vehicles meet at the later arrival time, where
	 * the earlier vehicle waits at the TL.
	 */
	private static boolean isTemporallyConsistent(Node[] takerRoute, int takePos, XFVRPModel takerModel,
	                                              Node[] supplierRoute, int dropPos, XFVRPModel supplierModel) {
		float meetingTime = Math.max(
				RouteTimingSimulator.simulateArrival(takerRoute, takePos, takerModel),
				RouteTimingSimulator.simulateArrival(supplierRoute, dropPos, supplierModel)
		);

		float transferFinish = meetingTime + takerRoute[takePos].getServiceTime();
		return transferFinish <= takerRoute[takePos].getTimeWindow(meetingTime)[1] + EPSILON;
	}

	/**
	 * Returns the model of the route at the given index of the joint plan. A
	 * missing models array or a null entry falls back to the model of the
	 * currently evaluated route.
	 */
	private static XFVRPModel modelFor(XFVRPModel[] routeModels, int routeIdx, XFVRPModel defaultModel) {
		if (routeModels == null || routeIdx >= routeModels.length || routeModels[routeIdx] == null)
			return defaultModel;
		return routeModels[routeIdx];
	}

	/**
	 * Returns the load, which was collected on the route before the given
	 * position (sum of customer demands reduced by prior drops).
	 */
	private static float collectedLoadBefore(Node[] route, int untilPos) {
		float collected = 0;
		for (int j = 1; j < untilPos; j++)
			collected += demandOf(route[j]);
		return collected;
	}

	private static float demandOf(Node node) {
		return (node.getDemand().length > 0) ? node.getDemand()[0] : 0;
	}

	private static boolean sharesRecordWith(Node[] route, Node[] otherRoute) {
		for (int i = 1; i < route.length - 1; i++) {
			float demand = demandOf(route[i]);
			if (route[i].getSiteType() != SiteType.TRANSSHIPMENT || demand == 0)
				continue;

			if (findMatchingVisit(null, new Node[][]{otherRoute}, route[i], -demand, null) != null)
				return true;
		}
		return false;
	}

	private static boolean containsTransshipment(Node[] route) {
		for (Node node : route)
			if (node.getSiteType() == SiteType.TRANSSHIPMENT)
				return true;
		return false;
	}

	/**
	 * Checks, if at least one customer follows the route position (until the
	 * next transshipment location or depot), so that the transferred load is
	 * served afterwards.
	 */
	private static boolean hasFollowingCustomer(Node[] route, int pos) {
		int j = pos + 1;
		while (j < route.length - 1 && route[j].getSiteType() == SiteType.CUSTOMER)
			j++;
		return j > pos + 1;
	}
}
