package xf.xfvrp.base;

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

	/** Efficient Load - parameter **/
	/* Each truck can contain at max two vessels like i.e. two trailers or containers */
	public final float capacityOfVesselFirst, capacity2OfVesselFirst; 
	public final int heightOfVesselFirst, widthOfVesselFirst, lengthOfVesselFirst;
	public final float capacityOfVesselSecond, capacity2OfVesselSecond; 
	public final int heightOfVesselSecond, widthOfVesselSecond, lengthOfVesselSecond;
	
	/** Driver time restriction **/
	public final float maxDrivingTimePerShift;
	public final float waitingTimeBetweenShifts;
	
	/** Priority **/
	public int priority = PRIORITY_UNDEF;
	
	/**
	 * Constructor for all variables
	 * 
	 * @param idx Internal index of this vehicle (no array index, cause no array for vehicles)
	 * @param name Name of vehicle
	 * @param nbrOfAvailableVehicles Number of available vehicles of this type
	 * @param capacity1 Maximal loading primary capacity
	 * @param capacity2 Maximal loading secondary capacity
	 * @param capacity3 Maximal loading third capacity
	 * @param maxRouteDuration Maximal route duration
	 * @param maxStopCount Maximal number of stops
	 * @param maxWaitingTime Maximal time a vehicle may wait at a customer location for service
	 * @param fixCost Fix cost per used vehicle 
	 * @param varCost Variable cost per each distance unit
	 * @param vehicleMetricId id of the associated metric data
	 * 
	 * @param capacityOfVesselFirst
	 * @param capacity2OfVesselFirst
	 * @param heightOfVesselFirst
	 * @param widthOfVesselFirst
	 * @param lengthOfVesselFirst
	 * @param capacityOfVesselSecond
	 * @param capacity2OfVesselSecond
	 * @param heightOfVesselSecond
	 * @param widthOfVesselSecond
	 * @param lengthOfVesselSecond
	 * @param withDriverTimeRestriction
	 * @param priority
	 */
	public Vehicle(int idx, String name, int nbrOfAvailableVehicles, float[] capacity,
			float maxRouteDuration, int maxStopCount, float maxWaitingTime,	float fixCost, float varCost, int vehicleMetricId,
			float capacityOfVesselFirst, float capacity2OfVesselFirst, int heightOfVesselFirst, int widthOfVesselFirst, int lengthOfVesselFirst,
			float capacityOfVesselSecond, float capacity2OfVesselSecond,  int heightOfVesselSecond, int widthOfVesselSecond, int lengthOfVesselSecond,
			float maxDrivingTimePerShift, float waitingTimeBetweenShifts, int priority
			) {		
		float sumCapacity = 0;
		for(int i = 0; i < capacity.length; i++)
			sumCapacity += capacity[i];
		if(sumCapacity <= 0)
			throw new IllegalStateException("Parameter for capacities must be greater than zero.");
		
		if(maxRouteDuration <= 0)
			throw new IllegalStateException("Parameter for maxRouteDuration must be greater than zero.");
		if(maxStopCount <= 0)
			throw new IllegalStateException("Parameter for maxStopCount must be greater than zero.");
		if(nbrOfAvailableVehicles <= 0)
			throw new IllegalStateException("Parameter for nbrOfAvailableVehicles must be greater than zero.");
		if(maxWaitingTime < 0)
			throw new IllegalStateException("Parameter for maxWaitingTime must be greater or equal than zero.");
		if(vehicleMetricId < 0)
			throw new IllegalStateException("Parameter for vehicleMetricId must be greater or equal than zero.");

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

		this.capacityOfVesselFirst = capacityOfVesselFirst;
		this.capacity2OfVesselFirst = capacity2OfVesselFirst;
		this.heightOfVesselFirst = heightOfVesselFirst;
		this.widthOfVesselFirst = widthOfVesselFirst;
		this.lengthOfVesselFirst = lengthOfVesselFirst;

		this.capacityOfVesselSecond = capacityOfVesselSecond;
		this.capacity2OfVesselSecond = capacity2OfVesselSecond;
		this.heightOfVesselSecond = heightOfVesselSecond;
		this.widthOfVesselSecond = widthOfVesselSecond;
		this.lengthOfVesselSecond = lengthOfVesselSecond;
		
		this.maxDrivingTimePerShift = maxDrivingTimePerShift;
		this.waitingTimeBetweenShifts = waitingTimeBetweenShifts;
		
		this.priority = priority;
	}
	
	/**
	 * 
	 * @return
	 */
	public String exportToString() {
		return
				"VEHICLE\t"+
				name+"\t"+
				exportArrayToString(capacity)+"\t"+
				fixCost+"\t"+
				varCost+"\t"+
				nbrOfAvailableVehicles+"\t"+
				maxRouteDuration+"\t"+
				maxStopCount+"\t"+
				maxWaitingTime+"\t"+
				vehicleMetricId+"\t"+
				capacityOfVesselFirst+"\t"+
				capacity2OfVesselFirst+"\t"+ 
				heightOfVesselFirst+"\t"+
				widthOfVesselFirst+"\t"+
				lengthOfVesselFirst+"\t"+
				capacityOfVesselSecond+"\t"+
				capacity2OfVesselSecond +"\t"+
				heightOfVesselSecond+"\t"+
				widthOfVesselSecond+"\t"+
				lengthOfVesselSecond+"\t"+
				maxDrivingTimePerShift+"\t"+
				waitingTimeBetweenShifts+"\t"+
				priority+"\n";
	}
	
	/**
	 * 
	 * @param arr
	 * @return
	 */
	private String exportArrayToString(float[] arr){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arr.length; i++)
			sb.append(arr[i]+";");
		return sb.toString();
	}
}