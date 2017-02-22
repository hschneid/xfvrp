package xf.xfvrp.base.fleximport;

import java.io.Serializable;

import xf.xfvrp.base.Vehicle;

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
public abstract class VehicleData implements Serializable {
	private static final long serialVersionUID = -7693160190888296907L;
	
	/** Basic - parameter **/
	protected String name = "";
	protected float[] capacity = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
	protected float fixCost = 0;
	protected float varCost = 1;
	protected int count = Integer.MAX_VALUE;
	protected float maxRouteDuration = Float.MAX_VALUE;
	protected int maxStopCount = Integer.MAX_VALUE;
	protected float maxWaitingTime = Float.MAX_VALUE;
	protected int vehicleMetricId = 0;
	
	/** Efficient Load - parameter **/
	/* Each truck can contain at max two vessels like i.e. two trailers or containers */
	protected float capacityOfVesselFirst = Float.MAX_VALUE;
	protected float capacity2OfVesselFirst = Float.MAX_VALUE; 
	protected int heightOfVesselFirst = Integer.MAX_VALUE;
	protected int widthOfVesselFirst = Integer.MAX_VALUE;
	protected int lengthOfVesselFirst = Integer.MAX_VALUE;

	protected float capacityOfVesselSecond = Float.MAX_VALUE;
	protected float capacity2OfVesselSecond = Float.MAX_VALUE; 
	protected int heightOfVesselSecond = Integer.MAX_VALUE;
	protected int widthOfVesselSecond = Integer.MAX_VALUE;
	protected int lengthOfVesselSecond = Integer.MAX_VALUE;
	
	/** Driver time restriction **/
	protected float maxDrivingTimePerShift = Integer.MAX_VALUE;
	protected float waitingTimeBetweenShifts = 0;
	
	protected int priority = Vehicle.PRIORITY_UNDEF;
	
	/**
	 * @param name the name to set
	 */
	public VehicleData setName(String name) {
		this.name = name;
		return this;
	}
	/**
	 * @param capacity the capacity to set
	 */
	public VehicleData setCapacity(float[] capacity) {
		this.capacity = capacity;
		return this;
	}
	
	/**
	 * @param fixCost the fixCost to set
	 */
	public VehicleData setFixCost(float fixCost) {
		this.fixCost = fixCost;
		return this;
	}
	/**
	 * @param varCost the varCost to set
	 */
	public VehicleData setVarCost(float varCost) {
		this.varCost = varCost;
		return this;
	}
	/**
	 * @param count the count to set
	 */
	public VehicleData setCount(int count) {
		this.count = count;
		return this;
	}
	/**
	 * @param maxRouteDuration the maxRouteDuration to set
	 */
	public VehicleData setMaxRouteDuration(float maxRouteDuration) {
		this.maxRouteDuration = maxRouteDuration;
		return this;
	}
	/**
	 * @param maxStopCount the maxStopCount to set
	 */
	public VehicleData setMaxStopCount(int maxStopCount) {
		this.maxStopCount = maxStopCount;
		return this;
	}
	
	/**
	 * @param maxWaitingTime
	 */
	public VehicleData setMaxWaitingTime(float maxWaitingTime) {
		this.maxWaitingTime = maxWaitingTime;
		return this;
	}
	
	/**
	 * @param capacityOfVesselFirst the capacityOfVesselFirst to set
	 */
	public VehicleData setCapacityOfVesselFirst(float capacityOfVesselFirst) {
		this.capacityOfVesselFirst = capacityOfVesselFirst;
		return this;
	}
	/**
	 * @param capacity2OfVesselFirst the capacity2OfVesselFirst to set
	 */
	public VehicleData setCapacity2OfVesselFirst(float capacity2OfVesselFirst) {
		this.capacity2OfVesselFirst = capacity2OfVesselFirst;
		return this;
	}
	/**
	 * @param heightOfVesselFirst the heightOfVesselFirst to set
	 */
	public VehicleData setHeightOfVesselFirst(int heightOfVesselFirst) {
		this.heightOfVesselFirst = heightOfVesselFirst;
		return this;
	}
	/**
	 * @param widthOfVesselFirst the widthOfVesselFirst to set
	 */
	public VehicleData setWidthOfVesselFirst(int widthOfVesselFirst) {
		this.widthOfVesselFirst = widthOfVesselFirst;
		return this;
	}
	/**
	 * @param lengthOfVesselFirst the lengthOfVesselFirst to set
	 */
	public VehicleData setLengthOfVesselFirst(int lengthOfVesselFirst) {
		this.lengthOfVesselFirst = lengthOfVesselFirst;
		return this;
	}
	/**
	 * @param capacityOfVesselSecond the capacityOfVesselSecond to set
	 */
	public VehicleData setCapacityOfVesselSecond(float capacityOfVesselSecond) {
		this.capacityOfVesselSecond = capacityOfVesselSecond;
		return this;
	}
	/**
	 * @param capacity2OfVesselSecond the capacity2OfVesselSecond to set
	 */
	public VehicleData setCapacity2OfVesselSecond(float capacity2OfVesselSecond) {
		this.capacity2OfVesselSecond = capacity2OfVesselSecond;
		return this;
	}
	/**
	 * @param heightOfVesselSecond the heightOfVesselSecond to set
	 */
	public VehicleData setHeightOfVesselSecond(int heightOfVesselSecond) {
		this.heightOfVesselSecond = heightOfVesselSecond;
		return this;
	}
	/**
	 * @param widthOfVesselSecond the widthOfVesselSecond to set
	 */
	public VehicleData setWidthOfVesselSecond(int widthOfVesselSecond) {
		this.widthOfVesselSecond = widthOfVesselSecond;
		return this;
	}
	/**
	 * @param lengthOfVesselSecond the lengthOfVesselSecond to set
	 */
	public VehicleData setLengthOfVesselSecond(int lengthOfVesselSecond) {
		this.lengthOfVesselSecond = lengthOfVesselSecond;
		return this;
	}
	/**
	 * @param vehicleMetricId
	 */
	public VehicleData setVehicleMetricId(int vehicleMetricId) {
		this.vehicleMetricId = vehicleMetricId;
		return this;
	}
	
	/**
	 * 
	 * @param maxDrivingTimePerShift
	 * @return
	 */
	public VehicleData setMaxDrivingTimePerShift(float maxDrivingTimePerShift) {
		this.maxDrivingTimePerShift = maxDrivingTimePerShift;
		return this;
	}
	
	/**
	 * 
	 * @param waitingTimeBetweenShifts
	 * @return
	 */
	public VehicleData setWaitingTimeBetweenShifts(float waitingTimeBetweenShifts) {
		this.waitingTimeBetweenShifts = waitingTimeBetweenShifts;
		return this;
	}
	
	/**
	 * 
	 * @param priority
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}
}
