package xf.xfvrp.report;

import java.util.ArrayList;
import java.util.List;

import xf.xfvrp.base.Vehicle;

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
	private boolean hasPackageEvents = false;
	private List<Event> eventList = new ArrayList<>();
	private List<PackageEvent> packageEventList = new ArrayList<>();

	
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
	 * @param e
	 */
	public void add(PackageEvent e) {
		packageEventList.add(e);
		hasPackageEvents  = true;
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
	 * 
	 * @return
	 */
	public List<PackageEvent> getPackageEvents() {
		return packageEventList;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean hasPackageEvents() {
		return hasPackageEvents;
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
