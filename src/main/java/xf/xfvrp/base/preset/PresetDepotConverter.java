package xf.xfvrp.base.preset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.fleximport.InternalCustomerData;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class PresetDepotConverter {

	/**
	 * Added the preset depot constraint, which means, that
	 * a customer is fixed allocated to a depot or multiple depots.
	 * 
	 * 
	 * @param nodes
	 * @param customerList
	 * @param st
	 */
	public static final void convert(Node[] nodes, List<InternalCustomerData> customerList, StatusManager st) {
		Map<String, Set<String>> presetMap = new HashMap<>();
		customerList.forEach(cust -> presetMap.put(cust.getExternID(), cust.getPresetDepotList()));

		Map<String, Integer> depotIdxMap = new HashMap<>();
		for (Node n : nodes)
			if(n.getSiteType() == SiteType.DEPOT)
				depotIdxMap.put(n.getExternID(), n.getGlobalIdx());


		// Converting external preset depot string to internal preset depot index (global)
		for (Node node : nodes) {
			if(presetMap.containsKey(node.getExternID())) {
				Set<String> idSet = presetMap.get(node.getExternID());
				idSet.forEach(id -> {
					if(!depotIdxMap.containsKey(id))
						st.fireMessage(StatusCode.EXCEPTION, "Could not found preset depot extern id "+id+" in depots.");
					else
						node.addPresetDepot(depotIdxMap.get(id));
				});
			}
		}
	}
}
