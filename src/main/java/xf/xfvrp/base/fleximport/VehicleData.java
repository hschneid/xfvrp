package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.exception.XFVRPException;

import java.io.Serializable;
import java.util.ArrayList;
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
 * @author hschneid
 *
 */
public class VehicleData implements Serializable {

	private static final long serialVersionUID = -7693160190888296907L;

	/** Basic - parameter **/
	protected String name = "";
	// Capacity per Compartment and Load Type - Default 3 compartments with max capacity
	protected List<CompartmentCapacity> capacityPerCompartment = Arrays.asList(new CompartmentCapacity(), new CompartmentCapacity(), new CompartmentCapacity());
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

	private static final String defaultVehicleName = "DEFAULT";

	/**
	 * @param name the name to set
	 */
	public VehicleData setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * This sets the capacity for a set of compartments, where all load types have same capacity.
	 */
	public VehicleData setCapacity(float[] capacity) {
		this.capacityPerCompartment = new ArrayList<>();
		for (float simpleCapacityPerCompartment : capacity) {
			this.capacityPerCompartment.add(new CompartmentCapacity(simpleCapacityPerCompartment));
		}

		return this;
	}

	/**
	 * This sets the capacity for a certain compartment with specification for each load type.
	 *
	 * compartmentIdx is an index in array. So it should be an integer between 0 and number of used compartments.
	 */
	public VehicleData setCapacityForCompartment(int compartmentIdx, CompartmentCapacity compartmentCapacity) {
		// If idx is out of range, increase size
		if(compartmentIdx >= capacityPerCompartment.size()) {
			List<CompartmentCapacity> newCapacityPerCompartment = new ArrayList<>(compartmentIdx + 1);
			for (int i = 0; i < capacityPerCompartment.size(); i++) {
				newCapacityPerCompartment.set(i, capacityPerCompartment.get(i));
			}
			this.capacityPerCompartment = newCapacityPerCompartment;
		}

		this.capacityPerCompartment.set(compartmentIdx, compartmentCapacity);
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
	 * @param maxDrivingTimePerShift
	 */
	public VehicleData setMaxDrivingTimePerShift(float maxDrivingTimePerShift) {
		this.maxDrivingTimePerShift = maxDrivingTimePerShift;
		return this;
	}

	/**
	 *
	 * @param waitingTimeBetweenShifts
	 */
	public VehicleData setWaitingTimeBetweenShifts(float waitingTimeBetweenShifts) {
		this.waitingTimeBetweenShifts = waitingTimeBetweenShifts;
		return this;
	}

	/**
	 * Is used by mixed fleet heuristics. If this parameter is set,
	 * this vehicle is priorized by this value. Lower value means
	 * higher priority.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	//////////////////////////////////////

	/**
	 * Creates a default vehicle object, which parameters mean no restriction.
	 *
	 * @return default vehicle
	 */
	public static VehicleData createDefault() {
		return new VehicleData().setName(defaultVehicleName);
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
	List<CompartmentCapacity> getCapacity() {
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
	int getPriority() {
		return priority;
	}

	/**
	 * @return Creates an internal Vehicle object with imported vehicle data
	 */
	Vehicle createVehicle(int idx) throws XFVRPException {
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