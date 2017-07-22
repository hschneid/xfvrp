package xf.xfvrp.opt.evaluation;

import java.util.Arrays;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.preset.BlockNameConverter;

public class Context {

	private static final int ROUTE_IDX = 0;
	private static final int LENGTH = 1;
	private static final int DELAY = 2;
	private static final int DURATION = 3;
	private static final int NBR_OF_STOPS = 4;
	private static final int TIME = 5; // Time incl. time windows, driving time, service time, waiting time, rest times
	private static final int DRIVING_TIME = 6; // only driving time

	// Active nodes for evalution are true, duplicates or empty routes are false
	private boolean[] activeNodes;

	// Variables
	private int maxGlobalNodeIdx;
	private float[] routeVar;
	private float[] amountsOfRoute;

	private int[] blockPresetArr;
	private int[] availablePresetCountArr;
	private int[] foundPresetCountArr;
	private int[] lastPresetSequenceRankArr;
	private boolean[] presetRoutingBlackList;
	private boolean[] presetRoutingNodeList;

	// Service times at the depot for amount on the route
	private float[][] routeDepotServiceMap;

	private Node currentDepot;
	private Node lastReplenishNode;
	private Node currentNode;
	private Node lastNode;

	private float[] lastDrivenDistance;

	public Context() {
		routeVar = new float[7];
		routeVar[ROUTE_IDX] = -1;
	}

	public void setNextNode(Node newCurrentNode) {
		this.lastNode = this.currentNode;
		this.currentNode = newCurrentNode;

		// Replace ending depot with starting depot (in multi depot problems its nessecary)
		if(currentNode.getSiteType() == SiteType.DEPOT)
			this.currentNode = this.currentDepot; 
	}

	public float[] getFittingTimeWindow() {
		return this.currentNode.getTimeWindow(routeVar[TIME]);
	}

