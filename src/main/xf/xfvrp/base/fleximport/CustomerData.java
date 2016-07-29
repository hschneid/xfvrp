package xf.xfvrp.base.fleximport;

import java.util.HashSet;
import java.util.Set;

import xf.xfvrp.base.LoadType;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * The CustomerData is the interface to external user
 * of the XFVRP suite. Each field in this class can be used
 * for later optimization. Not every combination of data leads
 * to a reasonable solution. So be aware of that.
 * 
 * CustomerData is also responsible for the flexi import. Hereby
 * a suite user can import data to the XFVRP suite adaptively. Not all
 * parameters has to be filled.
 * 
 * @author hschneid
 *
 */
public abstract class CustomerData extends DepotData {
	private static final long serialVersionUID = -2498257823778005561L;
	
	/** Basic XFVRP - parameter **/
	protected LoadType loadType = LoadType.PICKUP;
	protected float[] demand = new float[]{0, 0, 0};
	protected float serviceTime = 0;
	protected float serviceTimeForSite = 0;

	/** Efficient Load - parameter **/
	protected String shipID = "";
	protected int nbrOfPackages = 0;
	protected int heightOfPackage = 0;
	protected int widthOfPackage = 0;
	protected int lengthOfPackage = 0;
	protected float weightOfPackage = 0;
	protected float loadBearingOfPackage = Float.MAX_VALUE;
	protected int stackingGroupOfPackage = 0;
	protected int containerTypeOfPackage = 0;
	
	/** Preset parameter **/
	protected String presetBlockName = "";
	protected int presetBlockPos = -1;
	protected int presetBlockRank = 0;
	protected Set<String> presetBlockVehicleList = new HashSet<>();
	protected Set<String> presetDepotList = new HashSet<>();
	protected Set<String> presetRoutingBlackList = new HashSet<>();
	
	/**
	 * @param pdType the pdType to set
	 */
	public CustomerData setLoadType(LoadType pdType) {
		this.loadType = pdType;
		return this;
	}
	
	/**
	 * @param demand the demand to set
	 */
	public CustomerData setDemand(float[] demand) {
		this.demand = demand;
		return this;
	}
	
	/**
	 * @param demand the demand to set
	 */
	public CustomerData setDemand(float demand) {
		this.demand[0] = demand;
		return this;
	}

	/**
	 * @param demand the demand to set
	 */
	public CustomerData setDemand2(float demand) {
		this.demand[1] = demand;
		return this;
	}

	/**
	 * @param demand the demand to set
	 */
	public CustomerData setDemand3(float demand) {
		this.demand[2] = demand;
		return this;
	}
	
	/**
	 * @param serviceTime the serviceTime to set
	 */
	public CustomerData setServiceTime(float serviceTime) {
		this.serviceTime = serviceTime;
		return this;
	}
	
	/**
	 * @param shipID the shipID to set
	 */
	public CustomerData setShipID(String shipID) {
		this.shipID = shipID;
		return this;
	}
	
	/**
	 * @param nbrOfPackages the nbrOfPackages to set
	 */
	public CustomerData setNbrOfPackages(int nbrOfPackages) {
		this.nbrOfPackages = nbrOfPackages;
		return this;
	}
	
	/**
	 * @param heightOfPackage the heightOfPackage to set
	 */
	public CustomerData setHeightOfPackage(int heightOfPackage) {
		this.heightOfPackage = heightOfPackage;
		return this;
	}
	
	/**
	 * @param widthOfPackage the widthOfPackage to set
	 */
	public CustomerData setWidthOfPackage(int widthOfPackage) {
		this.widthOfPackage = widthOfPackage;
		return this;
	}
	
	/**
	 * @param lengthOfPackage the lengthOfPackage to set
	 */
	public CustomerData setLengthOfPackage(int lengthOfPackage) {
		this.lengthOfPackage = lengthOfPackage;
		return this;
	}
	
	/**
	 * @param weightOfPackage the weightOfPackage to set
	 */
	public CustomerData setWeightOfPackage(float weightOfPackage) {
		this.weightOfPackage = weightOfPackage;
		return this;
	}
	
