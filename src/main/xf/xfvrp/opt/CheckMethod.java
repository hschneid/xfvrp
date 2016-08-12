package xf.xfvrp.opt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.preset.BlockNameConverter;

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
public class CheckMethod {

	private static final int ROUTE_IDX = 0;
	private static final int LENGTH = 1;
	private static final int DELAY = 2;
	private static final int DURATION = 3;
	private static final int NBR_OF_STOPS = 4;
	private static final int TIME = 5; // Time incl. time windows, driving time, service time, waiting time, rest times
	private static final int DRIVING_TIME = 6; // only driving time

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
	public Quality check(Node[] giantRoute, XFVRPModel model) {
		Quality q = new Quality(null);
		
		if(giantRoute.length == 0)
			return q;
		
		Vehicle vehicle = model.getVehicle();

		// Active nodes for evalution are true, duplicates or empty routes are false
		boolean[] activeNodes = getActiveNodes(giantRoute);

		// Variables
		final int maxGlobalNodeIdx = model.getMaxGlobalNodeIdx() + 1;
		float[] routeVar = new float[7];
		float[] amountArr = new float[model.getVehicle().capacity.length * 3];
		routeVar[ROUTE_IDX] = -1;

		int[] blockPresetArr = new int[model.getNbrOfBlocks()]; Arrays.fill(blockPresetArr, -1);
		int[] availablePresetCountArr = model.getBlockPresetCountList();
		int[] foundPresetCountArr = new int[model.getNbrOfBlocks()];
		int[] lastPresetSequenceRankArr = new int[model.getNbrOfBlocks()];
		boolean[] presetRoutingBlackList = new boolean[maxGlobalNodeIdx];
		boolean[] presetRoutingNodeList = new boolean[maxGlobalNodeIdx];

		// Service times at the depot for amount on the route
		float[][] routeDepotServiceMap = createRouteDepotServiceMap(giantRoute, model);
		float[] earliestPickupTimesAtDepot = createEarliestPickupAtDepotMap(giantRoute, model);

		// Feasibility check
		{
			if(giantRoute[0].getSiteType() != SiteType.DEPOT)
				throw new IllegalStateException("First node in giant route is not a depot.");
			if(giantRoute[giantRoute.length - 1].getSiteType() != SiteType.DEPOT)
				throw new IllegalStateException("Last node in giant route is not a depot.");
		}

		// Begin first route (there is always a first route)
		Node currentDepot = giantRoute[0];

		beginRoute(routeVar, amountArr, currentDepot, findNextCustomer(giantRoute, 0), routeDepotServiceMap, earliestPickupTimesAtDepot, model);
		Node lastNode = currentDepot;
		for (int i = 1; i < giantRoute.length; i++) {
			if(!activeNodes[i])	continue;

			//			if(Debug.debug && i > 900)
			//				System.out.println();

			Node currNode = giantRoute[i];
			final SiteType currSiteType = currNode.getSiteType();

			// Replace ending depot with starting depot (in multi depot problems its nessecary)
			if(currSiteType == SiteType.DEPOT)
				currNode = currentDepot; 

			// Times and Distances
			float[] dist = drive(model, routeVar, lastNode, currNode);

			// Count each stop at different locations (distance between two locations is greater 0)
			if(currSiteType == SiteType.CUSTOMER)
				routeVar[NBR_OF_STOPS]++;
			if(lastNode.getSiteType() == SiteType.CUSTOMER 
					&& currSiteType == SiteType.CUSTOMER
					&& dist[0] == 0)
				routeVar[NBR_OF_STOPS]--;

			// Driver time restrictions for european drivers!
			checkDriverRestrictions(vehicle, routeVar);

			// Time window constraint for VRPTW
			checkTimeWindow(model, q, vehicle, routeVar, routeDepotServiceMap, currNode, dist);

			// Capacity constraint for VRP with Pickup & Delivery
			{
				// Reset loaded or unloaded volume
				if(currSiteType == SiteType.REPLENISH)
					replenishAmount(amountArr, vehicle, q);

				checkCapacities(q, vehicle, currNode, amountArr);
			}

			checkPreset(q, routeVar, blockPresetArr, foundPresetCountArr, lastPresetSequenceRankArr, presetRoutingBlackList, presetRoutingNodeList,	currentDepot, currNode,	lastNode, currSiteType);

			// Reset of route, if next depot is reached
			if(currSiteType == SiteType.DEPOT) {
				finishRoute(routeVar, amountArr, vehicle, q, model);

				beginRoute(routeVar, amountArr, giantRoute[i], findNextCustomer(giantRoute, i), routeDepotServiceMap, earliestPickupTimesAtDepot, model);
				lastPresetSequenceRankArr[BlockNameConverter.DEFAULT_BLOCK_IDX] = Integer.MIN_VALUE;

				// Check for black listed nodes on route
				// Afterwards reset the arrays for next route
				{
					for (int j = 0; j < presetRoutingBlackList.length; j++)
						if(presetRoutingBlackList[j] & presetRoutingNodeList[j]) {
							q.addPenalty(1, Quality.PENALTY_REASON_BLACKLIST);
							break;
						}
					Arrays.fill(presetRoutingBlackList, false);
					Arrays.fill(presetRoutingNodeList, false);
				}
			}

			// Update for next loop
			lastNode = giantRoute[i];
			if(giantRoute[i].getSiteType() == SiteType.DEPOT)
				currentDepot = giantRoute[i];
		}

		// Check of block preset penalty after last node
		for (int j = 0; j < foundPresetCountArr.length; j++)
			if(foundPresetCountArr[j] > 0 && availablePresetCountArr[j] - foundPresetCountArr[j] > 0)
				q.addPenalty(
						availablePresetCountArr[j] - foundPresetCountArr[j], 
						Quality.PENALTY_REASON_PRESETTING
						);

		return q;
	}

