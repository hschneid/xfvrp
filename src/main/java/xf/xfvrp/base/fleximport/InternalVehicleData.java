package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.exception.XFVRPException;

import java.util.List;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 */
public class InternalVehicleData extends VehicleData {

	private static final long serialVersionUID = -2633261848511514004L;

	public static final String defaultVehicleName = "DEFAULT";
	public static final String invalidVehicleName = "INVALID";

	/**
	 * Creates a default vehicle object, which parameters mean no restriction.
	 * 
	 * @return default vehicle
	 */
	public static InternalVehicleData createDefault() {
		return (InternalVehicleData) new InternalVehicleData().setName(defaultVehicleName);
	}

	/**
	 * Creates a default vehicle object for invalid routes, which parameters mean no restriction.
	 * 
	 * @return default vehicle for invalid routes
	 */
	public static InternalVehicleData createInvalid() {
		return (InternalVehicleData) new InternalVehicleData().setName(invalidVehicleName);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the capacity
	 */
	public List<CompartmentCapacity> getCapacity() {
		return capacityPerCompartment;
	}

	/**
	 * @return the fixCost
	 */
	public float getFixCost() {
		return fixCost;
	}
	/**
	 * @return the varCost
	 */
	public float getVarCost() {
		return varCost;
	}
	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}
	/**
	 * @return the maxRouteDuration
	 */
	public float getMaxRouteDuration() {
		return maxRouteDuration;
	}
	/**
	 * @return the maxStopCount
	 */
	public int getMaxStopCount() {
		return maxStopCount;
	}

	/**
	 * @return the maximal allowed time to wait at a certain node
	 */
	public float getMaxWaitingTime() {
		return maxWaitingTime;
	}
	
	public int getVehicleMetricId() {
		return vehicleMetricId;
	}
	/**
	 * @return the rank of the vehicle type, which vehicle type ordering should be planned
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * @return Creates an internal Vehicle object with imported vehicle data
	 */
	public Vehicle createVehicle(int idx) throws XFVRPException {
		return new Vehicle(
				idx,
				name,
				count,
				capacityPerCompartment,
				maxRouteDuration,
				maxStopCount,
				maxWaitingTime,
				fixCost,
				varCost,
				vehicleMetricId,
				maxDrivingTimePerShift,
				waitingTimeBetweenShifts,
				priority
				);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return name;
	}
}
