package xf.xfvrp.report;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * The Event is the atomic structure of a report and represents the
 * visit of a truck at a certain node. The values in a event are not
 * cummulative, so the summary objects contain the summed values.
 * 
 * Events are built up in the simulation process, when report object is
 * requested in the solution object.
 *  
 * @author hschneid
 *
 */
public class Event {
//	private Node node;
	
	private String id;
	private String shipId;
	
	private float distance = 0;
	private float travelTime = 0;
	private float duration = 0;
	private float amount = 0;
	private float amount2 = 0;
	private float amount3 = 0;
	
	private LoadType loadType = LoadType.UNDEF;
	private SiteType siteType;
	
	private float arrival = 0;
	private float departure = 0;
	private float service = 0;
	private float waiting = 0;
	
	private float delay = 0;
	
	public Event(Node node) {
		super();
		this.id = node.getExternID();
		this.shipId = node.getShipID();
		
		this.siteType = node.getSiteType();
	}
	
	/**
	 * @return the shipID
	 */
	public String getID() {
		return id;
	}
	
	/**
	 * @return the distance
	 */
	public float getDistance() {
		return distance;
	}
	/**
	 * @param distance the distance to set
	 */
	public void setDistance(float distance) {
		this.distance = distance;
	}
	/**
	 * @return the amount
	 */
	public float getAmount() {
		return amount;
	}
	/**
	 * @param amount the amount to set
	 */
	public void setAmount(float amount) {
		this.amount = amount;
	}

	/**
	 * 
	 * @return
	 */
	public SiteType getSiteType() {
		return siteType;
	}
	
	/**
	 * 
	 * @return
	 */
	public LoadType getLoadType() {
		return loadType;
	}
	
	/**
	 * 
	 * @param loadType
	 */
	public void setLoadType(LoadType loadType) {
		this.loadType = loadType;
	}
	
	/**
	 * @return the arrival
	 */
	public float getArrival() {
		return arrival;
	}
	/**
	 * @param arrival the arrival to set
	 */
	public void setArrival(float arrival) {
		this.arrival = arrival;
	}
	/**
	 * @return the departure
	 */
	public float getDeparture() {
		return departure;
	}
	/**
	 * @param departure the departure to set
	 */
	public void setDeparture(float departure) {
		this.departure = departure;
	}
	/**
	 * @return the service
	 */
	public float getService() {
		return service;
	}
	/**
	 * @param service the service to set
	 */
	public void setService(float service) {
		this.service = service;
	}
	/**
	 * @return the waiting
	 */
	public float getWaiting() {
		return waiting;
	}
	/**
	 * @param waiting the waiting to set
	 */
	public void setWaiting(float waiting) {
		this.waiting = waiting;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getDelay() {
		return delay;
	}
	
	/**
	 * 
	 * @param delay
	 */
	public void setDelay(float delay) {
		this.delay = delay;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getTravelTime() {
		return travelTime;
	}
	
	/**
	 * 
	 * @param travelTime
	 */
	public void setTravelTime(float travelTime) {
		this.travelTime = travelTime;
	}
	
	/**
	 * 
	 * @param duration
	 */
	public void setDuration(float duration) {
		this.duration = duration;
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
	 * @return
	 */
	public float getAmount2() {
		return amount2;
	}
	
	/**
	 * 
	 * @param amount2
	 */
	public void setAmount2(float amount2) {
		this.amount2 = amount2;
	}
	
	/**
	 * 
	 * @return
	 */
	public float getAmount3() {
		return amount3;
	}
	
	/**
	 * 
	 * @param amount3
	 */
	public void setAmount3(float amount3) {
		this.amount3 = amount3;
	}

	/**
	 * @return the shipId
	 */
	public String getShipID() {
		return shipId;
	}
}
