package xf.xfvrp.base.preset;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.fleximport.InternalCustomerData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class PresetBlacklistedNodeConverter {

	/**
	 * This method converts the external information about black listed customer combinations
	 * into the internal model. Each customer can define a set of other customers, which are not
	 * allowed to be planned together on the same route.
	 * 
	 * External informations are the node ids. Internal informations are the global index of the customer.
	 * 
	 * @param nodes List of internal node objects
	 * @param customerList List of external customer informations
	 */
	public static void convert(Node[] nodes, List<InternalCustomerData> customerList) {
		Map<String, Integer> indexes = allocateNodeIndexByExternID(nodes);

		Map<String, Set<String>> blacklistedNodes = allocateBlacklistedNodesByExternID(customerList);

		setBlacklistedNodeIndexes(nodes, indexes, blacklistedNodes);
	}

	private static void setBlacklistedNodeIndexes(Node[] nodes, Map<String, Integer> indexes,
			Map<String, Set<String>> blacklistedNodes) {
		for (Node n : nodes) {
			if(blacklistedNodes.containsKey(n.getExternID())) {
				// Translate black listed node name to node idx
				blacklistedNodes.get(n.getExternID())
					.stream()
					.filter(blackName -> indexes.containsKey(blackName))
					.map(blackName -> indexes.get(blackName))
					.forEach(f -> n.addToBlacklist(f));
			}
		}
	}

	private static Map<String, Set<String>> allocateBlacklistedNodesByExternID(List<InternalCustomerData> customerList) {
		return customerList.stream().collect(Collectors.toMap(InternalCustomerData::getExternID, InternalCustomerData::getPresetRoutingBlackList, (v1, v2) -> v1));
	}

	private static Map<String, Integer> allocateNodeIndexByExternID(Node[] nodes) {
		return Arrays.stream(nodes).filter(f -> f.getSiteType() == SiteType.CUSTOMER).collect(Collectors.toMap(k -> k.getExternID(), v -> v.getGlobalIdx(), (v1, v2) -> v1));
	}
}
