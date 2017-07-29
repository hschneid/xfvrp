package xf.xfvrp.base;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import xf.xfvrp.base.preset.BlockNameConverter;

/** 
 * Copyright (c) 2012-present Holger Schneider
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

	/** Efficient Load parameter **/
	private final String shipID;
	private final int nbrOfPackages;
	private final int heightOfPackage;
	private final int widthOfPackage;
	private final int lengthOfPackage;
	private final float weightOfPackage;
	private final float loadBearingOfPackage;
	private final int stackingGroupOfPackage;
	private final int containerTypeOfPackage;

	/** Preset parameter **/
	private int presetBlockIdx = BlockNameConverter.UNDEF_BLOCK_IDX;
	private int presetBlockPos;
	private int presetBlockRank;

	private final Set<Integer> presetBlockVehicleList = new HashSet<>();
	/** A list of depot node ids (global idx), where this customer must be allocated to one these depots. **/
	private final Set<Integer> presetDepotList = new HashSet<>();
	/** A list of node ids, which must not be routed with this node. **/
	private final Set<Integer> presetRoutingBlackList = new HashSet<>();
	/** If customer is invalid for whole route plan, the reason is written to invalid states **/

	private InvalidReason invalidReason = InvalidReason.NONE;

	private String invalidArguments = "";

	public Node() {
		externID = "";
		globalIdx = 0;
		
		demand = new float[3];

		loadType = LoadType.DELIVERY;
		xlong = 0;
		ylat = 0;
		timeWindowArr = new float[0][0];
		serviceTime = 0;
		serviceTimeForSite = 0;

		shipID = "";
		nbrOfPackages = 0;
		heightOfPackage = 0;
		widthOfPackage = 0;
		lengthOfPackage = 0;
		weightOfPackage = 0;
		loadBearingOfPackage = 0;
		stackingGroupOfPackage = 0;
		containerTypeOfPackage = 0;
	}

	/**
	 * Node constructor for use within Efficient Load
	 * All external data of a node are set here.
	 * Except for internal index, depotId and geoId.	
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
			int presetBlockPos,
			int presetBlockRank,
			String shipID,
			int nbrOfPackages,
			int heightOfPackage,
			int widthOfPackage,
			int lengthOfPackage,
			float weightOfPackage,
			float loadBearingOfPackage,
			int stackingGroupOfPackage,
			int containerTypeOfPackage			
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
		this.presetBlockPos = presetBlockPos;
		this.presetBlockRank = presetBlockRank;
		this.shipID = shipID;
		this.nbrOfPackages = nbrOfPackages;
		this.heightOfPackage = heightOfPackage;
		this.widthOfPackage = widthOfPackage;
		this.lengthOfPackage = lengthOfPackage;
		this.weightOfPackage = weightOfPackage;
		this.loadBearingOfPackage = loadBearingOfPackage;
		this.stackingGroupOfPackage = stackingGroupOfPackage;
		this.containerTypeOfPackage = containerTypeOfPackage;
	}

	/**
	 * Deep copy of this node
	 * 
	 * @return
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

	/**
	 * @return
	 */
	public int getIdx() {
		return idx;
	}

	@Override
	public String toString() {
		return this.externID+"";
	}

	/**
	 * 
	 * @param idx
	 */
	public void setIdx(int idx) {
		this.idx = idx;
	}

	/**
	 * 
	 * @return
	 */
	public int getGlobalIdx() {
		return globalIdx;
	}

	/**
	 * 
	 * @param geoId
	 */
	public void setGeoId(int geoId) {
		this.geoId = geoId;
	}

	/**
	 * 
	 * @return
	 */
	public String getExternID() {
		return externID;
	}

	/**
	 * 
	 * @return
	 */
	public float getServiceTime() {
		return serviceTime;
	}

	/**
	 * 
	 * @return
	 */
	public LoadType getLoadType() {
		return loadType;
	}

	/**
	 * 
	 * @return
	 */
	public int getGeoId() {
		return geoId;
	}

	/**
	 * Returns the time window of the node. For multiple time windows the last valid
	 * time window is searched and returned. If the given time is beyond the last
	 * valid time, the last time of the window is returned.
	 * 
	 * @param time 
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

	/**
	 * 
	 * @return
	 */
	public int getDepotId() {
		return depotId ;
	}

	/**
	 * 
	 * @param depotId
	 */
	public void setDepotId(int depotId) {
		this.depotId = depotId;
	}

	/**
	 * @return the shipID
	 */
	public final String getShipID() {
		return shipID;
	}

	/**
	 * @return the nbrOfPackages
	 */
	public final int getNbrOfPackages() {
		return nbrOfPackages;
	}

	/**
	 * @return the heightOfPackage
	 */
	public final int getHeightOfPackage() {
		return heightOfPackage;
	}

	/**
	 * @return the widthOfPackage
	 */
	public final int getWidthOfPackage() {
		return widthOfPackage;
	}

	/**
	 * @return the lengthOfPackage
	 */
	public final int getLengthOfPackage() {
		return lengthOfPackage;
	}

	/**
	 * @return the weightOfPackage
	 */
	public final float getWeightOfPackage() {
		return weightOfPackage;
	}

	/**
	 * @return the loadBearingOfPackage
	 */
	public final float getLoadBearingOfPackage() {
		return loadBearingOfPackage;
	}

	/**
	 * @return the stackingGroupOfPackage
	 */
	public final int getStackingGroupOfPackage() {
		return stackingGroupOfPackage;
	}

	/**
	 * @return the containerTypeOfPackage
	 */
	public final int getContainerTypeOfPackage() {
		return containerTypeOfPackage;
	}

	/**
	 * 
	 * @param blockIdx
	 */
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

	/**
	 * @return the presetBlockVehicleList
	 */
	public Set<Integer> getPresetBlockVehicleList() {
		return presetBlockVehicleList;
	}
	
	public void addPresetVehicle(int vehicleIdx) {
		presetBlockVehicleList.add(vehicleIdx);
	}

	/**
	 * 
	 * @return
	 */
	public float getServiceTimeForSite() {
		return serviceTimeForSite;
	}

	/**
	 * Returns list with node ids which are not allowed to be
	 * allocated with this node on one route together.
	 * 
	 * @return node id black list for routing on one route
	 */
	public Set<Integer> getPresetRoutingBlackList() {
		return presetRoutingBlackList;
	}

	/**
	 * 
	 * @param globalIdx
	 */
	public void addToBlacklist(int globalIdx){
		presetRoutingBlackList.add(globalIdx);
	}

	/**
	 * 
	 * @return
	 */
	public int getShipmentIdx() {
		return shipmentIdx;
	}

	/**
	 * 
	 * @param shipmentIdx
	 */
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

	/**
	 * 
	 * @param r The reason why this customer leads to a invalid route plan
	 */
	public void setInvalidReason(InvalidReason r) {
		setInvalidReason(r, "");
	}

	/**
	 * 
	 * @return
	 */
	public String getInvalidArguments() {
		return invalidArguments;
	}

	/**
	 * 
	 * @return
	 */
	public InvalidReason getInvalidReason() {
		return invalidReason;
	}

	/**
	 * @return
	 */
	public Set<Integer> getPresetDepotList() {
		return presetDepotList;
	}

	/**
	 * 
	 * @param globalIdx
	 * @return
	 */
	public boolean isInPresetDepotList(int globalIdx){
		return presetDepotList.contains(globalIdx);
	}

	/**
	 * 
	 * @param globalIdx
	 */
	public void addPresetDepot(int globalIdx) {
		presetDepotList.add(globalIdx);
	}

	/**
	 * 
	 * @param val
	 */
	public void setDemand(float val) {
		this.demand[0] = val;
	}
}
