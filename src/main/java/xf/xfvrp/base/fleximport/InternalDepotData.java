package xf.xfvrp.base.fleximport;

import java.util.List;

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
 * The InternalDepotData class is only for interal use. 
 * It holds the same data like DepotData.
 * 
 * It gives access to the inserted data for XFVRP suite. 
 * So the user wont see internal variable names or data
 * structures.
 * 
 * @author hschneid
 *
 */
public class InternalDepotData extends DepotData {
	private static final long serialVersionUID = -6291492608727438849L;

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
	 * @return the timeWindowList
	 */
	public List<float[]> getTimeWindowList() {
		return timeWindowList;
	}

	/**
	 * 
	 * @param idx
	 * @return
	 */
	public Node createDepot(int idx) {
		checkTimeWindows();
		Node n = new Node(
				idx,
				externID,
				SiteType.DEPOT,
				xlong,
				ylat,
				geoId,
				new float[]{0,0,0},
				timeWindowList.toArray(new float[0][]),
				0,
				0,
				null, 0, 0,
				"",0,0,0,0,0,0,0,0 // Load planning parameters
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
		
		return
				"DEPOT\t"+
				externID+"\t"+
				geoId+"\t"+
				xlong+"\t"+
				ylat+"\t"+
				sb.toString()+
				"\n";
	}

}
