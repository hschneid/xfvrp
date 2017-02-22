package xf.xfvrp.report;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Summary of a values for a route.
 * 
 * Events can be added, where the values are updated directly.
 * 
 * @author hschneid
 *
 */
public class RouteReportSummary {
	private Vehicle vehicle;
	
	private int nbrOfEvents = 0;
	private int nbrOfStops = 0;
	private float distance = 0;
	private float duration = 0;
	private float delay = 0;
	private float waitingTime = 0;
	private float pickup = 0;
	private float delivery = 0;
	private float pickup2 = 0;
	private float delivery2 = 0;
	private float pickup3 = 0;
	private float delivery3 = 0;

	/**
	 * 
	 * @param vehicle
	 */
	public RouteReportSummary(Vehicle vehicle) {
		this.vehicle = vehicle;
	}
	
	/**
	 * Adds a event to the summary object.
	 * 
	 * The aggregation of KPIs is done in this method.
	 * 
	 * @param e
	 */
	public void add(Event e) {
		distance += e.getDistance();
		duration += e.getDuration();
		delay += e.getDelay();
		waitingTime += e.getWaiting();
		nbrOfEvents++;
		
		if (e.getLoadType() != null)
			if(e.getLoadType().equals(LoadType.PICKUP)) {
				pickup += e.getAmount();
				pickup2 += e.getAmount2();
				pickup3 += e.getAmount3();
			} else if(e.getLoadType().equals(LoadType.DELIVERY)) {
				delivery += e.getAmount();
				delivery2 += e.getAmount2();
				delivery3 += e.getAmount3();
			}
		
		// If event is not at a depot, count it as a stop
		if(e.getSiteType().equals(SiteType.CUSTOMER))
			nbrOfStops++;
	}
	
	/**
	 * @return the nbrCustomers
	 */
	public int getNbrOfEvents() {
		return nbrOfEvents;
	}
	/**
	 * @return the distance
	 */
	public float getDistance() {
		return distance;
	}
	/**
	 * @return the delay
	 */
	public float getDelay() {
		return delay;
	}
	/**
	 * @return the pickup
	 */
	public float getPickup() {
		return pickup;
	}
	/**
	 * @return the delivery
	 */
	public float getDelivery() {
		return delivery;
	}
	
	/**
	 * Cost function: fix + var * distance
	 * 
	 * @return
	 */
	public float getCost() {
		return vehicle.fixCost + distance * vehicle.varCost;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getDuration() {
		return duration;
	}
	
	/**
	 * @param vehicle the vehicle to set
	 */
	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getPickup2() {
		return pickup2;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getDelivery2() {
		return delivery2;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getPickup3() {
		return pickup3;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getDelivery3() {
		return delivery3;
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
	public int getNbrOfStops() {
		return nbrOfStops;
	}
}