	private void checkCapacities(Quality q, Vehicle vehicle, Node currNode, float[] amountArr) {
		float penalty = 0;
		for (int i = 0; i < amountArr.length / 3; i++) {
			float delivery = (currNode.getLoadType() == LoadType.DELIVERY) ? currNode.getDemand()[i] : 0;
			float pickup = (currNode.getLoadType() == LoadType.PICKUP) ? currNode.getDemand()[i] : 0;
			float unloadOnRoute = (pickup < 0) ? pickup : 0; 

			amountArr[i*3] = Math.max(amountArr[i*3] + pickup, amountArr[i*3] + delivery) + unloadOnRoute;
			amountArr[i*3+1] += pickup;
			amountArr[i*3+2] += delivery;

			for (int j = 0; j < 3; j++)
				penalty += Math.max(0, amountArr[i*3+j] - vehicle.capacity[i]);
		}
		if (penalty > 0)
			q.addPenalty(penalty, Quality.PENALTY_REASON_CAPACITY);
	}

	/**
	 * @param model
	 * @param q
	 * @param vehicle
	 * @param routeVar
	 * @param routeDepotServiceMap
	 * @param currNode
	 * @param dist
	 */
	private void checkTimeWindow(XFVRPModel model, Quality q, Vehicle vehicle,
			float[] routeVar, float[][] routeDepotServiceMap, Node currNode,
			float[] dist) {
		// Service time at depot should be considered into time window
		if(model.getParameter().isWithUnloadingTimeAtDepot() && currNode.getSiteType() == SiteType.DEPOT) {
			float depotServiceTime = routeDepotServiceMap[(int)routeVar[ROUTE_IDX]][1];
			routeVar[TIME] += depotServiceTime;
			routeVar[DURATION] += depotServiceTime;
		}

		float[] tw = currNode.getTimeWindow(routeVar[TIME]);
		routeVar[DELAY] += (routeVar[TIME] - tw[1] > 0) ? routeVar[TIME] - tw[1] : 0;

		// Wenn der letzte Knoten ein Depot war, wird die
		// Wartezeit nicht mitberechnet, die er h�tte sp�ter abfahren k�nnen
		float waiting = (routeVar[TIME] < tw[0]) ? tw[0] - routeVar[TIME] : 0;

		// Check maxWaiting penalty
		if(waiting > vehicle.maxWaitingTime)
			q.addPenalty(1, Quality.PENALTY_REASON_DURATION);

		float serviceTime = (dist[0] == 0) ? currNode.getServiceTime() : currNode.getServiceTime() + currNode.getServiceTimeForSite();
		routeVar[TIME] = (routeVar[TIME] > tw[0]) ? routeVar[TIME] : tw[0];
		routeVar[TIME] += serviceTime;
		routeVar[DURATION] += serviceTime + waiting;
	}

