package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.*;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.quality.RouteQuality;
import xf.xfvrp.opt.Solution;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * @author hschneid
 *
 */
public class EvaluationService {

	/**
	 * Evaluates the costs and validates the restrictions of the 
	 * given solution. The costs are equal to the driven distance.
	 * The solution contains several routes, which are connected to
	 * a long node sequence. The first and the last node of the
	 * giant route has to be a depot. If a starting depot of a route is different
	 * from the ending depot, then a multi depot vehicle routing is solved. A route
	 * is evaluated always as closed route with same starting and ending depot. The
	 * starting depot of a route stands also for the ending depot, in any case. 
	 */
	public Quality check(Solution solution, XFVRPModel model) throws XFVRPException {
		Context context = ContextBuilder.build(model);

		checkRoutes(solution, context);

		return new Quality(solution.getQuality());
	}

	/**
	 * Checks, if routes are valid and returns the quality.
	 *
	 * Here only 2 routes are checked, which are changed by neighborhood search.
	 */
	public Quality check(Solution solution, XFVRPModel model, int routeIdxA, int routeIdxB) throws XFVRPException {
		Context context = ContextBuilder.build(model);

		solution.invalidateRouteQuality(routeIdxA);
		checkAndUpdateRoutes(routeIdxA, solution, context);
		if(routeIdxA != routeIdxB) {
			solution.invalidateRouteQuality(routeIdxB);
			checkAndUpdateRoutes(routeIdxB, solution, context);
		}

		return new Quality(solution.getQuality());
	}

	private void checkRoutes(Solution solution, Context context) throws XFVRPException {
		Node[][] routes = solution.getRoutes();

		for (int i = 0, routesLength = routes.length; i < routesLength; i++) {
			checkAndUpdateRoutes(i, solution, context);
		}
	}

	private void checkAndUpdateRoutes(int routeIdx, Solution solution, Context context) throws XFVRPException {
		Node[] route = solution.getRoutes()[routeIdx];
		if(route.length == 0) {
			solution.setRouteQuality(routeIdx, new Quality(null));
			return;
		}

		// Feasibility check
		FeasibilityAnalzer.checkFeasibility(route);

		Quality routeQuality = checkRoute(routeIdx, route, context);

		solution.setRouteQuality(routeIdx, routeQuality);
	}

	private Quality checkRoute(int routeIdx, Node[] route, Context context) throws XFVRPException {
		RouteQuality q = new RouteQuality(routeIdx, null);

		route = ActiveNodeAnalyzer.getActiveNodes(route);
		context.setRouteInfos(RouteInfoBuilder.build(route));

		context.setCurrentNode(route[0]);
		beginRoute(route[0], findNextCustomer(route), q, context);
		context.setNextNode(route[0]);

		for (int i = 1; i < route.length; i++) {
			context.setNextNode(route[i]);

			// Times and Distances
			drive(context);

			// Count each stop at different locations (distance between two locations is greater 0)
			checkStop(context);

			// Driver time restrictions for european drivers!
			checkDriverRestrictions(context);

			// Time window constraint for VRPTW
			checkTimeWindow(q, context);

			// Reset loaded or unloaded volume at replenish point
			replenishAmount(context);

			// Capacity constraint for VRP with Pickup & Delivery
			checkCapacities(q, context);

			// Presets
			checkPreset(q, context);

			// Reset of route, if next depot is reached
			if(context.getCurrentNode().getSiteType() == SiteType.DEPOT) {
				finishRoute(q, context);
			}
		}

		// Check of block preset penalty after last node
		int penalty = context.checkPresetBlockCount();
		q.addPenalty(penalty, Quality.PENALTY_REASON_PRESETTING);

		return q;
	}

	private void checkStop(Context context) {
		if(context.getCurrentNode().getSiteType() == SiteType.CUSTOMER)
			context.addStop();

		if(context.getLastNode().getSiteType() == SiteType.CUSTOMER
				&& context.getCurrentNode().getSiteType() == SiteType.CUSTOMER
				&& context.getLastDrivenDistance()[0] == 0)
			context.removeStop();
	}

	private void checkCapacities(Quality q, Context context) {
		float[] amounts = context.getAmountsOfRoute();

		for (int compartment = 0; compartment < context.getNbrOfCompartments(); compartment++) {
			float delivery = (context.getCurrentNode().getLoadType() == LoadType.DELIVERY) ? context.getCurrentNode().getDemand()[compartment] : 0;
			float pickup = (context.getCurrentNode().getLoadType() == LoadType.PICKUP) ? context.getCurrentNode().getDemand()[compartment] : 0;

			int compartmentIdx = compartment * CompartmentLoadType.NBR_OF_LOAD_TYPES;
			amounts[compartmentIdx + CompartmentLoadType.PICKUP.index()] += pickup;
			amounts[compartmentIdx + CompartmentLoadType.DELIVERY.index()] += delivery;
			amounts[compartmentIdx + CompartmentLoadType.MIXED.index()] -= delivery;
			amounts[compartmentIdx + CompartmentLoadType.MIXED.index()] += pickup;
		}

		int penalty = context.checkCapacities();
		q.addPenalty(penalty, Quality.PENALTY_REASON_CAPACITY);
	}

