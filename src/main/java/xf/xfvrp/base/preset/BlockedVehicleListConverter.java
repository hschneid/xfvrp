package xf.xfvrp.base.preset;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.fleximport.InternalCustomerData;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
	 */
	public static void convert(Node[] nodes, List<InternalCustomerData> customerList, Vehicle[] vehicles, StatusManager mon) {
		Map<String, Integer> vehMap = getVehicleMapping(vehicles);

		Map<String, Node> nodeMap = getNodeMapping(nodes);

		addPreset(customerList, vehMap, nodeMap, mon);
	}

	private static void addPreset(List<InternalCustomerData> customerList, Map<String, Integer> vehMap, Map<String, Node> nodeMap, StatusManager mon) {
		for (InternalCustomerData cust : customerList) {
			Node node = nodeMap.get(cust.getExternID());
			
			Set<String> presetBlockedVehicles = cust.getPresetBlockVehicleList();
			
			if(presetBlockedVehicles == null)
				continue;

			presetBlockedVehicles.forEach(vehName -> {
				vehName = vehName.trim();

				if(vehMap.containsKey(vehName)) 
					node.addPresetVehicle(vehMap.get(vehName));
				else 
					mon.fireMessage(StatusCode.EXCEPTION, "Found blocked vehicle name "+vehName+" for node "+node.getExternID()+" could not be bound to existing vehicle names. It will be ignored.");
			});
		}		
	}

	private static Map<String, Integer> getVehicleMapping(Vehicle[] vehicles) {
		return Arrays.stream(vehicles).collect(Collectors.toMap(k -> k.name, v -> v.idx, (v1, v2) -> v1));
	}

	private static Map<String, Node> getNodeMapping(Node[] nodes) {
		return Arrays.stream(nodes).collect(Collectors.toMap(k -> k.getExternID(), v -> v, (v1, v2) -> v1));
	}
}
