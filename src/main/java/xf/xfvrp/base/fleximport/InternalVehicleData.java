package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.Vehicle;

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

	public static String defaultVehicleName = "DEFAULT";
	public static String invalidVehicleName = "INVALID";


	/**
	 * Creates a default vehicle object, which parameters mean no restriction.
	 * 
	 * @return default vehicle
	 */
	public static InternalVehicleData createDefault() {
		InternalVehicleData v = (InternalVehicleData) new InternalVehicleData().setName(defaultVehicleName);

		return v;
	}

	/**
	 * Creates a default vehicle object for invalid routes, which parameters mean no restriction.
	 * 
	 * @return default vehicle for invalid routes
	 */
	public static InternalVehicleData createInvalid() {
		InternalVehicleData v = (InternalVehicleData) new InternalVehicleData().setName(invalidVehicleName);

		return v;
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
	public float[] getCapacity() {
		return capacity;
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
	 * @return
	 */
	public float getMaxWaitingTime() {
		return maxWaitingTime;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getVehicleMetricId() {
		return vehicleMetricId;
	}
	/**
	 * 
	 * @return
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * 
	 * @return
	 */
	public Vehicle createVehicle(int idx) {
		Vehicle v = new Vehicle(
				idx,
				name,
				count,
				capacity,
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

		return v;
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
