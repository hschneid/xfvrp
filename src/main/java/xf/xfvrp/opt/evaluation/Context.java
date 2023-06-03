package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.*;
import xf.xfvrp.base.compartment.CompartmentLoad;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.base.preset.BlockNameConverter;

import java.util.Arrays;
import java.util.Map;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @author hschneid
 *
 */
public class Context {

	public static final int ROUTE_IDX = 0;
	public static final int LENGTH = 1;
	public static final int DELAY = 2;
	public static final int DURATION = 3;
	public static final int NBR_OF_STOPS = 4;
	public static final int TIME = 5; // Time incl. time windows, driving time, service time, waiting time, rest times
	public static final int DRIVING_TIME = 6; // only driving time

	// Variables
	private int maxGlobalNodeIdx;
	private final float[] routeVar;
	private CompartmentLoad[] amountsOfRoute;

	private int[] blockPresetArr;
	private int[] availablePresetCountArr;
	private int[] foundPresetCountArr;
	private int[] lastPresetSequenceRankArr;
	private boolean[] presetRoutingBlackList;
	private boolean[] presetRoutingNodeList;

	// Pre-evaluated infos of a route (service times and amounts)
	private Map<Node, RouteInfo[]> routeInfos;

	private Node currentDepot;
	private Node currentNode;
	private Node lastNode;

	private float[] lastDrivenDistance;

	private final XFVRPModel model;

	public Context(XFVRPModel model) {
		this.model = model;
		routeVar = new float[7];
		routeVar[ROUTE_IDX] = -1;
	}

	public void setNextNode(Node newCurrentNode) {
		this.lastNode = this.currentNode;
		this.currentNode = newCurrentNode;
	}

	public float[] getFittingTimeWindow() {
		return this.currentNode.getTimeWindow(routeVar[TIME]);
	}

	public void drive(float[] distance) {
		this.lastDrivenDistance = distance;

		routeVar[LENGTH] += distance[0];
		routeVar[TIME] += distance[1];
		routeVar[DURATION] += distance[1];
		routeVar[DRIVING_TIME] += distance[1];
	}

	public void addStop() {
		routeVar[NBR_OF_STOPS]++;
	}

	public void removeStop() {
		routeVar[NBR_OF_STOPS]--;
	}

	public void resetDrivingTime() {
		Vehicle vehicle = model.getVehicle();

		routeVar[DRIVING_TIME] = 0;
		routeVar[TIME] += vehicle.getWaitingTimeBetweenShifts();
		routeVar[DURATION] += vehicle.getWaitingTimeBetweenShifts();
	}

	public void createNewRoute(Node newDepot) throws XFVRPException {
		routeVar[ROUTE_IDX]++;

		setCurrentDepot(newDepot);

		routeVar[DRIVING_TIME] = 0;
		routeVar[NBR_OF_STOPS] = 0;
		resetAmountsOfRoute();

		routeVar[LENGTH] = 0;
		routeVar[DELAY] = 0;

		lastPresetSequenceRankArr[BlockNameConverter.DEFAULT_BLOCK_IDX] = Integer.MIN_VALUE;
		Arrays.fill(presetRoutingBlackList, false);
		Arrays.fill(presetRoutingNodeList, false);
	}

	public void setDepartureTimeAtDepot(float earliestDepartureTime, float loadingTimeAtDepot) {
		routeVar[TIME] =
				Math.max(
						this.currentDepot.getTimeWindow(0)[0] + loadingTimeAtDepot,
						earliestDepartureTime
				);
		routeVar[DURATION] = loadingTimeAtDepot;
	}

