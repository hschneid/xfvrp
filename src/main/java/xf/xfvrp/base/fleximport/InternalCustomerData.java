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
	 * @return the nbrOfPackages
	 */
	public int getNbrOfPackages() {
		return nbrOfPackages;
	}

	/**
	 * @return the heightOfPackage
	 */
	public int getHeightOfPackage() {
		return heightOfPackage;
	}

	/**
	 * @return the widthOfPackage
	 */
	public int getWidthOfPackage() {
		return widthOfPackage;
	}

	/**
	 * @return the lengthOfPackage
	 */
	public int getLengthOfPackage() {
		return lengthOfPackage;
	}

	/**
	 * @return the weightOfPackage
	 */
	public float getWeightOfPackage() {
		return weightOfPackage;
	}

	/**
	 * @return the loadBearingOfPackage
	 */
	public float getLoadBearingOfPackage() {
		return loadBearingOfPackage;
	}

	/**
	 * @return the stackingGroupOfPackage
	 */
	public int getStackingGroupOfPackage() {
		return stackingGroupOfPackage;
	}

	/**
	 * @return the containerTypeOfPackage
	 */
	public int getContainerTypeOfPackage() {
		return containerTypeOfPackage;
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
				shipID,
				nbrOfPackages,
				heightOfPackage,
				widthOfPackage,
				lengthOfPackage,
				weightOfPackage,
				loadBearingOfPackage,
				stackingGroupOfPackage,
				containerTypeOfPackage
				);

		return n;
	}

	/**
	 * 
	 * @return
	 */
	public String exportToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("$");
		for (float[] fs : timeWindowList)
			sb.append(fs[0]+"#"+fs[1]+"$");
		sb.append("\t");

		StringBuilder sb1 = new StringBuilder();
		sb1.append("$");
		for (String s : presetBlockVehicleList)
			sb1.append(s+"$");
		sb1.append("\t");

		StringBuilder sb2 = new StringBuilder();
		sb2.append("$");
		for (String s : presetRoutingBlackList)
			sb2.append(s+"$");
		sb2.append("\t");

		return
				"CUSTOMER\t"+
				externID+"\t"+
				geoId+"\t"+
				xlong+"\t"+
				ylat+"\t"+
				sb.toString()+
				loadType.toString()+"\t"+
				exportArrayToString(demand)+"\t"+
				serviceTime+"\t"+
				serviceTimeForSite+"\t"+
				shipID+"\t"+
				nbrOfPackages+"\t"+
				heightOfPackage+"\t"+
				widthOfPackage+"\t"+
				lengthOfPackage+"\t"+
				weightOfPackage+"\t"+
				loadBearingOfPackage+"\t"+
				stackingGroupOfPackage+"\t"+
				containerTypeOfPackage+"\t"+
				presetBlockName+"\t"+
				presetBlockPos+"\t"+
				presetBlockRank+"\t"+
				sb1.toString()+
				sb2.toString()+
				"\n";
	}
	
	private static String exportArrayToString(float[] arr){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arr.length; i++)
			sb.append(arr[i]+";");
		return sb.toString();
	}
}
