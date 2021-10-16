package xf.xfvrp.report;

import util.ArrayUtil;
import xf.xfvrp.base.CompartmentLoadType;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.opt.evaluation.Context;

import java.util.Arrays;

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
	private float[] pickups;
	private float[] deliveries;

	private final float[] pickupLoads;
	private final float[] deliveryLoads;
	private final float[] commonLoads;
	private final float[] overloads;

	public RouteReportSummary(Vehicle vehicle) {
		this.vehicle = vehicle;

		int nbrOfCompartments = (vehicle.getCapacity().length / CompartmentLoadType.NBR_OF_LOAD_TYPES);
		pickups = new float[nbrOfCompartments];
		deliveries = new float[nbrOfCompartments];

		pickupLoads = new float[nbrOfCompartments];
		deliveryLoads = new float[nbrOfCompartments];
		commonLoads = new float[nbrOfCompartments];
		overloads = new float[nbrOfCompartments];
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

		if (e.getSiteType() == SiteType.REPLENISH) {
			if(context.getCurrentNode().isCompartmentReplenished() != null) {
				for (int i = 0; i < context.getNbrOfCompartments(); i++) {
					if(context.getCurrentNode().isCompartmentReplenished()[i]) {
						pickupLoads[i] = 0;
						deliveryLoads[i] = 0;
						commonLoads[i] = 0;
					}
				}
			} else {
				Arrays.fill(pickupLoads, 0);
				Arrays.fill(deliveryLoads, 0);
				Arrays.fill(commonLoads, 0);
			}
		}

		if (e.getLoadType() == LoadType.PICKUP) {
			setPickupLoad(e);
		} else if (e.getLoadType() == LoadType.DELIVERY) {
			setDeliveryLoad(e);
		}
		checkOverload(e);

		setNbrOfStops(e);
	}

	private void setDeliveryLoad(Event e) {
		for (int compartment = 0; compartment < deliveries.length; compartment++) {
			deliveries[compartment] += e.getAmounts()[compartment];

			deliveryLoads[compartment] += e.getAmounts()[compartment];
			commonLoads[compartment] -= e.getAmounts()[compartment];
		}
	}

	private void setPickupLoad(Event e) {
		for (int compartment = 0; compartment < pickups.length; compartment++) {
			if(e.getSiteType() == SiteType.CUSTOMER) {
				pickups[compartment] += e.getAmounts()[compartment];
				pickupLoads[compartment] += e.getAmounts()[compartment];
			}
			if (e.getAmounts().length == commonLoads.length)
				commonLoads[compartment] += e.getAmounts()[compartment];
		}
	}

	private void setNbrOfStops(Event e) {
		if (e.getSiteType().equals(SiteType.CUSTOMER) && e.getDistance() > 0)
			nbrOfStops++;
	}

	private void checkOverload(Event e) {
		for (int compartment = 0; compartment < overloads.length; compartment++) {
			if (deliveryLoads[compartment] > 0 && pickupLoads[compartment] == 0) {
				overloads[compartment] += deliveryLoads[compartment] > vehicle.getCapacity()[compartment * CompartmentLoadType.NBR_OF_LOAD_TYPES + CompartmentLoadType.DELIVERY.index()] ? e.getAmounts()[compartment] : 0;
			} else if (deliveryLoads[compartment] == 0 && pickupLoads[compartment] > 0) {
				overloads[compartment] += pickupLoads[compartment] > vehicle.getCapacity()[compartment * CompartmentLoadType.NBR_OF_LOAD_TYPES + CompartmentLoadType.PICKUP.index()] ? e.getAmounts()[compartment] : 0;
			} else if (deliveryLoads[compartment] > 0 && pickupLoads[compartment] > 0) {
				overloads[compartment] += commonLoads[compartment] > vehicle.getCapacity()[compartment * CompartmentLoadType.NBR_OF_LOAD_TYPES + CompartmentLoadType.MIXED.index()] ? e.getAmounts()[compartment] : 0;
			}
		}
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
