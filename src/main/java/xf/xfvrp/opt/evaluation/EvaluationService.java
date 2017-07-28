package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.Solution;

/** 
 * Copyright (c) 2012-present Holger Schneider
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
	 * given giant route. The costs are equal to the driven distance.
	 * The giant route contains several routes, which are connected to
	 * a long node sequence. The first and the last node of the
	 * giant route has to be a depot. If a starting depot of a route is different
	 * from the ending depot, then a multi depot vehicle routing is solved. A route
	 * is evaluated always as closed route with same starting and ending depot. The
	 * starting depot of a route stands also for the ending depot, in any case. 
	 * 
	 * @param giantRoute sequence of nodes, which is evaluated
	 * @param model The model which contains the main planning data
	 * @return
	 */
	public Quality check(Solution solution, XFVRPModel model) {
		Node[] giantRoute = solution.getGiantRoute();

		Quality q = new Quality(null);
		Vehicle vehicle = model.getVehicle();

		Context context = ContextBuilder.build(solution, model);

		// Feasibility check
		checkFeasibility(giantRoute);

		// Initialization
		context.setCurrentNode(giantRoute[0]);
		beginRoute(findNextCustomer(giantRoute, 0), vehicle, q, model, context);
		context.setNextNode(giantRoute[0]);

		for (int i = 1; i < giantRoute.length; i++) {
			if(!context.isNodeActive(i)) continue;

			context.setNextNode(giantRoute[i]);

			// Times and Distances
			drive(model, context);

			// Count each stop at different locations (distance between two locations is greater 0)
			checkStop(context);

			// Driver time restrictions for european drivers!
			checkDriverRestrictions(vehicle, context);

			// Time window constraint for VRPTW
			checkTimeWindow(model, q, vehicle, context);

			// Reset loaded or unloaded volume at replenish point
			replenishAmount(vehicle, q, context);

			// Capacity constraint for VRP with Pickup & Delivery
			checkCapacities(q, vehicle, context);

			// Presets
			checkPreset(q, context);

			// Reset of route, if next depot is reached
			if(context.getCurrentNode().getSiteType() == SiteType.DEPOT) {
				finishRoute(vehicle, q, model, context);

				beginRoute(findNextCustomer(giantRoute, i), vehicle, q, model, context);
			}
		}

		// Check of block preset penalty after last node
		int penalty = context.checkPresetBlockCount();
		q.addPenalty(penalty, Quality.PENALTY_REASON_PRESETTING);

		return q;
	}

	private void checkFeasibility(Node[] giantRoute) {
		if(giantRoute[0].getSiteType() != SiteType.DEPOT)
			throw new IllegalStateException("First node in giant route is not a depot.");
		if(giantRoute[giantRoute.length - 1].getSiteType() != SiteType.DEPOT)
			throw new IllegalStateException("Last node in giant route is not a depot.");
	}

	private void checkStop(Context context) {
		if(context.getCurrentNode().getSiteType() == SiteType.CUSTOMER)
			context.addStop();

		if(context.getLastNode().getSiteType() == SiteType.CUSTOMER 
				&& context.getCurrentNode().getSiteType() == SiteType.CUSTOMER
				&& context.getLastDrivenDistance()[0] == 0)
			context.removeStop();
	}

	private void checkCapacities(Quality q, Vehicle vehicle, Context context) {
		float[] amounts = context.getAmountsOfRoute();

		for (int i = 0; i < amounts.length / 2; i++) {
			float delivery = (context.getCurrentNode().getLoadType() == LoadType.DELIVERY) ? context.getCurrentNode().getDemand()[i] : 0;
			float pickup = (context.getCurrentNode().getLoadType() == LoadType.PICKUP) ? context.getCurrentNode().getDemand()[i] : 0;

			amounts[i * 2 + 0] -= delivery;
			amounts[i * 2 + 1] += pickup;
		}

		int penalty = context.checkCapacities(vehicle);
		q.addPenalty(penalty, Quality.PENALTY_REASON_CAPACITY);
	}

	/**
	 * 
	 * @param model
	 * @param q
	 * @param vehicle
	 * @param context
	 */
	private void checkTimeWindow(XFVRPModel model, Quality q, Vehicle vehicle, Context context) {
		Node currentNode = context.getCurrentNode();

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
		if(waiting > vehicle.maxWaitingTime)
			q.addPenalty(1, Quality.PENALTY_REASON_DURATION);

		float serviceTime = (context.getLastDrivenDistance()[0] == 0) ? currentNode.getServiceTime() : currentNode.getServiceTime() + currentNode.getServiceTimeForSite();

		context.setTimeToTimeWindow(timeWindow);
		context.addToTime(serviceTime);
		context.addToDuration(serviceTime + waiting);
	}



	/**
	 * Checks for a lot of preset restrictions or prepare
	 * for later preset restriction checks.
	 * 
	 * @param q
	 * @param routeVar
	 * @param blockPresetArr
	 * @param foundPresetCountArr
	 * @param lastPresetSequenceRankArr
	 * @param presetRoutingBlackList
	 * @param presetRoutingNodeList
	 * @param currDepot
	 * @param currNode
	 * @param lastNode
	 * @param currSiteType
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
		int penalty = context.setAndCheckPresetSequence(blockIndex);
		q.addPenalty(penalty, Quality.PENALTY_REASON_PRESETTING);

		// Set information for black listed nodes restriction (currenty only for customer)
		context.setPresetRouting();

		// Check PresetPosition restriction
		// If last and current node have blocks and blocks are same then a non-default position must be in right order
		penalty = context.checkPresetPosition();
		q.addPenalty(penalty, Quality.PENALTY_REASON_PRESETTING);

		// Check Preset Depot
		// If current depot is not in the preset depot list of customer
		penalty = context.checkPresetDepot();
		q.addPenalty(penalty, Quality.PENALTY_REASON_PRESETTING);
	}

	/**
	 * @param vehicle
	 * @param routeVar
	 */
	private void checkDriverRestrictions(Vehicle vehicle, Context context) {
		// check max driving time per shift restrictions
		if(context.getDrivingTime() >= vehicle.maxDrivingTimePerShift) {
			context.resetDrivingTime(vehicle);
		}
	}

	private void drive(XFVRPModel model, Context context) {
		float[] dist = model.getDistanceAndTime(context.getLastNode(), context.getCurrentNode());

		context.drive(dist);
	}

	/**
	 * 
	 * @param v
	 * @param q
	 * @param model
	 * @param context
	 */
	private void finishRoute(Vehicle v, Quality q, XFVRPModel model, Context context) {
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
	}

	/**
	 * 
	 * @param newDepot
	 * @param nextNode
	 * @param q
	 * @param model
	 * @param context
	 */
	private void beginRoute(Node nextNode, Vehicle vehicle, Quality q, XFVRPModel model, Context context) {
		// Check for black listed nodes on route
		// Afterwards reset the arrays for next route
		int penalty = context.checkPresetBlackList();
		q.addPenalty(penalty, Quality.PENALTY_REASON_BLACKLIST);

		penalty = context.createNewRoute(vehicle);
		q.addPenalty(penalty, Quality.PENALTY_REASON_CAPACITY);
		
		float earliestDepartureTime = (nextNode != null) ? nextNode.getTimeWindow(0)[0] - model.getTime(context.getCurrentNode(), nextNode) : 0;

		// If loading time at depot should be considered, service time of all
		// deliveries at the route is added to starting time at depot
		float loadingTimeAtDepot = 0;
		if(model.getParameter().isWithLoadingTimeAtDepot() && nextNode != null)
			loadingTimeAtDepot = context.getLoadingServiceTimeAtDepot();

		context.setDepartureTimeAtDepot(earliestDepartureTime, loadingTimeAtDepot);
	}

	/**
	 * 
	 * @param giantRoute
	 * @param pos
	 * @return
	 */
	private Node findNextCustomer(Node[] giantRoute, int pos) {
		for (int i = pos + 1; i < giantRoute.length; i++) {
			if(giantRoute[i].getSiteType() == SiteType.CUSTOMER)
				return giantRoute[i];
		}
		return null;
	}

	/**
	 * 
	 * @param vehicle
	 * @param q
	 * @param context
	 */
	private void replenishAmount(Vehicle vehicle, Quality q, Context context) {
		if(context.getCurrentNode().getSiteType() != SiteType.REPLENISH)
			return;

		context.resetAmountsOfRoute(vehicle);
	}
}
