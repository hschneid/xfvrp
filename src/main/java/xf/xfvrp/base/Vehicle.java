package xf.xfvrp.base;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;

/**
 * Copyright (c) 2012-present Holger Schneider
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
	
	/** Basic - parameter **/
	public final int idx;
	public final String name;
	public final float[] capacity;
	public final float fixCost;
	public final float varCost;
	public final int nbrOfAvailableVehicles;
	public final float maxRouteDuration;
	public final int maxStopCount;
	public final float maxWaitingTime;
	
	public final int vehicleMetricId;
	
	/** Driver time restriction **/
	public final float maxDrivingTimePerShift;
	public final float waitingTimeBetweenShifts;
	
	/**
	 * Priority
	 **/
	public int priority;
	
	/**
	 * Constructor for all variables
	 */
	public Vehicle(int idx, String name, int nbrOfAvailableVehicles, float[] capacity,
			float maxRouteDuration, int maxStopCount, float maxWaitingTime,	float fixCost, float varCost, int vehicleMetricId,
			float maxDrivingTimePerShift, float waitingTimeBetweenShifts, int priority
			) throws XFVRPException {
		float sumCapacity = 0;
		for(int i = capacity.length - 1; i >= 0; i--)
			sumCapacity += capacity[i];
		if(sumCapacity <= 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for capacities must be greater than zero.");
		
		if(maxRouteDuration <= 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for maxRouteDuration must be greater than zero.");
		if(maxStopCount <= 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for maxStopCount must be greater than zero.");
		if(nbrOfAvailableVehicles <= 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for nbrOfAvailableVehicles must be greater than zero.");
		if(maxWaitingTime < 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for maxWaitingTime must be greater or equal than zero.");
		if(vehicleMetricId < 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for vehicleMetricId must be greater or equal than zero.");

		this.idx = idx;
		this.name = name;
		this.nbrOfAvailableVehicles = nbrOfAvailableVehicles;
		this.capacity = capacity;
		this.maxRouteDuration = maxRouteDuration;
		this.maxStopCount = maxStopCount;
		this.maxWaitingTime = maxWaitingTime;
		this.fixCost = fixCost;
		this.varCost = varCost;
		this.vehicleMetricId = vehicleMetricId;
		
		this.maxDrivingTimePerShift = maxDrivingTimePerShift;
		this.waitingTimeBetweenShifts = waitingTimeBetweenShifts;
		
		this.priority = priority;
	}
}
