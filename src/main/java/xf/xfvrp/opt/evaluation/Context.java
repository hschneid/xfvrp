package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.*;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.BlockPositionConverter;

import java.util.Arrays;
import java.util.Map;

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
	private Node currentNode;
	private Node lastNode;

	private float[] lastDrivenDistance;

	private XFVRPModel model;

	public Context() {
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

	public int createNewRoute(Node newDepot) throws XFVRPException {
		routeVar[ROUTE_IDX]++;

		setCurrentDepot(newDepot);

		routeVar[DRIVING_TIME] = 0;
		routeVar[NBR_OF_STOPS] = 0;
		int penalty = resetAmountsOfRoute();

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

	public int resetAmountsOfRoute() throws XFVRPException {
		// Reset amounts to zero only for compartments, where parameter is set to true
		if(currentNode.isCompartmentReplenished() != null && currentNode.getSiteType() == SiteType.REPLENISH) {
			for (int compartment = 0; compartment < getNbrOfCompartments(); compartment++) {
				if(currentNode.isCompartmentReplenished()[compartment]) {
					Arrays.fill(
							amountsOfRoute,
							compartment * CompartmentLoadType.NBR_OF_LOAD_TYPES,
							compartment * CompartmentLoadType.NBR_OF_LOAD_TYPES + CompartmentLoadType.NBR_OF_LOAD_TYPES,
							0
					);
				}
			}
		} else {
			Arrays.fill(amountsOfRoute, 0);
		}

		// TODO(Lars) If this leads to an error, then please raise a bug ticket with concrete example (maybe test)
		// Init delivery amount on the route:
		// Here it is checked, that there is a pre-collection of amounts for this depot
		// For example: This fails, if currentNode is no depot, which means, that start of
		// route is no depot. This is a critical situation, because somewhere the route is
		// malformed initialized.
		if (!routeInfos.containsKey(currentNode)) {
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT,
					"Could not find route infos for depot id " + currentNode.getDepotId());
		}

		// If there are mixed compartments, consider delivery amount at starting depot
		RouteInfo routeInfo = routeInfos.get(currentNode);
		Amount deliveryOfRoute = routeInfo.getDeliveryAmount();
		// With old code, this would suffer...
		// if (deliveryOfRoute.hasAmount()) {
		if (deliveryOfRoute.hasAmount() && routeInfo.getPickupAmount().hasAmount()) {
			for (int compartment = 0; compartment < getNbrOfCompartments(); compartment++) {
				int mixedIndex = compartment * CompartmentLoadType.NBR_OF_LOAD_TYPES + CompartmentLoadType.MIXED.index();
				amountsOfRoute[mixedIndex] += deliveryOfRoute.getAmounts()[compartment];
			}
			return checkCapacities();
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

	public int getMaxGlobalNodeIdx() {
		return maxGlobalNodeIdx;
	}

	public void setMaxGlobalNodeIdx(int maxGlobalNodeIdx) {
		this.maxGlobalNodeIdx = maxGlobalNodeIdx;
	}

	public float[] getRouteVar() {
		return routeVar;
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

	public void setRouteInfos(Map<Node, RouteInfo> routeDepotServiceMap) {
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
		for (int compartment = 0; compartment < getNbrOfCompartments(); compartment++) {
			int compartmentIdx = compartment * CompartmentLoadType.NBR_OF_LOAD_TYPES;

			int pickupIdx = compartmentIdx + CompartmentLoadType.PICKUP.index();
			int deliveryIdx = compartmentIdx + CompartmentLoadType.DELIVERY.index();
			int mixedIndex = compartmentIdx + CompartmentLoadType.MIXED.index();

			// TODO(Lars): That is exactly the code, which considers if routes have mixed load or single load. In single load, the mixed load must not be checked, because this would lead to wrong interpretation.
//			if(amountsOfRoute[pickupIdx] == 0 && amountsOfRoute[deliveryIdx] > 0) {
//				sum += (int) Math.ceil(Math.max(0, amountsOfRoute[deliveryIdx] - vehicle.capacity[deliveryIdx]));
//			} else if(amountsOfRoute[pickupIdx] > 0 && amountsOfRoute[deliveryIdx] == 0) {
//				sum += (int) Math.ceil(Math.max(0, amountsOfRoute[pickupIdx] - vehicle.capacity[pickupIdx]));
//			} else if(amountsOfRoute[pickupIdx] > 0 && amountsOfRoute[deliveryIdx] > 0) {
//				sum += (int) Math.ceil(Math.max(0, amountsOfRoute[compartmentIdx + CompartmentLoadType.MIXED.index()] - vehicle.capacity[compartmentIdx + CompartmentLoadType.MIXED.index()]));
//			}

			float[] capacity = vehicle.getCapacity();
			sum += (int) Math.ceil(Math.max(0, amountsOfRoute[deliveryIdx] - capacity[deliveryIdx]));
			sum += (int) Math.ceil(Math.max(0, amountsOfRoute[pickupIdx] - capacity[pickupIdx]));
			sum += (int) Math.ceil(Math.max(0, amountsOfRoute[mixedIndex] - capacity[mixedIndex]));

			System.out.println("X "+compartment+" "+
					amountsOfRoute[deliveryIdx]+","+
					amountsOfRoute[pickupIdx] +","+
					amountsOfRoute[mixedIndex] + "|"+
					capacity[deliveryIdx]+","+
					capacity[pickupIdx] +","+
					capacity[mixedIndex] + "="+sum
			);
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

	public void setModel(XFVRPModel model) {
		this.model = model;
	}

	public RouteInfo getRouteInfo() {
		return routeInfos.get(currentNode);
	}

	public int getNbrOfCompartments() {
		return amountsOfRoute.length / CompartmentLoadType.NBR_OF_LOAD_TYPES;
	}
}
