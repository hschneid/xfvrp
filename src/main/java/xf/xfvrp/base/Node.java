package xf.xfvrp.base;

import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.BlockPositionConverter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** 
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Nodes are the basic structure of this optimization model. The route representation
 * relies on nodes. Depots and customers are both Node objects, whereas the site type differs
 * between the two types.
 * 
 * @author hschneid
 *
 */
public class Node implements Cloneable {

	/* External name of this order, must be unique */
	private String externID;

	/* Local index of the node during optimization. unique only in an optimization model */
	private int idx;

	/* Global unique index of the node over all optimization processes */
	private final int globalIdx;

	/* Unique index of a shipment of a pair of nodes. Shipment name is memorized in FlexiConverter. */
	private int shipmentIdx = -1;

	private int geoId = -1;

	private final LoadType loadType;
	private final float xlong, ylat;
	private SiteType siteType;

	private float[] demand;

	private final float[][] timeWindowArr;
	private final float serviceTime;
	private final float serviceTimeForSite;
	private int depotId = -1;
	private final String shipID;

	/** Preset parameter **/
	private int presetBlockIdx = BlockNameConverter.UNDEF_BLOCK_IDX;
	private int presetBlockPos = BlockPositionConverter.UNDEF_POSITION;
	private int presetBlockRank;

	private final Set<Integer> presetBlockVehicleList = new HashSet<>();
	/** A list of depot node ids (global idx), where this customer must be allocated to one these depots. **/
	private final Set<Integer> presetDepotList = new HashSet<>();
	/** A list of node ids (global idx), which must not be routed with this node. **/
	private int[] presetRoutingBlackList = new int[0];

	/** Only for depots **/
	private final int maxNbrOfRoutes;

	/** If customer is invalid for whole route plan, the reason is written to invalid states **/
	private InvalidReason invalidReason = InvalidReason.NONE;

	private String invalidArguments = "";

	/**
	 * Constructor, which is used manly for testing
	 */
	Node() {
		externID = "";
		globalIdx = 0;
		
		demand = new float[1];

		loadType = LoadType.DELIVERY;
		xlong = 0;
		ylat = 0;
		timeWindowArr = new float[0][0];
		serviceTime = 0;
		serviceTimeForSite = 0;

		shipID = "";

		maxNbrOfRoutes = Integer.MAX_VALUE;
	}

	/**
	 * Node constructor
	 * All external data of a node are set here.
	 */
	public Node(
			int globalIdx,
			String externID,
			SiteType siteType,
			float xlong,
			float ylat,
			int geoId,
			float[] demand,
			float[][] timeWindow,
			float serviceTime,
			float serviceTimeForSite,
			LoadType loadType,
			int presetBlockRank,
			String shipID,
			int maxNbrOfRoutes
			) {
		this.globalIdx = globalIdx;
		this.externID = externID;
		this.siteType = siteType;
		this.demand = demand;
		this.timeWindowArr = timeWindow;
		this.serviceTime = serviceTime;
		this.serviceTimeForSite = serviceTimeForSite;
		this.loadType = loadType;
		this.xlong = xlong;
		this.ylat = ylat;
		this.geoId = geoId;
		this.presetBlockRank = presetBlockRank;
		this.shipID = shipID;
		this.maxNbrOfRoutes = maxNbrOfRoutes;
	}

