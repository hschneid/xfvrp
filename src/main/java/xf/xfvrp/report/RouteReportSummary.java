package xf.xfvrp.report;

import util.ArrayUtil;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.compartment.CompartmentLoad;
import xf.xfvrp.base.compartment.CompartmentLoadBuilder;
import xf.xfvrp.opt.evaluation.Context;

/**
 * Copyright (c) 2012-2021 Holger Schneider
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

	private int nbrOfRoutes = 1;
	private int nbrOfEvents = 0;
	private int nbrOfStops = 0;
	private float distance = 0;
	private float duration = 0;
	private float waitingTime = 0;
	private float delay = 0;

	// Capacity values per compartment
	private final CompartmentLoad[] amounts;
	private float[] pickups;
	private float[] deliveries;

	private final float[] pickupLoads;
	private final float[] deliveryLoads;
	private final float[] commonLoads;
	private final float[] overloads;

	public RouteReportSummary(Vehicle vehicle, XFVRPModel model) {
		this.vehicle = vehicle;

		int nbrOfCompartments = (vehicle.getCapacity().length);
		pickups = new float[nbrOfCompartments];
		deliveries = new float[nbrOfCompartments];

		pickupLoads = new float[nbrOfCompartments];
		deliveryLoads = new float[nbrOfCompartments];
		commonLoads = new float[nbrOfCompartments];
		overloads = new float[nbrOfCompartments];

		amounts = CompartmentLoadBuilder.createCompartmentLoads(model.getCompartments());
	}

	/**
	 * This method is used to aggregate the ReportSummary per vehicle type.
	 */
	public void add(RouteReportSummary addReport) {
		this.nbrOfEvents += addReport.nbrOfEvents;
		this.nbrOfStops += addReport.nbrOfStops;
		this.distance += addReport.distance;
		this.duration += addReport.duration;
		this.waitingTime += addReport.waitingTime;
		this.delay += addReport.delay;
		this.nbrOfRoutes += addReport.nbrOfRoutes;
		ArrayUtil.add(this.pickups, addReport.pickups, this.pickups);
		ArrayUtil.add(this.deliveries, addReport.deliveries, this.deliveries);
		ArrayUtil.add(this.pickupLoads, addReport.pickupLoads, this.pickupLoads);
		ArrayUtil.add(this.deliveryLoads, addReport.deliveryLoads, this.deliveryLoads);
		ArrayUtil.add(this.commonLoads, addReport.commonLoads, this.commonLoads);
		ArrayUtil.add(this.overloads, addReport.overloads, this.overloads);
	}

	/**
	 * Adds an event to the summary object.
	 * <p>
	 * The aggregation of KPIs is done in this method.
	 */
	public void add(Event e, Context context) {
		distance += e.getDistance();
		duration += e.getDuration();
		delay += e.getDelay();
		waitingTime += e.getWaiting();
		nbrOfEvents++;


		for (int i = 0; i < context.getAmountsOfRoute().length; i++) {
			if (e.getSiteType() == SiteType.REPLENISH) {
				amounts[i].replenish();
			} else {
				amounts[i].addAmount(e.getAmounts(), e.getLoadType());
			}
			overloads[i] = amounts[i].checkCapacity(vehicle.getCapacity());
		}

		setNbrOfStops(e);
	}

	private void setNbrOfStops(Event e) {
		if (e.getSiteType().equals(SiteType.CUSTOMER) && e.getDistance() > 0)
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

	public float[] getPickups() {
		return pickups;
	}

	public float[] getDeliveries() {
		return deliveries;
	}

	/**
	 * Cost function: fix + var * distance
	 */
	public float getCost() {
		return vehicle.getFixCost() + distance * vehicle.getVarCost();
	}

	public float getDuration() {
		return duration;
	}

	/**
	 * @param vehicle the vehicle to set
	 */
	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public float getWaitingTime() {
		return waitingTime;
	}

	public int getNbrOfStops() {
		return nbrOfStops;
	}

	public float[] getOverloads() {
		return overloads;
	}

	public void setDuration(float duration) {
		this.duration = duration;
	}

	public void setDelay(float delay) {
		this.delay = delay;
	}

	public int getNbrOfRoutes() {
		return nbrOfRoutes;
	}
}
