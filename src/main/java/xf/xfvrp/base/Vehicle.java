package xf.xfvrp.base;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.base.fleximport.CompartmentCapacity;

import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2012-2021 Holger Schneider
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
	// Capacity per compartement and load type (PICKUP, DELIVERY, MIXED)
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
	public Vehicle(int idx, String name, int nbrOfAvailableVehicles, List<CompartmentCapacity> capacity,
				   float maxRouteDuration, int maxStopCount, float maxWaitingTime, float fixCost, float varCost, int vehicleMetricId,
				   float maxDrivingTimePerShift, float waitingTimeBetweenShifts, int priority
	) throws XFVRPException {

		if (idx > -1) {
			float sumCapacity = 0;
			for (int i = capacity.size() - 1; i >= 0; i--)
				for (int j = 0; j < CompartmentLoadType.NBR_OF_LOAD_TYPES; j++)
					sumCapacity += capacity.get(i).asArray()[j];
			if (sumCapacity <= 0)
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for capacities must be greater than zero.");

			if (maxRouteDuration <= 0)
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for maxRouteDuration must be greater than zero.");
			if (maxStopCount <= 0)
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for maxStopCount must be greater than zero.");
			if (nbrOfAvailableVehicles <= 0)
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for nbrOfAvailableVehicles must be greater than zero.");
			if (maxWaitingTime < 0)
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for maxWaitingTime must be greater or equal than zero.");
			if (vehicleMetricId < 0)
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for vehicleMetricId must be greater or equal than zero.");
		}

		this.idx = idx;
		this.name = name;
		this.nbrOfAvailableVehicles = nbrOfAvailableVehicles;
		this.capacity = transform(capacity);
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

	private float[] transform(List<CompartmentCapacity> capacity) {
		float[] capacityArray = new float[capacity.size() * CompartmentLoadType.NBR_OF_LOAD_TYPES];
		Arrays.fill(capacityArray, Float.MAX_VALUE);

		for (int i = 0; i < capacity.size(); i++) {
			CompartmentCapacity compartmentCapacity = capacity.get(i);
			if(compartmentCapacity != null) {
				System.arraycopy(compartmentCapacity.asArray(), 0, capacityArray, i * CompartmentLoadType.NBR_OF_LOAD_TYPES, CompartmentLoadType.NBR_OF_LOAD_TYPES);
			}
		}

		return capacityArray;
	}
}