	/**
	 * Deep copy of this node
	 */
	public Node copy() {
		Node c = null;
		try {
			c = (Node) super.clone();
			c.demand = Arrays.copyOf(this.demand, this.demand.length);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return c;
	}

	/**
	 * @return the type
	 */
	public SiteType getSiteType() {
		return siteType;
	}

	public void setSiteType(SiteType t){
		this.siteType = t;
	}

	/**
	 * @return the xlong
	 */
	public float getXlong() {
		return xlong;
	}

	/**
	 * @return the ylat
	 */
	public float getYlat() {
		return ylat;
	}

	/**
	 * @return the demand
	 */
	public float[] getDemand() {
		return demand;
	}

	public int getIdx() {
		return idx;
	}

	@Override
	public String toString() {
		return this.externID+"";
	}

	/**
	 * @param idx - index in node array of current execution
	 */
	public void setIdx(int idx) {
		this.idx = idx;
	}

	/**
	 * @return global index of node - mostly used for meta-heuristics or distance matrix
	 */
	public int getGlobalIdx() {
		return globalIdx;
	}

	/**
	 * @param geoId - id in distance metrix
	 */
	public void setGeoId(int geoId) {
		this.geoId = geoId;
	}

	/**
	 * @return business id
	 */
	public String getExternID() {
		return externID;
	}

	public float getServiceTime() {
		return serviceTime;
	}

	public LoadType getLoadType() {
		return loadType;
	}

	public int getGeoId() {
		return geoId;
	}

	/**
	 * Returns the time window of the node. For multiple time windows the last valid
	 * time window is searched and returned. If the given time is beyond the last
	 * valid time, the last time of the window is returned.
	 * 
	 * @return time window that holds the given time
	 */
	public float[] getTimeWindow(float time) {
		if(timeWindowArr.length == 1)
			return timeWindowArr[0];

		// Choose first time window, that can hold the given time
		for (int i = 0; i < timeWindowArr.length; i++)
			if(time < timeWindowArr[i][1])
				return timeWindowArr[i];

		// If no time window can hold the given time, than return the last time window
		return timeWindowArr[timeWindowArr.length - 1];
	}

	public int getDepotId() {
		return depotId ;
	}

	public void setDepotId(int depotId) {
		this.depotId = depotId;
	}

	/**
	 * @return the shipID
	 */
	public final String getShipID() {
		return shipID;
	}

	public void setPresetBlockIdx(int blockIdx) {
		this.presetBlockIdx = blockIdx;
	}

	/**
	 * @return the presetBlockIdx
	 */
	public final int getPresetBlockIdx() {
		return presetBlockIdx;
	}

	/**
	 * @return the presetSequencePos
	 */
	public final int getPresetBlockPos() {
		return presetBlockPos;
	}

	public void setPresetBlockPos(int presetBlockPos) {
		this.presetBlockPos = presetBlockPos;
	}

	public int getPresetBlockRank() {
		return presetBlockRank;
	}

	public void setPresetBlockRank(int presetBlockRank) {
		this.presetBlockRank = presetBlockRank;
	}

	public Set<Integer> getPresetBlockVehicleList() {
		return presetBlockVehicleList;
	}
	
	public void addPresetVehicle(int vehicleIdx) {
		presetBlockVehicleList.add(vehicleIdx);
	}

	public float getServiceTimeForSite() {
		return serviceTimeForSite;
	}

	/**
	 * Returns list with node ids which are not allowed to be
	 * allocated with this node on one route together.
	 * 
	 * @return node id black list for routing on one route
	 */
	public int[] getPresetRoutingBlackList() {
		return presetRoutingBlackList;
	}

	public void setPresetRoutingBlackList(int[] presetRoutingBlackList) {
		this.presetRoutingBlackList = presetRoutingBlackList;
	}

	public int getShipmentIdx() {
		return shipmentIdx;
	}

	public void setShipmentIdx(int shipmentIdx) {
		this.shipmentIdx = shipmentIdx;
	}

	/**
	 * 
	 * @param r The reason why this customer leads to a invalid route plan
	 * @param a A description with more details of the invalid reason
	 */
	public void setInvalidReason(InvalidReason r, String a) {
		if (r != null && r.equals(InvalidReason.WRONG_VEHICLE_TYPE))
			return;
		this.invalidReason = r;
		this.invalidArguments = a;
	}

	public int getMaxNbrOfRoutes() {
		return maxNbrOfRoutes;
	}

	/**
	 * 
	 * @param reason The reason why this customer leads to a invalid route plan
	 */
	public void setInvalidReason(InvalidReason reason) {
		setInvalidReason(reason, "");
	}

	public String getInvalidArguments() {
		return invalidArguments;
	}

	public InvalidReason getInvalidReason() {
		return invalidReason;
	}

	public Set<Integer> getPresetDepotList() {
		return presetDepotList;
	}

	public boolean isInPresetDepotList(int globalIdx){
		return presetDepotList.contains(globalIdx);
	}

	public void addPresetDepot(int globalIdx) {
		presetDepotList.add(globalIdx);
	}

	public void setDemand(float val) {
		this.demand[0] = val;
	}

	public void setDemands(float[] demands) {
		this.demand = demands;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Node node = (Node) o;
		return idx == node.idx && globalIdx == node.globalIdx && externID.equals(node.externID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(externID, idx, globalIdx);
	}
}
