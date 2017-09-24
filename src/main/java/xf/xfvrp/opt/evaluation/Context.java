package xf.xfvrp.opt.evaluation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.BlockPositionConverter;

public class Context {

	private static final int ROUTE_IDX = 0;
	private static final int LENGTH = 1;
	private static final int DELAY = 2;
	private static final int DURATION = 3;
	private static final int NBR_OF_STOPS = 4;
	private static final int TIME = 5; // Time incl. time windows, driving time, service time, waiting time, rest times
	private static final int DRIVING_TIME = 6; // only driving time

	// Active nodes for evalution are true, duplicates or empty routes are false
	private List<Node> activeNodes;

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

	// Pre-evaluated infos of a route (service times and amounts)
	private Map<Node, RouteInfo> routeInfos;

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

		// Replace ending depot with starting depot (in multi-depot problems its necessary)
		if(currentNode.getSiteType() == SiteType.DEPOT)
			this.currentNode = this.currentDepot; 
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

	public void resetDrivingTime(Vehicle vehicle) {
		routeVar[DRIVING_TIME] = 0;
		routeVar[TIME] += vehicle.waitingTimeBetweenShifts;
		routeVar[DURATION] += vehicle.waitingTimeBetweenShifts;
	}

	public int createNewRoute(Node newDepot, Vehicle vehicle) {
		routeVar[ROUTE_IDX]++;

		setCurrentDepot(newDepot);

		routeVar[DRIVING_TIME] = 0;
		routeVar[NBR_OF_STOPS] = 0;
		int penalty = resetAmountsOfRoute(vehicle);

		routeVar[LENGTH] = 0;
		routeVar[DELAY] = 0;

		lastPresetSequenceRankArr[BlockNameConverter.DEFAULT_BLOCK_IDX] = Integer.MIN_VALUE;
		Arrays.fill(presetRoutingBlackList, false);
		Arrays.fill(presetRoutingNodeList, false);

		return penalty;
	}

	public void setDepartureTimeAtDepot(float earliestDepartureTime, float loadingTimeAtDepot) {
		routeVar[TIME] = 
				Math.max(
						this.currentDepot.getTimeWindow(0)[0] + loadingTimeAtDepot, 
						earliestDepartureTime
						);
		routeVar[DURATION] = loadingTimeAtDepot;
	}

	public int resetAmountsOfRoute(Vehicle vehicle) {
		Arrays.fill(amountsOfRoute, 0);

		Amount deliveryOfRoute = routeInfos.get(currentNode).getDeliveryAmount();

		if(deliveryOfRoute == null)
			throw new IllegalStateException("Could not find route infos for depot id " + currentNode.getDepotId());

		if(deliveryOfRoute.hasAmount()) {
			for (int i = 0; i < amountsOfRoute.length / 2; i++)
				amountsOfRoute[i * 2 + 0] = deliveryOfRoute.getAmounts()[i];

			return checkCapacities(vehicle);
		}

		return 0;
	}

	public float getLoadingServiceTimeAtDepot() {
		return routeInfos.get(currentDepot).getLoadingServiceTime();
	}

	public float getUnLoadingServiceTimeAtDepot() {
		return routeInfos.get(currentDepot).getUnLoadingServiceTime();
	}

	public void addToTime(float addedTime) {
		routeVar[TIME] += addedTime;
	}

	public void addToDuration(float addedDuration) {
		routeVar[DURATION] += addedDuration;
	}

	public List<Node> getActiveNodes() {
		return activeNodes;
	}

	public void setActiveNodes(List<Node> activeNodes) {
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

	public void setRouteInfos(Map<Node, RouteInfo> routeDepotServiceMap) {
		this.routeInfos = routeDepotServiceMap;
	}

	public Node getLastDepot() {
		return currentDepot;
	}

	public void setCurrentDepot(Node newDepot) {
		this.currentDepot = newDepot;
		this.currentNode = newDepot;
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
		int sum = 0;
		for (int i = 0; i < amountsOfRoute.length / 2; i++) {
			// Common Load of Pickups and Deliveries
			sum += (int)Math.ceil(Math.max(0, (amountsOfRoute[i * 2 + 0] + amountsOfRoute[i * 2 + 1]) - v.capacity[i]));
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
			for (int bn : currentNode.getPresetRoutingBlackList())
				presetRoutingBlackList[bn] = true;
			presetRoutingNodeList[currentNode.getGlobalIdx()] = true;
		}		
	}

	public int checkPresetPosition() {
		if (currentNode.getPresetBlockPos() > BlockPositionConverter.UNDEF_POSITION)
			// 1 is the first setted block position. The second block pos needs to be checked at first. 
			if(currentNode.getPresetBlockPos() > 1)
				if (currentNode.getPresetBlockIdx() > BlockNameConverter.DEFAULT_BLOCK_IDX)
					if (currentNode.getPresetBlockIdx() == lastNode.getPresetBlockIdx()) {
						if (lastNode.getPresetBlockPos() != currentNode.getPresetBlockPos() - 1)
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
}
