package xf.xfvrp.base;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class ShipmentConverter {

	/**
	 * 
	 * @param nodes
	 * @param customerList
	 */
	public static void convert(Node[] nodes, List<InternalCustomerData> customerList) {
		Map<String, Node> nodeMap = new HashMap<>();
		for (Node node: nodes)
			nodeMap.put(node.getExternID(), node);

		int shipmentIdx = 0;
		Map<String, Integer> shipmentIdxMap = new HashMap<>();
		for (InternalCustomerData iCust : customerList) {
			String shipID = iCust.getShipID();

			// Wenn shipID noch keinen Index zugewiesen bekommen hat,
			// dann füge einen neuen Index ein.
			if(!shipmentIdxMap.containsKey(shipID))
				shipmentIdxMap.put(shipID, shipmentIdx++);
			
			// Setze zu Customer-Objekt den Shipment-Index
			nodeMap.get(iCust.getExternID()).setShipmentIdx(shipmentIdxMap.get(shipID));
		}
	}
}
