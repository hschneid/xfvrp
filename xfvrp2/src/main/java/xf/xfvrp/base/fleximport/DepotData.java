package xf.xfvrp.base.fleximport;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Depot data class defines all fields which can be used
 * in XFVRP suite. Users can call the set-Methods to insert
 * data into this object. There is no predefined sequence of 
 * doing that. All fields are initialized by default values 
 * in that way, that restrictions are always holds.
 * 
 * Each set-method returns the depot data object itself, so
 * that the set-methods can be called in one way.
 * 
 * There is no way to clear a value back to default value. The
 * user has to do that by himself. All values can be set multiple times, 
 * where last set value is overwritten with only one exception:
 * It is possible to set multiple time windows. It is not possible
 * to clear the list of inserted time windows.
 * 
 * @author hschneid
 *
 */
public abstract class DepotData extends NodeData {

	/**
	 * @param externID the externID to set
	 */
	public DepotData setExternID(String externID) {
		this.externID = externID;
		return this;
	}

	/**
	 * @param geoId the geoId to set
	 */
	public DepotData setGeoId(int geoId) {
		this.geoId = geoId;
		return this;
	}

	/**
	 * @param xlong the xlong to set
	 */
	public DepotData setXlong(float xlong) {
		this.xlong = xlong;
		return this;
	}

	/**
	 * @param ylat the ylat to set
	 */
	public DepotData setYlat(float ylat) {
		this.ylat = ylat;
		return this;
	}

	/**
	 * 
	 * @param open
	 * @param close
	 * @return
	 */
	public DepotData setTimeWindow(float open, float close) {
		this.timeWindowList.add(new float[]{open, close});
		return this;
	}

	/**
	 * @param open1 the open1 to set
	 */
	public DepotData setOpen1(float open1) {
		this.open1 = open1;
		return this;
	}

	/**
	 * @param close1 the close1 to set
	 */
	public DepotData setClose1(float close1) {
		this.close1 = close1;
		return this;
	}

	/**
	 * @param open2 the open2 to set
	 */
	public DepotData setOpen2(float open2) {
		this.open2 = open2;
		return this;
	}

	/**
	 * @param close2 the close2 to set
	 */
	public DepotData setClose2(float close2) {
		this.close2 = close2;
		return this;
	}
}
