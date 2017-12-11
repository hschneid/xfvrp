package xf.xfvrp.base.fleximport;

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
public class InternalReplenishData extends ReplenishData {

	/**
	 * 
	 * @param idx
	 * @return
	 */
	public Node createReplenishment(int idx) {
		checkTimeWindows();
				
		Node n = new Node(
				idx,
				externID,
				SiteType.REPLENISH,
				xlong,
				ylat,
				geoId,
				new float[]{0, 0, 0},
				timeWindowList.toArray(new float[0][]),
				0,
				0,
				LoadType.REPLENISH,
				0,
				""
				);
		
		return n;
	}
}
