package xf.xfvrp.base.preset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
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
public class BlockedVehicleListConverter {

	/**
	 * Sets for a node the blocked vehicle object which was given in
	 * the input data
	 * 
	 * @param node Nodes without associated blocked vehicle objects
	 * @param customerList Contains the input data
	 * @param vehicles
	 * @param mon
	 * @return Node with associated blocked vehicle objects
	 */
	public static void convert(Node[] nodes, List<InternalCustomerData> customerList, Vehicle[] vehicles, StatusManager mon) {
		// Build map of vehicle name and vehicle index
		Map<String, Integer> vehMap = new HashMap<>();
		for (Vehicle veh : vehicles)
			vehMap.put(veh.name, veh.idx);

		Map<String, Node> nodeMap = new HashMap<>();
		for (Node node : nodes)
			nodeMap.put(node.getExternID(), node);

		// Allocate found tokens in listAsString to vehicle index
		customerList.forEach(cust -> {
			Node node = nodeMap.get(cust.getExternID());

			Set<String> blockedVehicleList = cust.getPresetBlockVehicleList();
			if(blockedVehicleList.size() > 0) {
				for (String vehName : blockedVehicleList) {
					vehName = vehName.trim();

					if(vehMap.containsKey(vehName)) 
						node.getPresetBlockVehicleList().add(vehMap.get(vehName));
					else 
						mon.fireMessage(StatusCode.RUNNING, "Found blocked vehicle name "+vehName+" for node "+node.getExternID()+" could not be bound to existing vehicle names. It will be ignored.");
				}
			}
		});
	}
}
