package xf.xfvrp.base.preset;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.fleximport.CustomerData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Converts the trailer accessibility flag of the imported customers into the
 * internal preset vehicle mechanism. The preset vehicle list of a node is an
 * allowlist: If it is not empty, the node is only allowed to be served by the
 * vehicles in the list.
 * <p>
 * A customer, which is not trailer accessible (lorry customer), is allowed
 * only for autonomous vehicles, which pull no trailer (pure lorries). If the
 * fleet contains no pure lorry, the lorry customer cannot be served at all
 * and is marked unservable by an artificial vehicle index.
 *
 * @author hschneid
 */
public class TrailerAccessibilityConverter {

	/**
	 * Artificial vehicle index for customers, which cannot be served by any
	 * vehicle of the fleet.
	 */
	public static final int UNSERVABLE_MARKER = -1;

	/**
	 * Restricts all lorry customers to pure lorries (no trailer capability).
	 */
	public static void convert(Node[] nodes, List<CustomerData> customers, Vehicle[] vehicles) {
		Map<String, Node> nodeMap = getNodeMapping(nodes);

		List<Integer> pureLorryIdx = new ArrayList<>();
		for (Vehicle veh : vehicles)
			if (isPureLorry(veh))
				pureLorryIdx.add(veh.getIdx());

		for (CustomerData cust : customers) {
			if (cust.isTrailerAccessible())
				continue;

			Node node = nodeMap.get(cust.getExternID());
			if (node == null)
				continue;

			Set<Integer> presetAllowedVehicles = node.getPresetBlockVehicleList();

			Set<Integer> newAllowedVehicles = new HashSet<>();
			for (Integer vehicleIdx : pureLorryIdx)
				if (presetAllowedVehicles.isEmpty() || presetAllowedVehicles.contains(vehicleIdx))
					newAllowedVehicles.add(vehicleIdx);

			// No vehicle can serve this lorry customer
			if (newAllowedVehicles.isEmpty())
				newAllowedVehicles.add(UNSERVABLE_MARKER);

			node.getPresetBlockVehicleList().clear();
			node.getPresetBlockVehicleList().addAll(newAllowedVehicles);
		}
	}

	/**
	 * A vehicle is trailer capable, if it is coupled to a trailer. A pure
	 * lorry is autonomous and pulls no trailer.
	 */
	public static boolean isPureLorry(Vehicle veh) {
		return veh.isAutonomous() && !isTrailerCapable(veh);
	}

	/**
	 * A vehicle is trailer capable, if it is coupled to a trailer.
	 */
	public static boolean isTrailerCapable(Vehicle veh) {
		return veh.getTrailerId() != Vehicle.NO_TRAILER;
	}

	private static Map<String, Node> getNodeMapping(Node[] nodes) {
		return Arrays.stream(nodes)
				.collect(Collectors.toMap(Node::getExternID, v -> v, (v1, v2) -> v1));
	}
}
