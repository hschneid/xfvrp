package xf.xfvrp.base.fleximport;

import java.util.List;
import java.util.Set;

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
 * InternalCustomerData is the internal interface of CustomerData.
 * Internal classes can capture the information of external filled data.
 * 
 * It is only for masking access privileges.
 * 
 * @author hschneid
 *
 */
public class InternalCustomerData extends CustomerData {
	private static final long serialVersionUID = -5336103246844878384L;

	/**
	 * @return the externID
	 */
	public String getExternID() {
		return externID;
	}

	/**
	 * @return the geoId
	 */
	public int getGeoId() {
		return geoId;
	}

	/**
	 * @return the loadType
	 */
	public LoadType getLoadType() {
		return loadType;
	}

	/**
	 * @return the xlong
	 */
	public float getXlong() {
		return xlong;
	}

	/**
	 * @return the ylat
	 */
	public float getYlat() {
		return ylat;
	}

	/**
	 * @return the demand
	 */
	public float[] getDemand() {
		return demand;
	}

	/**
	 * @return the timeWindowList
	 */
	public List<float[]> getTimeWindowList() {
		return timeWindowList;
	}

	/**
	 * @return the serviceTime
	 */
	public float getServiceTime() {
		return serviceTime;
	}

	/**
	 * @return the shipID
	 */
	public String getShipID() {
		return shipID;
	}

	/**
	 * @return the presetBlockName
	 */
	public String getPresetBlockName() {
		return presetBlockName;
	}

	/**
	 * @return the presetBlockVehicleList
	 */
	public Set<String> getPresetBlockVehicleList() {
		return presetBlockVehicleList;
	}

	/**
	 * @return the serviceTimeForSite
	 */
	public float getServiceTimeForSite() {
		return serviceTimeForSite;
	}

	/**
	 * @return the presetRoutingBlackList
	 */
	public Set<String> getPresetRoutingBlackList() {
		return presetRoutingBlackList;
	}
	
	/**
	 * @return the presetDepotList
	 */
	public Set<String> getPresetDepotList() {
		return presetDepotList;
	}
	
	public int getPresetBlockPosition() {
		return presetBlockPos;
	}

	/**
	 * 
	 * @param idx
	 * @return
	 */
	public Node createCustomer(int idx) {
		checkTimeWindows();

		Node n = new Node(
				idx,
				externID,
				SiteType.CUSTOMER,
				xlong,
				ylat,
				geoId,
				demand,
				timeWindowList.toArray(new float[0][]),
				serviceTime,
				serviceTimeForSite,
				loadType,
				presetBlockPos,
				presetBlockRank,
				shipID
				);

		return n;
	}
}
