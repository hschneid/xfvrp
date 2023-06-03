package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;

import java.util.HashSet;
import java.util.Set;

/** 
 * Copyright (c) 2012-2023 Holger Schneider
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
public class CustomerData extends NodeData {
	
	/** Basic XFVRP - parameter **/
	protected LoadType loadType = LoadType.PICKUP;
	protected float[] demand = new float[]{0};
	protected float serviceTime = 0;
	protected float serviceTimeForSite = 0;
	protected String shipID = "";
	
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
	 * @param externID the externID to set
	 */
	public CustomerData setExternID(String externID) {
		this.externID = externID;
		return this;
	}
	
	/**
	 * @param geoId the geoId to set
	 */
	public CustomerData setGeoId(int geoId) {
		this.geoId = geoId;
		return this;
	}
	
	/**
	 * @param xlong the xlong to set
	 */
	public CustomerData setXlong(float xlong) {
		this.xlong = xlong;
		return this;
	}
	
	/**
	 * @param ylat the ylat to set
	 */
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
	public CustomerData setOpen1(float open1) {
		this.open1 = open1;
		return this;
	}

	/**
	 * @param close1 the close1 to set
	 */
	public CustomerData setClose1(float close1) {
		this.close1 = close1;
		return this;
	}

	/**
	 * @param open2 the open2 to set
	 */
	public CustomerData setOpen2(float open2) {
		this.open2 = open2;
		return this;
	}

	/**
	 * @param close2 the close2 to set
	 */
	public CustomerData setClose2(float close2) {
		this.close2 = close2;
		return this;
	}
	
	/**
	 * A customer can belong to a block. A block is identified by its block name.
	 * All customers of one block must be placed together on one route.
	 *
	 * If this parameter is not set, customers can be placed elsewhere.
	 * If there are too many customers in one block, then it is possible, that there
	 * is no feasible routing. Then all customers of block will be set to unplanned.
	 *
	 * This is handy, if customers mean actions on a route. If the actions have
	 * time windows for example or more complex compartment restrictions, then
	 * they can be modeled as multiple customers, which should take place at same
	 * route.
	 */
	public CustomerData setPresetBlockName(String blockName) {
		this.presetBlockName = blockName;
		return this;
	}
	
	/**
	 * A customer can be dedicated to a certain subset of vehicles. Each vehicle
	 * is identified by the vehicle name, which must fit to vehicle objects.
	 *
	 * If this parameter is not set, then all vehicles are possible.
	 * If given vehicle id is not available in data set, then this customer cannot be
	 * placed and will stay unplanned.
	 *
	 * This is handy for multi-vehicle problems, where certain customers cannot placed to
	 * every vehicle: like big/heavy boxes to small vehicles.
	 */
	public CustomerData setPresetBlockVehicleList(Set<String> blockedVehicleList) {
		this.presetBlockVehicleList = blockedVehicleList;
		return this;
	}

	/**
	 * A customer, which is dedicated to a block, can get a certain position. The follower customer
	 * of this customer with same block must have a position, which is exactly one bigger.
	 * It is valid, if customers of one block are spreaded of the route. Hence it is valid, if
	 * between 2 block-customers, there is a non-block customer.
	 *
	 * If this customer is not dedicated to a certain block, then this parameter is ignored.
	 * If, for any reason, there is no possible solution for given block and position, then all customers
	 * of this block will be unplanned.
	 *
	 * Default value is -1, where this node has no presetted position.
	 *
	 * This is handy, if the instance is well known and there are already best-patterns. Then
	 * these patterns can be modeled as blocks with positions.
	 */
	public CustomerData setPresetBlockPos(int pos) {
		this.presetBlockPos = pos;
		return this;
	}
	

	/**
	 * A customer, which is dedicated to a block, can get a rank value. All customers in a
	 * block must be placed so, that the rank of a follower customer must be greater than ancestor.
	 * It is valid, if customers of one block are spreaded of the route. Hence it is valid, if
	 * between 2 block-customers, there is a non-block customer.
	 *
	 * If this customer is not dedicated to a certain block, then this parameter is ignored.
	 * If, for any reason, there is no possible solution for given block and position, then all customers
	 * of this block will be unplanned.
	 *
	 * Default value is 0, which means, that no rank is given. The rank must not be lower than 0.
	 *
	 * This is handy, if the instance is well known or other business relevant restrictions must be
	 * considered. Also, preset positions and ranks can be mixed in a block.
	 */
	public CustomerData setPresetBlockRank(int rank) {
		this.presetBlockRank = rank;
		return this;
	}

	/**
	 * This list of customers are not allowed to be placed on one route together with this
	 * customer. The blacklisted customers are identified by their external id.
	 *
	 * If there is no feasible solution, this customer will be unplanned.
	 *
	 * This is handy, if a customer can retrieve multiple service, but not on same route.
	 * For example multi-period plannings.
	 */
	public CustomerData setPresetRoutingBlackList(Set<String> blackListedNodeSet) {
		if(blackListedNodeSet != null && blackListedNodeSet.size() > 0) {
			presetRoutingBlackList.addAll(blackListedNodeSet);
		}
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
	 * @param depots
	 * @return
	 */
	public CustomerData setPresetDepotList(Set<String> depots) {
		if(depots != null) {
			presetDepotList.addAll(depots);
		}
		return this;
	}

	///////////////////////////////////////

	public float[] getDemand() {
		return demand;
	}

	/**
	 * @return the serviceTime
	 */
	public float getServiceTime() {
		return serviceTime;
	}

	/**
	 * @return the shipID
	 */
	public String getShipID() {
		return shipID;
	}

	/**
	 * @return the presetBlockName
	 */
	public String getPresetBlockName() {
		return presetBlockName;
	}

	/**
	 * @return the presetBlockVehicleList
	 */
	public Set<String> getPresetBlockVehicleList() {
		return presetBlockVehicleList;
	}

	/**
	 * @return the serviceTimeForSite
	 */
	float getServiceTimeForSite() {
		return serviceTimeForSite;
	}

	/**
	 * @return the presetRoutingBlackList
	 */
	public Set<String> getPresetRoutingBlackList() {
		return presetRoutingBlackList;
	}

	/**
	 * @return the presetDepotList
	 */
	public Set<String> getPresetDepotList() {
		return presetDepotList;
	}

	public int getPresetBlockPosition() {
		return presetBlockPos;
	}

	Node createCustomer(int idx) {
		checkTimeWindows();

		return new Node(
				idx,
				externID,
				SiteType.CUSTOMER,
				xlong,
				ylat,
				geoId,
				demand,
				timeWindowList.toArray(new float[0][]),
				serviceTime,
				serviceTimeForSite,
				loadType,
				presetBlockRank,
				shipID,
				0
		);
	}
}
