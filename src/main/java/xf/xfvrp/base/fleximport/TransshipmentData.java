package xf.xfvrp.base.fleximport;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;

/**
 * Copyright (c) 2026 Lars Hackstein
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Transshipment data class defines all fields which can be used to describe a
 * transshipment location (TL) in the VRP with Trailers and Transshipments
 * (VRPTT).
 *
 * At a transshipment location load can be transferred between vehicles. The
 * location can have time windows and a fixed transfer duration, which is
 * consumed by each vehicle, that transfers load at this location.
 *
 * Each set-method returns the transshipment data object itself, so that the
 * set-methods can be called in a fluent way.
 *
 * @author hschneid
 *
 */
public class TransshipmentData extends NodeData {

	/**
	 * If parking is not allowed, the location is a pure transshipment point,
	 * where no trailer can be parked (dropped) between two visits of a pulling
	 * vehicle. Parking is a Phase 2 feature and not evaluated in Phase 1.
	 */
	protected boolean parkingAllowed = true;

	/**
	 * Fixed duration (in time units) for the transfer of load at this
	 * transshipment location. The value is stored as service time of the
	 * resulting node.
	 */
	protected float fixedTransferTime = 0;

	/**
	 * @param externID the externID to set
	 */
	public TransshipmentData setExternID(String externID) {
		this.externID = externID;
		return this;
	}

	/**
	 * @param geoId the geoId to set
	 */
	public TransshipmentData setGeoId(int geoId) {
		this.geoId = geoId;
		return this;
	}

	/**
	 * @param xlong the xlong to set
	 */
	public TransshipmentData setXlong(float xlong) {
		this.xlong = xlong;
		return this;
	}

	/**
	 * @param ylat the ylat to set
	 */
	public TransshipmentData setYlat(float ylat) {
		this.ylat = ylat;
		return this;
	}

	/**
	 * Sets an optional time window, where transshipment is allowed.
	 */
	public TransshipmentData setTimeWindow(float open, float close) {
		this.timeWindowList.add(new float[]{open, close});
		return this;
	}

	/**
	 * If false, no trailer can be parked (dropped) at this location. It is
	 * then a pure transshipment point. Parking is evaluated from Phase 2 on.
	 */
	public TransshipmentData setParkingAllowed(boolean parkingAllowed) {
		this.parkingAllowed = parkingAllowed;
		return this;
	}

	/**
	 * Sets the fixed duration (in seconds resp. time units of the metric),
	 * which is consumed by a vehicle, that transfers load at this location.
	 * Default is 0.
	 */
	public TransshipmentData setFixedTransferTime(float fixedTransferTime) {
		this.fixedTransferTime = fixedTransferTime;
		return this;
	}

	boolean isParkingAllowed() {
		return parkingAllowed;
	}

	float getFixedTransferTime() {
		return fixedTransferTime;
	}

	///////////////////////////////////

	Node createTransshipment(int idx) {
		checkTimeWindows();

		return new Node(
				idx,
				externID,
				SiteType.TRANSSHIPMENT,
				xlong,
				ylat,
				geoId,
				new float[0],
				timeWindowList.toArray(new float[0][]),
				fixedTransferTime,
				0,
				LoadType.UNDEF,
				0,
				"",
				0
		);
	}
}
