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
	private float waitingTime = 0;

	private float pickupLoad = 0;
	private float delivery = 0;
	private float pickup2 = 0;
	private float delivery2 = 0;
	private float pickup3 = 0;
	private float delivery3 = 0;

	private float delay = 0;
	private float overload1 = 0;
	private float overload2 = 0;
	private float overload3 = 0;
	private float[] maxCommonLoad = new float[3];

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
		
		if(e.getSiteType() == SiteType.REPLENISH)
			maxCommonLoad = new float[3];


		if(e.getLoadType() == LoadType.PICKUP) {
			setPickupLoad(e);
			if(e.getSiteType() == SiteType.CUSTOMER)
				checkOverload(e);
		} else if(e.getLoadType() == LoadType.DELIVERY) {
			if(e.getSiteType() == SiteType.CUSTOMER)
				checkOverload(e);
			setDeliveryLoad(e);
		}
		
		
		setNbrOfStops(e);
	}

	private void setDeliveryLoad(Event e) {
		if(e.getSiteType() == SiteType.CUSTOMER) {
			delivery += e.getAmount();
			delivery2 += e.getAmount2();
			delivery3 += e.getAmount3();
		}

		maxCommonLoad[0] -= e.getAmount();
		maxCommonLoad[1] -= e.getAmount2();
		maxCommonLoad[2] -= e.getAmount3();
	}

	private void setPickupLoad(Event e) {
		if(e.getSiteType() == SiteType.CUSTOMER) {
			pickupLoad += e.getAmount();
			pickup2 += e.getAmount2();
			pickup3 += e.getAmount3();
		}

		maxCommonLoad[0] += e.getAmount();
		maxCommonLoad[1] += e.getAmount2();
		maxCommonLoad[2] += e.getAmount3();
	}

	private void setNbrOfStops(Event e) {
		if(e.getSiteType().equals(SiteType.CUSTOMER) && e.getDistance() > 0)
			nbrOfStops++;
	}

	private void checkOverload(Event e) {
		overload1 += (maxCommonLoad[0] > vehicle.capacity[0]) ? e.getAmount() : 0;
		overload2 += (vehicle.capacity.length >= 2 && maxCommonLoad[1] > vehicle.capacity[1]) ? e.getAmount2() : 0;
		overload3 += (vehicle.capacity.length >= 3 && maxCommonLoad[2] > vehicle.capacity[2]) ? e.getAmount3() : 0;
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
		return pickupLoad;
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

	public float getOverload1() {
		return overload1;
	}

	public float getOverload2() {
		return overload2;
	}

	public float getOverload3() {
		return overload3;
	}


}
