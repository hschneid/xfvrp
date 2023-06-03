package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.Node;

import java.util.Objects;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class RouteInfo {

	private final int compartmentIdx;
	private final Node depot;
	
	private float loadingServiceTime = 0;
	private float unLoadingServiceTime = 0;
	
	private float deliveryAmount = 0;
	private float pickupAmount = 0;
	
	public RouteInfo(Node depot, int compartmentIdx) {
		this.depot = depot;
		this.compartmentIdx = compartmentIdx;
	}
	
	public void addLoadingServiceTime(float time) {
		loadingServiceTime += time;
	}
	
	public void addUnLoadingServiceTime(float time) {
		unLoadingServiceTime += time;
	}

	public void addPickUpAmount(float demand) {
		pickupAmount += demand;
	}

	public void addDeliveryAmount(float demand) {
		deliveryAmount += demand;
	}

	public float getLoadingServiceTime() {
		return loadingServiceTime;
	}

	public float getUnLoadingServiceTime() {
		return unLoadingServiceTime;
	}

	public Node getDepot() {
		return depot;
	}

	public float getDeliveryAmount() {
		return deliveryAmount;
	}

	public float getPickupAmount() {
		return pickupAmount;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RouteInfo routeInfo = (RouteInfo) o;
		return Objects.equals(depot.getIdx(), routeInfo.depot.getIdx());
	}

	@Override
	public int hashCode() {
		return Objects.hash(depot.getIdx());
	}
}
