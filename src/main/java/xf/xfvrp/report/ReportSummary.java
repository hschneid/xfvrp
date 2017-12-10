package xf.xfvrp.report;

import java.util.HashMap;
import java.util.Map;

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
public class ReportSummary {

	private static final int DISTANCE = 0;
	private static final int NBR_VEHICLES = 1;
	private static final int DELAY = 2;
	private static final int OVERLOAD = 3;
	private static final int COST = 4;
	private static final int DURATION = 5;
	private static final int WAITING = 6;
	private static final int LENGTH = 7;
	
	private float distance = 0;
	private float duration = 0;
	private float nbrOfUsedVehicles = 0;
	private float delay = 0;
	private float waitingTime = 0;
	private float overload1 = 0;
	private float overload2 = 0;
	private float overload3 = 0;
	private float cost = 0;
	
	private Map<Vehicle, float[]> dataMap = new HashMap<>();
	
	/**
	 * 
	 * @param t
	 */
	public void add(RouteReport t) {
		if(!dataMap.containsKey(t.getVehicle()))
			dataMap.put(t.getVehicle(), new float[LENGTH]);
		float[] data = dataMap.get(t.getVehicle());
		
		RouteReportSummary routeSummary = t.getSummary();
		
		data[DISTANCE] += routeSummary.getDistance();
		data[NBR_VEHICLES]++;
		data[DELAY] += routeSummary.getDelay();
		data[OVERLOAD] += routeSummary.getOverload1();
		data[COST] += routeSummary.getCost();
		data[DURATION] += routeSummary.getDuration();
		data[WAITING] += routeSummary.getWaitingTime();
		
		distance += routeSummary.getDistance();
		nbrOfUsedVehicles++;
		delay += routeSummary.getDelay();
		waitingTime += routeSummary.getWaitingTime();
		cost += routeSummary.getCost();
		duration += routeSummary.getDuration();
		overload1 += routeSummary.getOverload1();
		overload2 += routeSummary.getOverload2();
		overload3 += routeSummary.getOverload3();
	}

	/**
	 * 
	 * @param veh
	 * @return the distance
	 */
	public float getDistance(Vehicle veh) {
		return dataMap.get(veh)[DISTANCE];
	}
	
	/**
	 * @return the distance
	 */
	public float getDistance() {
		return distance;
	}
	
	/**
	 * 
	 * @param veh
	 * @return the tour
	 */
	public float getNbrOfUsedVehicles(Vehicle veh) {
		return dataMap.get(veh)[NBR_VEHICLES];
	}

	/**
	 * @return the tour
	 */
	public float getNbrOfUsedVehicles() {
		return nbrOfUsedVehicles;
	}
	
	/**
	 * 
	 * @param veh
	 * @return the delay
	 */
	public float getDelay(Vehicle veh) {
		return dataMap.get(veh)[DELAY];
	}

	/**
	 * @return the delay
	 */
	public float getDelay() {
		return delay;
	}
	
	/**
	 * @param veh
	 * @return the overload
	 */
	public float getOverload(Vehicle veh) {
		return dataMap.get(veh)[OVERLOAD];
	}

	/**
	 * @return the overload
	 */
	public float getOverload1() {
		return overload1;
	}
	
	public float getOverload2() {
		return overload2;
	}

	public float getOverload3() {
		return overload3;
	}

	/**
	 * @param veh
	 * @return the cost
	 */
	public float getCost(Vehicle veh) {
		return dataMap.get(veh)[COST];
	}

	/**
	 * @return the cost
	 */
	public float getCost() {
		return cost;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getDuration() {
		return duration;
	}
	
	/**
	 * 
	 * @param veh
	 * @return
	 */
	public float getDuration(Vehicle veh) {
		return dataMap.get(veh)[DURATION];
	}
	
	/**
	 * 
	 * @return
	 */
	public float getWaitingTime() {
		return waitingTime;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getWaitingTime(Vehicle veh) {
		return dataMap.get(veh)[WAITING];
	}
}
