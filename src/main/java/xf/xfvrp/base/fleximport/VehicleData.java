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
