package xf.xfvrp.base.fleximport;

import static xf.xfvrp.base.Vehicle.PRIORITY_UNDEF;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xf.xfvrp.base.CompartmentLoadType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;

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
	
	protected String name;
	
	protected float fixCost = 0;
	protected float varCost = 1;
	
	protected int count = Integer.MAX_VALUE;
	protected float maxRouteDuration = Float.MAX_VALUE;
	protected int maxStopCount = Integer.MAX_VALUE;
	protected float maxWaitingTime = Float.MAX_VALUE;
	protected int vehicleMetricId = 0;
	
	protected float maxDrivingTimePerShift = Integer.MAX_VALUE;
	protected float waitingTimeBetweenShifts = 0;
	
	protected int priority = PRIORITY_UNDEF;
	
	protected List<CompartmentCapacity> compartments;
	
	private static final String defaultVehicleName = "DEFAULT";
	
	public VehicleData() {
		compartments = Arrays.asList(new CompartmentCapacity(), new CompartmentCapacity(), new CompartmentCapacity());
	}
	
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
		this.compartments = new ArrayList<>();
		for (float simpleCapacityPerCompartment : capacity) {
			this.compartments.add(new CompartmentCapacity(simpleCapacityPerCompartment));
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
		if(compartmentIdx >= compartments.size()) {
			List<CompartmentCapacity> newCapacityPerCompartment = new ArrayList<>(compartmentIdx + 1);
			for (int i = 0; i < compartments.size(); i++) {
				newCapacityPerCompartment.set(i, compartments.get(i));
			}
			this.compartments = newCapacityPerCompartment;
		}
		
		this.compartments.set(compartmentIdx, compartmentCapacity);
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
	public VehicleData setPriority(int priority) {
		this.priority = priority;
		return this;
	}
	
	/**
	 * Creates a default vehicle object, which parameters mean no restriction.
	 *
	 * @return default vehicle
	 */
	public static VehicleData createDefault() {
		return new VehicleData().setName(defaultVehicleName);
	}
	
	
	/**
	 * @return Creates an internal Vehicle object with imported vehicle data
	 */
	Vehicle createVehicle(int idx) throws XFVRPException {
		if(maxRouteDuration <= 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for maxRouteDuration must be greater than zero.");
		if(maxStopCount <= 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for maxStopCount must be greater than zero.");
		if(count <= 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for nbrOfAvailableVehicles must be greater than zero.");
		if(maxWaitingTime < 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for maxWaitingTime must be greater or equal than zero.");
		if(vehicleMetricId < 0)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Parameter for vehicleMetricId must be greater or equal than zero.");
		
		float[] capacityArray = transformCapacity(compartments);
		boolean greaterZero = false;
		for (float v : capacityArray)
			if (v > 0) {
				greaterZero = true;
				break;
			}
		if(!greaterZero)
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "Vehicle capacities must be greater or equal than zero.");
		
		return new Vehicle(
				idx, name, count, capacityArray, maxRouteDuration, maxStopCount, maxWaitingTime, fixCost,
				varCost, vehicleMetricId, maxDrivingTimePerShift, waitingTimeBetweenShifts, priority
		);
	}
	
	public static float[] transformCapacity(List<CompartmentCapacity> capacityPerCompartment) {
		float[] capacityArray = new float[capacityPerCompartment.size() * CompartmentLoadType.NBR_OF_LOAD_TYPES];
		Arrays.fill(capacityArray, Float.MAX_VALUE);
		for (int i = 0; i < capacityPerCompartment.size(); i++) {
			CompartmentCapacity compartmentCapacity = capacityPerCompartment.get(i);
			if(compartmentCapacity != null) {
				System.arraycopy(compartmentCapacity.asArray(), 0, capacityArray,
						i * CompartmentLoadType.NBR_OF_LOAD_TYPES, CompartmentLoadType.NBR_OF_LOAD_TYPES);
			}
		}
		return capacityArray;
	}
	
	@Override
	public String toString() {
		return name;
	}
}