	public boolean isNodeActive(int nodeIdx) {
		return activeNodes[nodeIdx];
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

	public void resetDrivingTime(Vehicle vehicle) {
		routeVar[DRIVING_TIME] = 0;
		routeVar[TIME] += vehicle.waitingTimeBetweenShifts;
		routeVar[DURATION] += vehicle.waitingTimeBetweenShifts;
	}

	public void createNewRoute(Node currentNode, float earliestDepartureTime, float loadingTimeAtDepot) {
		routeVar[ROUTE_IDX]++;

		routeVar[TIME] = 
				Math.max(
						this.currentNode.getTimeWindow(0)[0] + loadingTimeAtDepot, 
						earliestDepartureTime
						);

		routeVar[DRIVING_TIME] = 0;
		routeVar[NBR_OF_STOPS] = 0;
		resetAmountsOfRoute();

		routeVar[LENGTH] = 0;
		routeVar[DURATION] = loadingTimeAtDepot;
		routeVar[DELAY] = 0;

		setCurrentDepot(currentNode);

		lastPresetSequenceRankArr[BlockNameConverter.DEFAULT_BLOCK_IDX] = Integer.MIN_VALUE;
		Arrays.fill(presetRoutingBlackList, false);
		Arrays.fill(presetRoutingNodeList, false);
	}

	public void resetAmountsOfRoute() {
		Arrays.fill(amountsOfRoute, 0);
	}

	public float getLoadingTimesAtDepotForCurrentRoute() {
		return routeDepotServiceMap[(int)routeVar[ROUTE_IDX]][0];
	}

	public float getUnLoadingTimesAtDepotForCurrentRoute() {
		return routeDepotServiceMap[(int)routeVar[ROUTE_IDX]][1];
	}

	public void addToTime(float addedTime) {
		routeVar[TIME] += addedTime;
	}

	public void addToDuration(float addedDuration) {
		routeVar[DURATION] += addedDuration;
	}

	public boolean[] getActiveNodes() {
		return activeNodes;
	}

	public void setActiveNodes(boolean[] activeNodes) {
		this.activeNodes = activeNodes;
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

	public void setRouteVar(float[] routeVar) {
		this.routeVar = routeVar;
	}

	public float[] getAmountsOfRoute() {
		return amountsOfRoute;
	}

	public void setAmountArr(float[] amountArr) {
		this.amountsOfRoute = amountArr;
	}

	public int[] getBlockPresetArr() {
		return blockPresetArr;
	}

	public void setBlockPresetArr(int[] blockPresetArr) {
		this.blockPresetArr = blockPresetArr;
	}

	public int[] getAvailablePresetCountArr() {
		return availablePresetCountArr;
	}

	public void setAvailablePresetCountArr(int[] availablePresetCountArr) {
		this.availablePresetCountArr = availablePresetCountArr;
	}

	public int[] getFoundPresetCountArr() {
		return foundPresetCountArr;
	}

	public void setFoundPresetCountArr(int[] foundPresetCountArr) {
		this.foundPresetCountArr = foundPresetCountArr;
	}

	public int[] getLastPresetSequenceRankArr() {
		return lastPresetSequenceRankArr;
	}

	public void setLastPresetSequenceRankArr(int[] lastPresetSequenceRankArr) {
		this.lastPresetSequenceRankArr = lastPresetSequenceRankArr;
	}

	public boolean[] getPresetRoutingBlackList() {
		return presetRoutingBlackList;
	}

	public void setPresetRoutingBlackList(boolean[] presetRoutingBlackList) {
		this.presetRoutingBlackList = presetRoutingBlackList;
	}

	public boolean[] getPresetRoutingNodeList() {
		return presetRoutingNodeList;
	}

	public void setPresetRoutingNodeList(boolean[] presetRoutingNodeList) {
		this.presetRoutingNodeList = presetRoutingNodeList;
	}

	public float[][] getRouteDepotServiceMap() {
		return routeDepotServiceMap;
	}

	public void setRouteDepotServiceMap(float[][] routeDepotServiceMap) {
		this.routeDepotServiceMap = routeDepotServiceMap;
	}

	public Node getLastDepot() {
		return currentDepot;
	}

	public void setCurrentDepot(Node lastDepot) {
		this.currentDepot = lastDepot;
	}

	public Node getLastReplenishNode() {
		return lastReplenishNode;
	}

	public void setLastReplenishNode(Node lastReplenishNode) {
		this.lastReplenishNode = lastReplenishNode;
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

	public void setLastNode(Node lastNode) {
		this.lastNode = lastNode;
	}

	public float[] getLastDrivenDistance() {
		return lastDrivenDistance;
	}

	public void setLastDrivenDistance(float[] lastDrivenDistance) {
		this.lastDrivenDistance = lastDrivenDistance;
	}

	public Node getCurrentDepot() {
		return currentDepot;
	}

	public float getDrivingTime() {
		return routeVar[DRIVING_TIME];
	}

	public void setTimeToTimeWindow(float[] timeWindow) {
		routeVar[TIME] = (routeVar[TIME] > timeWindow[0]) ? routeVar[TIME] : timeWindow[0];
	}

	public void addDelayWithTimeWindow(float[] timeWindow) {
		routeVar[DELAY] += (routeVar[TIME] - timeWindow[1] > 0) ? routeVar[TIME] - timeWindow[1] : 0;		
	}

	public float getWaitingTimeAtTimeWindow(float[] timeWindow) {
		return (routeVar[TIME] < timeWindow[0]) ? timeWindow[0] - routeVar[TIME] : 0;
	}

	public int checkCapacities(Vehicle v) {
		int penalty = 0;
		for (int i = 0; i < amountsOfRoute.length / 3; i++)
			for (int j = 0; j < 3; j++)
				penalty += Math.max(0, amountsOfRoute[i * 3 + j] - v.capacity[i]);

		return penalty;
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
			for (int bn : currentNode.getPresetRoutingBlackList())
				presetRoutingBlackList[bn] = true;
			presetRoutingNodeList[currentNode.getGlobalIdx()] = true;
		}		
	}

	public int checkPresetPosition() {
		if(currentNode.getPresetBlockIdx() > BlockNameConverter.DEFAULT_BLOCK_IDX)
			if (lastNode.getPresetBlockIdx() > BlockNameConverter.DEFAULT_BLOCK_IDX)
				if (currentNode.getPresetBlockIdx() == lastNode.getPresetBlockIdx())
					if (currentNode.getPresetBlockPos() >= 0 && lastNode.getPresetBlockPos() >= 0 && lastNode.getPresetBlockPos() > currentNode.getPresetBlockPos())
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
}