	public void resetAmountsOfRoute() throws XFVRPException {
		// Assumption: currentNode = DEPOT or REPLENISH

		for (int compartmentIdx = amountsOfRoute.length - 1; compartmentIdx >= 0; compartmentIdx--) {
			// If replenishment is not allowed, ignore rest
			if (currentNode.getSiteType() == SiteType.REPLENISH && !model.getCompartments()[compartmentIdx].isReplenished()) {
				continue;
			}

			if (currentNode.getSiteType() == SiteType.REPLENISH) {
				amountsOfRoute[compartmentIdx].replenish();
			} else {
				amountsOfRoute[compartmentIdx].clear();
			}

			// Init delivery amount on the route
			if(!routeInfos.containsKey(currentNode))
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT, "Could not find route infos for depot id " + currentNode.getDepotId());

			// The Compartment object searches itself for correct value from deliveryOfRoute.getAmounts()
			Amount deliveryOfRoute = Amount.ofDelivery(routeInfos.get(currentNode));
			amountsOfRoute[compartmentIdx].addAmount(deliveryOfRoute.getAmounts(), LoadType.PRELOAD_AT_DEPOT);
		}
	}

	public float getLoadingServiceTimeAtDepot() {
		float maxServiceTime = 0;
		for (int compartmentIdx = 0; compartmentIdx < amountsOfRoute.length; compartmentIdx++) {
			maxServiceTime = Math.max(maxServiceTime, routeInfos.get(currentDepot)[compartmentIdx].getLoadingServiceTime());
		}
		return maxServiceTime;
	}

	public float getUnLoadingServiceTimeAtDepot() {
		float maxServiceTime = 0;
		for (int compartmentIdx = 0; compartmentIdx < amountsOfRoute.length; compartmentIdx++) {
			maxServiceTime = Math.max(maxServiceTime, routeInfos.get(currentDepot)[compartmentIdx].getUnLoadingServiceTime());
		}
		return maxServiceTime;
	}

	public void addToTime(float addedTime) {
		routeVar[TIME] += addedTime;
	}

	public void addToDuration(float addedDuration) {
		routeVar[DURATION] += addedDuration;
	}

	public int getMaxGlobalNodeIdx() {
		return maxGlobalNodeIdx;
	}

	public void setMaxGlobalNodeIdx(int maxGlobalNodeIdx) {
		this.maxGlobalNodeIdx = maxGlobalNodeIdx;
	}

	public float[] getRouteVar() {
		return routeVar;
	}

	public CompartmentLoad[] getAmountsOfRoute() {
		return amountsOfRoute;
	}

	public void setAmountArr(CompartmentLoad[] amountArr) {
		this.amountsOfRoute = amountArr;
	}

	public int[] getBlockPresetArr() {
		return blockPresetArr;
	}

	public void setBlockPresetArr(int[] blockPresetArr) {
		this.blockPresetArr = blockPresetArr;
	}

	public void setAvailablePresetCountArr(int[] availablePresetCountArr) {
		this.availablePresetCountArr = availablePresetCountArr;
	}

	public void setFoundPresetCountArr(int[] foundPresetCountArr) {
		this.foundPresetCountArr = foundPresetCountArr;
	}

	public void setLastPresetSequenceRankArr(int[] lastPresetSequenceRankArr) {
		this.lastPresetSequenceRankArr = lastPresetSequenceRankArr;
	}

	public void setPresetRoutingBlackList(boolean[] presetRoutingBlackList) {
		this.presetRoutingBlackList = presetRoutingBlackList;
	}

	public void setPresetRoutingNodeList(boolean[] presetRoutingNodeList) {
		this.presetRoutingNodeList = presetRoutingNodeList;
	}

	public void setRouteInfos(Map<Node, RouteInfo[]> routeDepotServiceMap) {
		this.routeInfos = routeDepotServiceMap;
	}

	public void setCurrentDepot(Node newDepot) {
		this.currentDepot = newDepot;
		this.currentNode = newDepot;
	}

	public Node getCurrentNode() {
		return currentNode;
	}

	public void setCurrentNode(Node currentNode) {
		this.currentNode = currentNode;
	}

	public Node getLastNode() {
		return lastNode;
	}

	public float[] getLastDrivenDistance() {
		return lastDrivenDistance;
	}

	public XFVRPModel getModel() {
		return model;
	}

	public float getDrivingTime() {
		return routeVar[DRIVING_TIME];
	}

	public void setTimeToTimeWindow(float[] timeWindow) {
		routeVar[TIME] = Math.max(routeVar[TIME], timeWindow[0]);
	}

	public void addDelayWithTimeWindow(float[] timeWindow) {
		routeVar[DELAY] += (routeVar[TIME] - timeWindow[1] > 0) ? routeVar[TIME] - timeWindow[1] : 0;
	}

	public float getWaitingTimeAtTimeWindow(float[] timeWindow) {
		return (routeVar[TIME] < timeWindow[0]) ? timeWindow[0] - routeVar[TIME] : 0;
	}

	public int checkCapacities() {
		Vehicle vehicle = model.getVehicle();

		int sum = 0;
		for (int compartment = 0; compartment < model.getCompartments().length; compartment++) {
			sum += amountsOfRoute[compartment].checkCapacity(vehicle.getCapacity());
		}

		return sum;
	}

	public int setAndCheckPresetSequence(int blockIndex) {
		int penalty = 0;
		if(lastPresetSequenceRankArr[blockIndex] > currentNode.getPresetBlockRank())
			penalty = 1;
		lastPresetSequenceRankArr[blockIndex] = currentNode.getPresetBlockRank();

		return penalty;
	}

	public void setPresetRouting() {
		if(currentNode.getSiteType() == SiteType.CUSTOMER) {
			int[] routingBlackList = currentNode.getPresetRoutingBlackList();
			for (int i = 0, routingBlackListLength = routingBlackList.length; i < routingBlackListLength; i++) {
				presetRoutingBlackList[routingBlackList[i]] = true;
			}
			presetRoutingNodeList[currentNode.getGlobalIdx()] = true;
		}
	}

	public int checkPresetPosition() {
		// curr node must have a preset block position greater than 1 (1 is first position)
		if(currentNode.getPresetBlockPos() > 1)
			// AND must have a dedicated block
			if (currentNode.getPresetBlockIdx() > BlockNameConverter.DEFAULT_BLOCK_IDX)
				// If curr node and last node have same block
				if (currentNode.getPresetBlockIdx() == lastNode.getPresetBlockIdx()) {
					// than diff of positions must be 1
					if (currentNode.getPresetBlockPos() - lastNode.getPresetBlockPos() != 1)
						return 1;
				} else
					return 1;

		return 0;
	}

	public int checkPresetDepot() {
		if(currentNode.getPresetDepotList().size() > 0 && !currentNode.isInPresetDepotList(currentDepot.getGlobalIdx()))
			return 1;

		return 0;
	}

	public int setAndCheckPresetBlock(int blockIndex) {
		int penalty = 0;
		// If route index for this block is not initialized, then set route index
		if(blockPresetArr[blockIndex] == -1)
			blockPresetArr[blockIndex] = (int)routeVar[ROUTE_IDX];
			// If route index of this block is different from current route index, failure
			// because all nodes of one block must be on one route
		else if(blockPresetArr[blockIndex] != (int)routeVar[ROUTE_IDX])
			penalty = 1;

		// Save number of seen nodes for this block
		foundPresetCountArr[blockIndex]++;

		return penalty;
	}

	public int checkPresetBlackList() {
		for (int j = 0; j < presetRoutingBlackList.length; j++) {
			if(presetRoutingBlackList[j] & presetRoutingNodeList[j]) {
				return 1;
			}
		}

		return 0;
	}

	public int checkPresetBlockCount() {
		int penalty = 0;
		for (int j = 0; j < foundPresetCountArr.length; j++)
			if(foundPresetCountArr[j] > 0 && availablePresetCountArr[j] - foundPresetCountArr[j] > 0)
				penalty += availablePresetCountArr[j] - foundPresetCountArr[j];

		return penalty;
	}

	public float getNbrOfStops() {
		return routeVar[NBR_OF_STOPS];
	}

	public float getDuration() {
		return routeVar[DURATION];
	}

	public float getDelay() {
		return routeVar[DELAY];
	}

	public float getLength() {
		return routeVar[LENGTH];
	}

	public RouteInfo[] getRouteInfo() {
		return routeInfos.get(currentNode);
	}
}
