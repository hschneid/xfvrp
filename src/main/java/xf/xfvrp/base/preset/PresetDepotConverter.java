package xf.xfvrp.base.preset;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
		Map<String, Set<String>> presetMap = getPresetDepotsByExternID(customerList);

		Map<String, Integer> depotIdxMap = getDepotIdxByExternID(nodes);

		setPresetDepotIndex(nodes, presetMap, depotIdxMap, st);
	}

	private static void setPresetDepotIndex(Node[] nodes, Map<String, Set<String>> presetMap,
			Map<String, Integer> depotIdxMap, StatusManager st) {
		for (Node node : nodes) {
			if(presetMap.containsKey(node.getExternID())) {
				presetMap.get(node.getExternID()).forEach(id -> {
					if(!depotIdxMap.containsKey(id))
						st.fireMessage(StatusCode.EXCEPTION, "Could not found preset depot extern id "+id+" in depots.");
					else
						node.addPresetDepot(depotIdxMap.get(id));
				});
			}
		}
	}

	private static Map<String, Integer> getDepotIdxByExternID(Node[] nodes) {
		return Arrays.stream(nodes)
				.filter(node -> node.getSiteType() == SiteType.DEPOT)
				.collect(Collectors.toMap(Node::getExternID, Node::getGlobalIdx, (v1, v2) -> v1));
	}

	private static Map<String, Set<String>> getPresetDepotsByExternID(List<InternalCustomerData> customerList) {
		return customerList.stream()
				.collect(Collectors.toMap(
						InternalCustomerData::getExternID,
						InternalCustomerData::getPresetDepotList,
						(v1, v2) -> v1
						));
	}
}