	/**
	 * @param loadBearingOfPackage the loadBearingOfPackage to set
	 */
	public CustomerData setLoadBearingOfPackage(float loadBearingOfPackage) {
		this.loadBearingOfPackage = loadBearingOfPackage;
		return this;
	}
	
	/**
	 * @param stackingGroupOfPackage the stackingGroupOfPackage to set
	 */
	public CustomerData setStackingGroupOfPackage(int stackingGroupOfPackage) {
		this.stackingGroupOfPackage = stackingGroupOfPackage;
		return this;
	}
	
	/**
	 * @param containerTypeOfPackage the containerTypeOfPackage to set
	 */
	public CustomerData setContainerTypeOfPackage(int containerTypeOfPackage) {
		this.containerTypeOfPackage = containerTypeOfPackage;
		return this;
	}

	/**
	 * @param externID the externID to set
	 */
	@Override
	public CustomerData setExternID(String externID) {
		this.externID = externID;
		return this;
	}
	
	/**
	 * @param geoId the geoId to set
	 */
	@Override
	public CustomerData setGeoId(int geoId) {
		this.geoId = geoId;
		return this;
	}
	
	/**
	 * @param xlong the xlong to set
	 */
	@Override
	public CustomerData setXlong(float xlong) {
		this.xlong = xlong;
		return this;
	}
	
	/**
	 * @param ylat the ylat to set
	 */
	@Override
	public CustomerData setYlat(float ylat) {
		this.ylat = ylat;
		return this;
	}
	
	/**
	 * @param timeWindow the timeWindow to set
	 */
	public CustomerData setTimeWindow(float[] timeWindow) {
		this.timeWindowList.add(timeWindow);
		return this;
	}
	
	/**
	 * @param open1 the open1 to set
	 */
	@Override
	public CustomerData setOpen1(float open1) {
		this.open1 = open1;
		return this;
	}

	/**
	 * @param close1 the close1 to set
	 */
	@Override
	public CustomerData setClose1(float close1) {
		this.close1 = close1;
		return this;
	}

	/**
	 * @param open2 the open2 to set
	 */
	@Override
	public CustomerData setOpen2(float open2) {
		this.open2 = open2;
		return this;
	}

	/**
	 * @param close2 the close2 to set
	 */
	@Override
	public CustomerData setClose2(float close2) {
		this.close2 = close2;
		return this;
	}
	
	/**
	 * 
	 * @param blockName
	 * @return
	 */
	public CustomerData setPresetBlockName(String blockName) {
		this.presetBlockName = blockName;
		return this;
	}
	
	/**
	 * 
	 * @param blockedVehicleList
	 * @return
	 */
	public CustomerData setPresetBlockVehicleList(Set<String> blockedVehicleList) {
		this.presetBlockVehicleList = blockedVehicleList;
		return this;
	}

	/**
	 * 
	 * @param pos
	 * @return
	 */
	public CustomerData setPresetBlockPos(int pos) {
		this.presetBlockPos = pos;
		return this;
	}
	

	/**
	 * 
	 * @param pos
	 * @return
	 */
	public CustomerData setPresetBlockRank(int rank) {
		this.presetBlockRank = rank;
		return this;
	}
	
	/**
	 * 
	 * @param serviceTimeForSite
	 */
	public CustomerData setServiceTimeForSite(float serviceTimeForSite) {
		this.serviceTimeForSite = serviceTimeForSite;
		return this;
	}
	
	/**
	 * 
	 * @param blackListedNodeSet
	 * @return
	 */
	public CustomerData setPresetRoutingBlackList(Set<String> blackListedNodeSet) {
		presetRoutingBlackList.addAll(blackListedNodeSet);
		return this;
	}

	/**
	 * 
	 * @param depotSet
	 * @return
	 */
	public CustomerData setPresetDepotList(Set<String> depotSet) {
		presetDepotList.addAll(depotSet);
		return this;
	}
}
