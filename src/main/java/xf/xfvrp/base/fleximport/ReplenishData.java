package xf.xfvrp.base.fleximport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
public abstract class ReplenishData implements Serializable{
	private static final long serialVersionUID = -5651090849497978139L;
	
	protected String externID = "";
	protected int geoId = -1;

	protected float xlong = 0;
	protected float ylat = 0;
	protected List<float[]> timeWindowList = new ArrayList<>();

	protected float open1 = 0;
	protected float close1 = Integer.MAX_VALUE;
	protected float open2 = 0;
	protected float close2 = Integer.MAX_VALUE;


	/**
	 * Internal method to check the time window list.
	 * 
	 * If no time windows are given, a default time window
	 * is inserted.
	 * 
	 * This method is to clear data structure for internal use. 
	 */
	protected void checkTimeWindows() {
		if(timeWindowList.size() < 2)
			if(open1 != 0 || close1 != Integer.MAX_VALUE)
				timeWindowList.add(new float[]{open1, close1});
		if(timeWindowList.size() < 2)
			if(open2 != 0 || close2 != Integer.MAX_VALUE)
				timeWindowList.add(new float[]{open2, close2});

		// Bigger time window overrides smaller ones.
		Collections.sort(timeWindowList, new Comparator<float[]>() {
			@Override
			public int compare(float[] o1, float[] o2) {
				return (int)((o1[0] - o2[0]) * 1000f);
			}
		});
		
		// If no time window is given, insert the default time window
		if(timeWindowList.size() == 0)
			timeWindowList.add(new float[]{0, Integer.MAX_VALUE});
	}

	/**
	 * @param externID the externID to set
	 */
	public ReplenishData setExternID(String externID) {
		this.externID = externID;
		return this;
	}

	/**
	 * @param geoId the geoId to set
	 */
	public ReplenishData setGeoId(int geoId) {
		this.geoId = geoId;
		return this;
	}

	/**
	 * @param xlong the xlong to set
	 */
	public ReplenishData setXlong(float xlong) {
		this.xlong = xlong;
		return this;
	}

	/**
	 * @param ylat the ylat to set
	 */
	public ReplenishData setYlat(float ylat) {
		this.ylat = ylat;
		return this;
	}

	/**
	 * 
	 * @param open
	 * @param close
	 * @return
	 */
	public ReplenishData setTimeWindow(float open, float close) {
		this.timeWindowList.add(new float[]{open, close});
		return this;
	}

	/**
	 * @param open1 the open1 to set
	 */
	public ReplenishData setOpen1(float open1) {
		this.open1 = open1;
		return this;
	}

	/**
	 * @param close1 the close1 to set
	 */
	public ReplenishData setClose1(float close1) {
		this.close1 = close1;
		return this;
	}

	/**
	 * @param open2 the open2 to set
	 */
	public ReplenishData setOpen2(float open2) {
		this.open2 = open2;
		return this;
	}

	/**
	 * @param close2 the close2 to set
	 */
	public ReplenishData setClose2(float close2) {
		this.close2 = close2;
		return this;
	}

}
