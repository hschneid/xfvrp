package xf.xfvrp.base.preset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.fleximport.InternalCustomerData;

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
public class BlackListIDConverter {

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
		Map<String, Set<String>> blackListMap = new HashMap<>();
		Map<String, Node> nodeMap = new HashMap<>();

		// Translate Node name to Node object
		for (Node n : nodes)
			if(n.getSiteType() == SiteType.CUSTOMER)
				nodeMap.put(n.getExternID(), n);

		// Map ExternID with external black list
		customerList.forEach(cust -> blackListMap.put(cust.getExternID(), cust.getPresetRoutingBlackList()));

		// Allocate for each internal node the internal black list
		for (Node n : nodes) {
			if(blackListMap.containsKey(n.getExternID())) {
				Set<String> blackList = blackListMap.get(n.getExternID());

				// Translate black listed node name to node idx
				blackList.forEach(blackName -> {
					if(nodeMap.containsKey(blackName))
						n.addToBlacklist(nodeMap.get(blackName).getGlobalIdx());
				});
			}
		}
	}

}