	private void checkTimeWindow(Quality q, Context context) {
		Node currentNode = context.getCurrentNode();
		XFVRPModel model = context.getModel();

		// Service time at depot should be considered into time window
		if(model.getParameter().isWithUnloadingTimeAtDepot() && currentNode.getSiteType() == SiteType.DEPOT) {
			float depotServiceTime = context.getUnLoadingServiceTimeAtDepot();
			context.addToTime(depotServiceTime);
			context.addToDuration(depotServiceTime);
		}

		float[] timeWindow = context.getFittingTimeWindow();

		context.addDelayWithTimeWindow(timeWindow);

		// Wenn der letzte Knoten ein Depot war, wird die
		// Wartezeit nicht mitberechnet, die er h�tte sp�ter abfahren k�nnen
		float waiting = context.getWaitingTimeAtTimeWindow(timeWindow);

		// Check maxWaiting penalty
		if(waiting > model.getVehicle().maxWaitingTime)
			q.addPenalty(1, Quality.PENALTY_REASON_DURATION);

		float serviceTime = (context.getLastDrivenDistance()[0] == 0) ? currentNode.getServiceTime() : currentNode.getServiceTime() + currentNode.getServiceTimeForSite();

		context.setTimeToTimeWindow(timeWindow);
		context.addToTime(serviceTime);
		context.addToDuration(serviceTime + waiting);
	}

	/**
	 * Checks for a lot of preset restrictions or prepare
	 * for later preset restriction checks.
	 */
	private void checkPreset(Quality q, Context context) {
		Node currNode = context.getCurrentNode();

		int blockIndex = currNode.getPresetBlockIdx();

		// Only for non default blocks
		if(blockIndex > BlockNameConverter.DEFAULT_BLOCK_IDX) {
			int peanlty = context.setAndCheckPresetBlock(blockIndex);
			q.addPenalty(peanlty, Quality.PENALTY_REASON_PRESETTING);
		}

		// Sequence rank of current node must be greater or equal than last node
		if(blockIndex >= BlockNameConverter.DEFAULT_BLOCK_IDX) {
			int penalty = context.setAndCheckPresetSequence(blockIndex);
			q.addPenalty(penalty, Quality.PENALTY_REASON_PRESETTING);
		}

		// Set information for black listed nodes restriction (currenty only for customer)
		context.setPresetRouting();

		// Check PresetPosition restriction
		// If last and current node have blocks and blocks are same then a non-default position must be in right order
		int penalty = context.checkPresetPosition();
		q.addPenalty(penalty, Quality.PENALTY_REASON_PRESETTING);

		// Check Preset Depot
		// If current depot is not in the preset depot list of customer
		penalty = context.checkPresetDepot();
		q.addPenalty(penalty, Quality.PENALTY_REASON_PRESETTING);
	}

	private void checkDriverRestrictions(Context context) {
		// check max driving time per shift restrictions
		if(context.getDrivingTime() >= context.getModel().getVehicle().maxDrivingTimePerShift) {
			context.resetDrivingTime();
		}
	}

	private void drive(Context context) {
		float[] dist = context.getModel().getDistanceAndTime(context.getLastNode(), context.getCurrentNode());

		context.drive(dist);
	}

	private void finishRoute(Quality q, Context context) {
		Vehicle v = context.getModel().getVehicle();

		float stopCountPenalty = Math.max(0, context.getNbrOfStops() - v.maxStopCount);
		float durationPenalty = Math.max(0, context.getDuration() - v.maxRouteDuration);
		float delayPenalty = context.getDelay();

		q.addPenalty(stopCountPenalty, Quality.PENALTY_REASON_STOPCOUNT);
		q.addPenalty(delayPenalty, Quality.PENALTY_REASON_DELAY);
		q.addPenalty(durationPenalty, Quality.PENALTY_REASON_DURATION);

		// Add var cost (distance)
		q.addCost(context.getLength());

		// Add fix cost per route
		if(context.getNbrOfStops() > 0)
			q.addCost(v.fixCost);

		// Check for black listed nodes on route
		// Afterwards reset the arrays for next route
		int penalty = context.checkPresetBlackList();
		q.addPenalty(penalty, Quality.PENALTY_REASON_BLACKLIST);
	}

	private void beginRoute(Node newDepot, Node nextNode, Quality q, Context context) throws XFVRPException {
		XFVRPModel model = context.getModel();

		float penalty = context.createNewRoute(newDepot);
		q.addPenalty(penalty, Quality.PENALTY_REASON_CAPACITY);

		float earliestDepartureTime = (nextNode != null) ? nextNode.getTimeWindow(0)[0] - model.getTime(newDepot, nextNode) : 0;

		// If loading time at depot should be considered, service time of all
		// deliveries at the route is added to starting time at depot
		float loadingTimeAtDepot = 0;
		if(model.getParameter().isWithLoadingTimeAtDepot() && nextNode != null)
			loadingTimeAtDepot = context.getLoadingServiceTimeAtDepot();

		context.setDepartureTimeAtDepot(earliestDepartureTime, loadingTimeAtDepot);
	}

	private Node findNextCustomer(Node[] route) {
		for (int i = 1; i < route.length; i++) {
			if(route[i].getSiteType() == SiteType.CUSTOMER)
				return route[i];
		}
		return null;
	}

	private void replenishAmount(Context context) throws XFVRPException {
		if(context.getCurrentNode().getSiteType() != SiteType.REPLENISH)
			return;

		context.resetAmountsOfRoute();
	}
}
