package xf.xfvrp.base;

import xf.xfvrp.base.fleximport.InternalCustomerData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			String shipId = iCust.getShipID();
			
			if(shipId == null || shipId.length() == 0)
				continue;

			// Wenn shipID noch keinen Index zugewiesen bekommen hat,
			// dann füge einen neuen Index ein.
			if(!shipmentIdxMap.containsKey(shipId))
				shipmentIdxMap.put(shipId, shipmentIdx++);
			
			// Setze zu Customer-Objekt den Shipment-Index
			nodeMap.get(iCust.getExternID()).setShipmentIdx(shipmentIdxMap.get(shipId));
		}
	}
}
