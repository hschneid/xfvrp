package xf.xfvrp.report;

import xf.xfvrp.base.Vehicle;

import java.util.ArrayList;
import java.util.List;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * LPReport of a route by a list of events
 * 
 * Each route report has a allocated vehicle, which contains
 * the parameters.
 * 
 * @author hschneid
 *
 */
public class RouteReport {

	private final RouteReportSummary summary;
	private final Vehicle vehicle;
	private List<Event> eventList = new ArrayList<>();

	
	/**
	 * 
	 * @param vehicle
	 */
	public RouteReport(Vehicle vehicle) {
		this.vehicle = vehicle;
		this.summary = new RouteReportSummary(vehicle);
	}

	/**
	 * 
	 * @param e
	 */
	public void add(Event e) {
		summary.add(e);
		eventList.add(e);
	}
		
	/**
	 * 
	 * @return
	 */
	public RouteReportSummary getSummary() {
		return summary;
	}
	
	/**
	 * 
	 * @return
	 */
	public Vehicle getVehicle() {
		return vehicle;
	}
	
	/**
	 * Returns a list of all event objects of all sub routes of this route.
	 * 
	 * @return list of event objects
	 */
	public List<Event> getEvents() {
		return eventList;
	}
	
	
}
