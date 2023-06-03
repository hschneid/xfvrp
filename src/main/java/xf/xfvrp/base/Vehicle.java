package xf.xfvrp.base;

import xf.xfvrp.base.exception.XFVRPException;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * A Container encapsulates all necessary parameters like
 * loading capacity, cost parameters or maximal route duration.
 *
 * @author hschneid
 *
 */
public class Vehicle {
	public static final int PRIORITY_UNDEF = -1;
	
	private final int idx;
	private final String name;
	private int nbrOfAvailableVehicles;
	
	private float[] capacity;
	private final float fixCost;
	private final float varCost;
	private final float maxRouteDuration;
	private final int maxStopCount;
	private final float maxWaitingTime;
	private final int vehicleMetricId;

	private final float maxDrivingTimePerShift;
	private final float waitingTimeBetweenShifts;

	private int priority;

	public Vehicle(int idx, String name, int nbrOfAvailableVehicles, float[] capacity,
				   float maxRouteDuration, int maxStopCount, float maxWaitingTime, float fixCost, float varCost, int vehicleMetricId,
				   float maxDrivingTimePerShift, float waitingTimeBetweenShifts, int priority
	) throws XFVRPException {
		this.idx = idx;
		this.name = name;
		this.maxRouteDuration = maxRouteDuration;
		this.maxStopCount = maxStopCount;
		this.maxWaitingTime = maxWaitingTime;
		this.fixCost = fixCost;
		this.varCost = varCost;
		this.vehicleMetricId = vehicleMetricId;
		this.maxDrivingTimePerShift = maxDrivingTimePerShift;
		this.waitingTimeBetweenShifts = waitingTimeBetweenShifts;
		
		this.setCapacity(capacity);
		this.setNbrOfAvailableVehicles(nbrOfAvailableVehicles);
		this.setPriority(priority);
	}
	
	public Vehicle(Vehicle other) {
		this(other.idx, other.name, other.nbrOfAvailableVehicles, other.capacity, other.maxRouteDuration, other.maxStopCount,
				other.maxWaitingTime, other.fixCost, other.varCost, other.vehicleMetricId, other.maxDrivingTimePerShift, other.waitingTimeBetweenShifts,
				other.priority);
	}
	
	public int getIdx() {
		return idx;
	}
	
	public String getName() {
		return name;
	}
	
	public float[] getCapacity() {
		return capacity;
	}
	
	public void setCapacity(float[] capacity) {
		this.capacity = capacity;
	}
	
	public float getFixCost() {
		return fixCost;
	}
	
	public float getVarCost() {
		return varCost;
	}
	
	public int getNbrOfAvailableVehicles() {
		return nbrOfAvailableVehicles;
	}
	
	public void setNbrOfAvailableVehicles(int nbrOfAvailableVehicles) {
		this.nbrOfAvailableVehicles = nbrOfAvailableVehicles;
	}
	
	public float getMaxRouteDuration() {
		return maxRouteDuration;
	}
	
	public int getMaxStopCount() {
		return maxStopCount;
	}
	
	public float getMaxWaitingTime() {
		return maxWaitingTime;
	}
	
	public int getVehicleMetricId() {
		return vehicleMetricId;
	}
	
	public float getMaxDrivingTimePerShift() {
		return maxDrivingTimePerShift;
	}
	
	public float getWaitingTimeBetweenShifts() {
		return waitingTimeBetweenShifts;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public void setPriority(int priority) {
		this.priority = priority;
	}
}