	/**
	 * Searches in the giant route for nodes which can be ignored during
	 * evalution. This can be the case for empty routes or unnecessary
	 * replenishments.
	 * 
	 * @param giantRoute
	 * @return list of active (true) or disabled (false) nodes in giant route
	 */
	private boolean[] getActiveNodes(Node[] giantRoute) {
		boolean[] activeNodes = new boolean[giantRoute.length];

		int lastNodeIdx = 0;
		Node lastNode = giantRoute[lastNodeIdx];
		for (int i = 1; i < activeNodes.length; i++) {
			activeNodes[i] = true;

			Node currNode = giantRoute[i];

			if(currNode.getSiteType() == SiteType.DEPOT && 
					lastNode.getSiteType() == SiteType.DEPOT)
				activeNodes[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.REPLENISH &&
					lastNode.getSiteType() == SiteType.REPLENISH)
				activeNodes[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.DEPOT &&
					lastNode.getSiteType() == SiteType.REPLENISH)
				activeNodes[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.REPLENISH &&
					lastNode.getSiteType() == SiteType.DEPOT)
				activeNodes[i] = false;

			if(activeNodes[i]) {
				lastNode = currNode;
				lastNodeIdx = i;
			}
		}

		return activeNodes;
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
	private void checkPreset(
			Quality q,
			float[] routeVar,
			int[] blockPresetArr,
			int[] foundPresetCountArr,
			int[] lastPresetSequenceRankArr,
			boolean[] presetRoutingBlackList,
			boolean[] presetRoutingNodeList,
			Node currDepot,
			Node currNode,
			Node lastNode,
			SiteType currSiteType) {
		int blockIndex = currNode.getPresetBlockIdx();
		// Only for non default blocks
		if(blockIndex > BlockNameConverter.DEFAULT_BLOCK_IDX) {
			// If route index for this block is not initialized, then set route index
			if(blockPresetArr[blockIndex] == -1)
				blockPresetArr[blockIndex] = (int)routeVar[ROUTE_IDX];
			// If route index of this block is different from current route index, failure
			// because all nodes of one block must be on one route
			else if(blockPresetArr[blockIndex] != (int)routeVar[ROUTE_IDX])
				q.addPenalty(1, Quality.PENALTY_REASON_PRESETTING);

			// Save number of seen nodes for this block
			foundPresetCountArr[blockIndex]++;
		}

		// For all blocks
		if(blockIndex >= BlockNameConverter.DEFAULT_BLOCK_IDX) {
			// Sequence rank of current node must be greater or equal than last node
			if(lastPresetSequenceRankArr[blockIndex] > currNode.getPresetBlockRank())
				q.addPenalty(1, Quality.PENALTY_REASON_PRESETTING);
			lastPresetSequenceRankArr[blockIndex] = currNode.getPresetBlockRank();
		}

		// Set information for black listed nodes restriction (currenty only for customer)
		if(currSiteType == SiteType.CUSTOMER) {
			for (int bn : currNode.getPresetRoutingBlackList())
				presetRoutingBlackList[bn] = true;
			presetRoutingNodeList[currNode.getGlobalIdx()] = true;
		}

		// Check PresetPosition restriction
		// If last and current node have blocks and blocks are same then a non-default position must be in right order
		if(currNode.getPresetBlockIdx() > BlockNameConverter.DEFAULT_BLOCK_IDX)
			if (lastNode.getPresetBlockIdx() > BlockNameConverter.DEFAULT_BLOCK_IDX)
				if (currNode.getPresetBlockIdx() == lastNode.getPresetBlockIdx())
					if (currNode.getPresetBlockPos() >= 0 && lastNode.getPresetBlockPos() >= 0 && lastNode.getPresetBlockPos() > currNode.getPresetBlockPos())
						q.addPenalty(1, Quality.PENALTY_REASON_PRESETTING);

		// Check Preset Depot
		// If current depot is not in the preset depot list of customer
		if(currNode.getPresetDepotList().size() > 0 && !currNode.isInPresetDepotList(currDepot.getGlobalIdx()))
			q.addPenalty(1, Quality.PENALTY_REASON_PRESETTING);
	}

	/**
	 * @param vehicle
	 * @param routeVar
	 */
	private void checkDriverRestrictions(Vehicle vehicle, float[] routeVar) {
		// check max driving time per shift restrictions
		if(routeVar[DRIVING_TIME] >= vehicle.maxDrivingTimePerShift) {
			routeVar[DRIVING_TIME] = 0;
			routeVar[TIME] += vehicle.waitingTimeBetweenShifts;
			routeVar[DURATION] += vehicle.waitingTimeBetweenShifts;
		}
	}

	/**
	 * @param model
	 * @param routeVar
	 * @param lastNode
	 * @param currNode
	 * @return
	 */
	private float[] drive(XFVRPModel model, float[] routeVar, Node lastNode,
			Node currNode) {
		float[] dist = model.getDistanceAndTime(lastNode, currNode);
		{
			routeVar[LENGTH] += dist[0];
			routeVar[TIME] += dist[1];
			routeVar[DURATION] += dist[1];
			routeVar[DRIVING_TIME] += dist[1];
		}
		return dist;
	}

	/**
	 * Pr�fen der Strafterme
	 * @param routeVar
	 * @param subRouteVar
	 * @param amountArr
	 * @param v
	 * @param q
	 * @param model 
	 */
	private void finishRoute(float[] routeVar, float[] amountArr, Vehicle v, Quality q, XFVRPModel model) {
		float stopCountPenalty = routeVar[NBR_OF_STOPS] - v.maxStopCount;
		float durationPenalty = routeVar[DURATION] - v.maxRouteDuration;
		float delayPenalty = routeVar[DELAY];

		if (stopCountPenalty > 0) q.addPenalty(stopCountPenalty, Quality.PENALTY_REASON_STOPCOUNT);
		if (delayPenalty > 0) q.addPenalty(delayPenalty, Quality.PENALTY_REASON_DELAY);
		if (durationPenalty > 0) q.addPenalty(durationPenalty, Quality.PENALTY_REASON_DURATION);

		// Add var cost (distance)
		q.addCost(routeVar[LENGTH]);

		// Add fix cost per route
		if(routeVar[NBR_OF_STOPS] > 0)
			q.addCost(v.fixCost);
	}

	/**
	 * 
	 * @param routeVar
	 * @param amountArr
	 * @param currNode
	 * @param nextNode
	 * @param routeDepotServiceMap
	 * @param earliestPickupTimesAtDepot
	 * @param model
	 */
	private void beginRoute(float[] routeVar, float[] amountArr, Node currNode, Node nextNode, float[][] routeDepotServiceMap, float[] earliestPickupTimesAtDepot, XFVRPModel model) {
		float earliestDepartureTime = 0;
		if(nextNode != null)
			earliestDepartureTime = nextNode.getTimeWindow(0)[0] - model.getTime(currNode, nextNode);

		routeVar[ROUTE_IDX]++;

		// If loading time at depot should be considered, service time of all
		// deliveries at the route is added to starting time at depot
		float loadingTime = 0;
		if(nextNode != null && model.getParameter().isWithLoadingTimeAtDepot())
			loadingTime = routeDepotServiceMap[(int)routeVar[ROUTE_IDX]][0];

		routeVar[TIME] = 
				Math.max(
						currNode.getTimeWindow(0)[0] + loadingTime, 
						earliestDepartureTime
						);

		// If one node at this route has a delivery, which depends on a certain 
		// pickup time at the depot, then the earliest departure of this route
		// is reset to the earliest pickup time at depot.

		if(nextNode != null && earliestPickupTimesAtDepot != null)
			routeVar[TIME] = 
			Math.max(
					routeVar[TIME],
					earliestPickupTimesAtDepot[(int)routeVar[ROUTE_IDX]]
					);

		routeVar[DRIVING_TIME] = 0;
		routeVar[NBR_OF_STOPS] = 0;
		Arrays.fill(amountArr, 0);

		routeVar[LENGTH] = 0;
		routeVar[DURATION] = loadingTime;
		routeVar[DELAY] = 0;
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
	 * @param amountArr
	 * @param v
	 * @param q
	 */
	private void replenishAmount(float[] amountArr, Vehicle v, Quality q) {
		float penalty = 0;
		for (int i = 0; i < amountArr.length / 3; i++)
			for (int j = 0; j < 3; j++)
				penalty += Math.max(0, amountArr[i * 3 + j] - v.capacity[i]);

		q.addPenalty(penalty, Quality.PENALTY_REASON_CAPACITY);

		Arrays.fill(amountArr, 0);
	}

	/**
	 * 
	 * @param giantRoute
	 * @param model 
	 * @return For each route it saves the service time at starting and ending depot
	 */
	private float[][] createRouteDepotServiceMap(Node[] giantRoute, XFVRPModel model) {
		List<float[]> list = new ArrayList<>();

		if(model.getParameter().isWithLoadingTimeAtDepot() || model.getParameter().isWithUnloadingTimeAtDepot()) {
			float pickupServiceTime = 0;
			float deliverySericeTime = 0;

			for (int i = 1; i < giantRoute.length; i++) {
				if(giantRoute[i].getSiteType() == SiteType.DEPOT) {
					list.add(new float[]{deliverySericeTime, pickupServiceTime});
					pickupServiceTime = deliverySericeTime = 0;
				} else if(giantRoute[i].getSiteType() == SiteType.CUSTOMER) {
					if(giantRoute[i].getLoadType() == LoadType.PICKUP)
						pickupServiceTime += giantRoute[i].getServiceTime(); 
					else if(giantRoute[i].getLoadType() == LoadType.DELIVERY)
						deliverySericeTime += giantRoute[i].getServiceTime();
					else
						throw new IllegalStateException("Found unexpected load type ("+giantRoute[i].getLoadType().toString()+")");
				} else
					throw new IllegalStateException("Found unexpected site type ("+giantRoute[i].getSiteType().toString()+")");
			}
			return list.toArray(new float[0][]);
		}

		return null;
	}

	/**
	 * 
	 * @param giantRoute
	 * @param model 
	 * @return For each route it saves the earliest pickup time at the starting depot
	 */
	private float[] createEarliestPickupAtDepotMap(Node[] giantRoute, XFVRPModel model) {
		List<Float> list = new ArrayList<>();

		if(model.getParameter().considerEarliestPickupTimeAtDepot()) {
			float earliestPickupTimeAtDepot = 0;

			for (int i = 1; i < giantRoute.length; i++) {
				if(giantRoute[i].getSiteType() == SiteType.DEPOT) {
					list.add(earliestPickupTimeAtDepot);
					earliestPickupTimeAtDepot = 0;
				} else if(giantRoute[i].getSiteType() == SiteType.CUSTOMER) {
					if(giantRoute[i].getLoadType() == LoadType.DELIVERY)
						earliestPickupTimeAtDepot = Math.max(earliestPickupTimeAtDepot, giantRoute[i].getEarliestPickupTimeAtDepot());
				}
			}

			float[] arr = new float[list.size()];
			for (int i = 0; i < arr.length; i++)
				arr[i] = list.get(i);

			return arr;
		}

		return null;
	}
}
