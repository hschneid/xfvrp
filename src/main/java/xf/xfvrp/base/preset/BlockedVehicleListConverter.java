package xf.xfvrp.base.preset;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.fleximport.CustomerData;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;

import java.util.*;
import java.util.stream.Collectors;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
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
	public static void convert(Node[] nodes, List<CustomerData> customers, Vehicle[] vehicles, StatusManager mon) {
		Map<String, Integer> vehMap = getVehicleMapping(vehicles);

		Map<String, Node> nodeMap = getNodeMapping(nodes);

		addPreset(customers, vehMap, nodeMap, mon);
	}

	private static void addPreset(List<CustomerData> customers, Map<String, Integer> vehicles, Map<String, Node> nodes, StatusManager mon) {
		for (CustomerData cust : customers) {
			Node node = nodes.get(cust.getExternID());
			
			Set<String> presetBlockedVehicles = cust.getPresetBlockVehicleList();

			if(presetBlockedVehicles == null || presetBlockedVehicles.size() == 0)
				continue;

			presetBlockedVehicles.forEach(vehName -> {
				vehName = vehName.trim();

				if(vehicles.containsKey(vehName))
					node.addPresetVehicle(vehicles.get(vehName));
				else 
					mon.fireMessage(StatusCode.EXCEPTION, "Found blocked vehicle name "+vehName+" for node "+node.getExternID()+" could not be bound to existing vehicle names. It will be ignored.");
			});
		}		
	}

	private static Map<String, Integer> getVehicleMapping(Vehicle[] vehicles) {
		return Arrays.stream(vehicles)
				.collect(Collectors.toMap(Vehicle::getName, Vehicle::getIdx, (v1, v2) -> v1));
	}

	private static Map<String, Node> getNodeMapping(Node[] nodes) {
		return Arrays.stream(nodes).collect(Collectors.toMap(Node::getExternID, v -> v, (v1, v2) -> v1));
	}
}
