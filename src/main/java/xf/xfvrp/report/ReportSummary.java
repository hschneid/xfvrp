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

	private static final int LENGTH = 7;
	
	private float distance = 0;
	private float duration = 0;
	private float nbrOfUsedVehicles = 0;
	private float delay = 0;
	private float waitingTime = 0;
	private float overload = 0;
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
		
		data[0] += routeSummary.getDistance();
		data[1]++;
		data[2] += routeSummary.getDelay();
		data[3] += 0;
		data[4] += routeSummary.getCost();
		data[5] += routeSummary.getDuration();
		data[6] += routeSummary.getWaitingTime();
		
		distance += routeSummary.getDistance();
		nbrOfUsedVehicles++;
		delay += routeSummary.getDelay();
		waitingTime += routeSummary.getWaitingTime();
		cost += routeSummary.getCost();
		duration += routeSummary.getDuration();
	}

	/**
	 * 
	 * @param veh
	 * @return the distance
	 */
	public float getDistance(Vehicle veh) {
		return dataMap.get(veh)[0];
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
		return dataMap.get(veh)[1];
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
		return dataMap.get(veh)[2];
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
		return dataMap.get(veh)[3];
	}

	/**
	 * @return the overload
	 */
	public float getOverload() {
		return overload;
	}
	
	/**
	 * @param veh
	 * @return the cost
	 */
	public float getCost(Vehicle veh) {
		return dataMap.get(veh)[4];
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
		return dataMap.get(veh)[5];
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
		return dataMap.get(veh)[6];
	}
}
