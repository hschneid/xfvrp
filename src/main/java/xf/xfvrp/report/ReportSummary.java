package xf.xfvrp.report;

import util.ArrayUtil;
import xf.xfvrp.base.Vehicle;

import java.util.HashMap;
import java.util.Map;

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
	private float[] overloads;
	private float cost = 0;
	
	private final Map<Vehicle, float[]> dataMap = new HashMap<>();

	public void add(RouteReport t) {
		if(!dataMap.containsKey(t.getVehicle()))
			dataMap.put(t.getVehicle(), new float[LENGTH]);
		float[] data = dataMap.get(t.getVehicle());
		
		RouteReportSummary routeSummary = t.getSummary();
		
		data[DISTANCE] += routeSummary.getDistance();
		data[NBR_VEHICLES]++;
		data[DELAY] += routeSummary.getDelay();
		data[OVERLOAD] += routeSummary.getOverloads()[0];
		data[COST] += routeSummary.getCost();
		data[DURATION] += routeSummary.getDuration();
		data[WAITING] += routeSummary.getWaitingTime();
		
		distance += routeSummary.getDistance();
		nbrOfUsedVehicles++;
		delay += routeSummary.getDelay();
		waitingTime += routeSummary.getWaitingTime();
		cost += routeSummary.getCost();
		duration += routeSummary.getDuration();

		if(overloads == null && routeSummary.getOverloads() != null) {
			overloads = new float[routeSummary.getOverloads().length];
		}
		ArrayUtil.add(overloads, routeSummary.getOverloads(), overloads);
	}

	public float getDistance(Vehicle veh) {
		return dataMap.get(veh)[DISTANCE];
	}

	public float getDistance() {
		return distance;
	}

	public float getNbrOfUsedVehicles(Vehicle veh) {
		return dataMap.get(veh)[NBR_VEHICLES];
	}

	public float getNbrOfUsedVehicles() {
		return nbrOfUsedVehicles;
	}

	public float getDelay(Vehicle veh) {
		return dataMap.get(veh)[DELAY];
	}

	public float getDelay() {
		return delay;
	}

	public float getOverload(Vehicle veh) {
		return dataMap.get(veh)[OVERLOAD];
	}

	public float[] getOverloads() {
		return overloads;
	}

	public float getCost(Vehicle veh) {
		return dataMap.get(veh)[COST];
	}

	public float getCost() {
		return cost;
	}

	public float getDuration() {
		return duration;
	}

	public float getDuration(Vehicle veh) {
		return dataMap.get(veh)[DURATION];
	}

	public float getWaitingTime() {
		return waitingTime;
	}

	public float getWaitingTime(Vehicle veh) {
		return dataMap.get(veh)[WAITING];
	}
}
