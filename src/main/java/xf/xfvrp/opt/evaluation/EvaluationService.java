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

		// Begin first route (there is always a first route)
		context.setNextNode(giantRoute[0]);
		beginRoute(giantRoute[0], findNextCustomer(giantRoute, 0), q, model, context);

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

				beginRoute(giantRoute[i], findNextCustomer(giantRoute, i), q, model, context);

			}
		}

		// Check of block preset penalty after last node
		int penalty = context.checkPresetBlockCount();
		q.addPenalty(
				penalty, 
				Quality.PENALTY_REASON_PRESETTING
				);

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

		for (int i = 0; i < amounts.length / 3; i++) {
			float delivery = (context.getCurrentNode().getLoadType() == LoadType.DELIVERY) ? context.getCurrentNode().getDemand()[i] : 0;
			float pickup = (context.getCurrentNode().getLoadType() == LoadType.PICKUP) ? context.getCurrentNode().getDemand()[i] : 0;
			float unloadOnRoute = (pickup < 0) ? pickup : 0; 

			amounts[i*3] = Math.max(amounts[i*3] + pickup, amounts[i*3] + delivery) + unloadOnRoute;
			amounts[i*3+1] += pickup;
			amounts[i*3+2] += delivery;
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
			float depotServiceTime = context.getUnLoadingTimesAtDepotForCurrentRoute();
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
		float stopCountPenalty = context.getNbrOfStops() - v.maxStopCount;
		float durationPenalty = context.getDuration() - v.maxRouteDuration;
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
	 * @param currNode
	 * @param nextNode
	 * @param q
	 * @param model
	 * @param context
	 */
	private void beginRoute(Node currNode, Node nextNode, Quality q, XFVRPModel model, Context context) {
		float earliestDepartureTime = (nextNode != null) ? nextNode.getTimeWindow(0)[0] - model.getTime(currNode, nextNode) : 0;

		// If loading time at depot should be considered, service time of all
		// deliveries at the route is added to starting time at depot
		float loadingTime = 0;
		if(model.getParameter().isWithLoadingTimeAtDepot() && nextNode != null)
			loadingTime = context.getLoadingTimesAtDepotForCurrentRoute();

		// Check for black listed nodes on route
		// Afterwards reset the arrays for next route
		int penalty = context.checkPresetBlackList();
		q.addPenalty(penalty, Quality.PENALTY_REASON_BLACKLIST);

		context.createNewRoute(currNode, earliestDepartureTime, loadingTime);
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
	 * @param v
	 * @param q
	 * @param context
	 */
	private void replenishAmount(Vehicle v, Quality q, Context context) {
		if(context.getCurrentNode().getSiteType() != SiteType.REPLENISH)
			return;

		int penalty = context.checkCapacities(v);

		q.addPenalty(penalty, Quality.PENALTY_REASON_CAPACITY);

		context.resetAmountsOfRoute();
	}
}
