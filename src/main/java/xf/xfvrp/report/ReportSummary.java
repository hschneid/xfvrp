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

	private float distance = 0;
	private float duration = 0;
	private float nbrOfUsedVehicles = 0;
	private float delay = 0;
	private float waitingTime = 0;
	private float[] overloads;
	private float cost = 0;
	
	private final Map<Vehicle, RouteReportSummary> dataMap = new HashMap<>();

	public void add(RouteReport t) {
		RouteReportSummary routeSummary = t.getSummary();
		addPerVehicleType(t, routeSummary);
		addTotal(routeSummary);
	}

	private void addTotal(RouteReportSummary routeSummary) {
		distance += routeSummary.getDistance();
		nbrOfUsedVehicles++;
		delay += routeSummary.getDelay();
		waitingTime += routeSummary.getWaitingTime();
		cost += routeSummary.getCost();
		duration += routeSummary.getDuration();
		addOverloads(routeSummary);
	}

	private void addPerVehicleType(RouteReport t, RouteReportSummary routeSummary) {
		if(!dataMap.containsKey(t.getVehicle()))
			dataMap.put(t.getVehicle(), new RouteReportSummary(t.getVehicle()));
		RouteReportSummary data = dataMap.get(t.getVehicle());
		data.add(routeSummary);
	}

	private void addOverloads(RouteReportSummary routeSummary) {
		if(overloads == null && routeSummary.getOverloads() != null) {
			overloads = new float[routeSummary.getOverloads().length];
		}
	}

	public float getDistance(Vehicle veh) {
		return dataMap.get(veh).getDistance();
	}

	public float getDistance() {
		return distance;
	}

	public float getNbrOfUsedVehicles(Vehicle veh) {
		return dataMap.get(veh).getNbrOfRoutes();
	}

	public float getNbrOfUsedVehicles() {
		return nbrOfUsedVehicles;
	}

	public float getDelay(Vehicle veh) {
		return dataMap.get(veh).getDelay();
	}

	public float getDelay() {
		return delay;
	}

	public float getOverload(Vehicle veh) {
		return dataMap.get(veh).getOverloads()[0];
	}

	public float[] getOverloads(Vehicle veh) {
		return dataMap.get(veh).getOverloads();
	}

	public float[] getOverloads() {
		return overloads;
	}

	public float getCost(Vehicle veh) {
		return dataMap.get(veh).getCost();
	}

	public float getCost() {
		return cost;
	}

	public float getDuration() {
		return duration;
	}

	public float getDuration(Vehicle veh) {
		return dataMap.get(veh).getDuration();
	}

	public float getWaitingTime() {
		return waitingTime;
	}

	public float getWaitingTime(Vehicle veh) {
		return dataMap.get(veh).getWaitingTime();
	}
}
