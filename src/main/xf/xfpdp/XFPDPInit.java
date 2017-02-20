package xf.xfpdp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public class XFPDPInit {

	public static boolean IGNORE_IMPROPER_AMOUNTS = true;

	/**
	 * 
	 */
	public Node[] precheck(Node[] nodes, boolean[] plannedNodes) {
		List<Node> customerList = Arrays.stream(nodes)
				.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
				.filter(n -> plannedNodes[n.getGlobalIdx()] == false)
				.collect(Collectors.toList());

		// Check and Mark nodes in invalid shipments (pair of pickup and delivery)
		Set<Node> localInvalidNodes = new HashSet<>();

		customerList.stream()
		.filter(c -> c.getShipID() == null)
		.forEach(c -> {
			c.setInvalidReason(InvalidReason.PDP_INCOMPLETE);
			customerList.remove(c);
		});

		Map<Integer, int[]> ships = collectPairs(customerList.toArray(new Node[0]));

		Set<Integer> okNodes = new HashSet<>();

		ships.forEach((k, pair) -> {
			if (pair[0] == -1 || pair[1] == -1) {
				if (pair[0] > -1) {
					Node n = customerList.get(pair[0]);
					n.setInvalidReason(InvalidReason.PDP_INCOMPLETE);
					customerList.remove(n);
				}
				if (pair[1] > -1) {
					Node n = customerList.get(pair[1]);
					n.setInvalidReason(InvalidReason.PDP_INCOMPLETE);
					customerList.remove(n);
				}
				return;
			}

			Node src = customerList.get(pair[0]);
			Node dst = customerList.get(pair[1]);

			if (pair[0] == pair[1]){
				src.setInvalidReason(InvalidReason.PDP_SOURCE_DEST_EQUAL);
				dst.setInvalidReason(InvalidReason.PDP_SOURCE_DEST_EQUAL);
				customerList.remove(src);
				customerList.remove(dst);
				
				return;
			}			
			if (src.getDemand()[0] != -dst.getDemand()[0]) {
				if (IGNORE_IMPROPER_AMOUNTS) {
					float max = Math.max(Math.abs(src.getDemand()[0]), Math.abs(dst.getDemand()[0]));
					src.setDemand(max);
					dst.setDemand(-max);
				} else {
					localInvalidNodes.add(src);
					src.setInvalidReason(InvalidReason.PDP_IMPROPER_AMOUNTS);
					localInvalidNodes.add(dst);
					dst.setInvalidReason(InvalidReason.PDP_IMPROPER_AMOUNTS);
					return;
				}
			}
			if (okNodes.contains(pair[0]) || okNodes.contains(pair[1])) {
				localInvalidNodes.add(src);
				src.setInvalidReason(InvalidReason.PDP_NODE_MULTIPLE_MENTIONS);
				localInvalidNodes.add(dst);
				dst.setInvalidReason(InvalidReason.PDP_NODE_MULTIPLE_MENTIONS);
				return;
			}

			okNodes.add(pair[0]);
			okNodes.add(pair[1]);
		});

		List<Node> newNodes = Arrays.stream(nodes)
				.filter(n -> n.getSiteType() == SiteType.DEPOT || n.getSiteType() == SiteType.REPLENISH)
				.collect(Collectors.toList());
		newNodes.addAll(customerList);
		
		return newNodes.toArray(new Node[0]);
	}

	/**
	 * 
	 * @param model
	 * @param invalidCustomers 
	 * @return
	 */
	public Solution buildInitPDP(XFVRPModel model, List<Node> invalidNodes) {
		Node[] nodeArr = model.getNodeArr();
		if (nodeArr.length == 0)
			return new Node[0];

		Map<Integer, int[]> ships = collectPairs(nodeArr);

		List<Node> gL = new ArrayList<>();
		int[] depotIdx = new int[]{0};
		int[] maxIdx = new int[]{0};

		ships.forEach((k, v) -> {
			int[] pair = v;

			if(!checkShipment(model, pair)) {
				invalidNodes.add(nodeArr[pair[0]]);
				invalidNodes.add(nodeArr[pair[1]]);
				return;
			}

			// Create a trivial route with a depot, pickup and delivery.
			gL.add(Util.createIdNode(nodeArr[depotIdx[0]], maxIdx[0]++));
			gL.add(nodeArr[pair[0]]);
			gL.add(nodeArr[pair[1]]);

			depotIdx[0] = ((depotIdx[0] + 1) % model.getNbrOfDepots());

			return;
		});

		if (gL.size() > 0) gL.add(Util.createIdNode(nodeArr[depotIdx[0]], maxIdx[0]++));

		return gL.toArray(new Node[0]);
	}

	/**
	 * 
	 * @param model
	 * @param shipment
	 * @return
	 */
	private boolean checkShipment(XFVRPModel model, int[] shipment) {
		Node depot = model.getNodeArr()[0];
		Node pick = model.getNodeArr()[shipment[0]];
		Node deli = model.getNodeArr()[shipment[1]];

		float travelTime = model.getTime(depot, pick);
		float travelTime2 = model.getTime(pick, deli);
		float travelTime3 = model.getTime(deli, depot);		

		// Check if customer is allowed for this vehicle type
		if(!pick.getPresetBlockVehicleList().isEmpty() && ! pick.getPresetBlockVehicleList().contains(model.getVehicle().idx)){
			pick.setInvalidReason(InvalidReason.WRONG_VEHICLE_TYPE);
			return false;
		}
		// Check if customer is allowed for this vehicle type
		if(!deli.getPresetBlockVehicleList().isEmpty() && ! deli.getPresetBlockVehicleList().contains(model.getVehicle().idx)){
			deli.setInvalidReason(InvalidReason.WRONG_VEHICLE_TYPE);
			return false;
		}

		// Check route duration with this customer
		float time = travelTime + travelTime2 + travelTime3 + pick.getServiceTime() + deli.getServiceTime();
		if(time > model.getVehicle().maxRouteDuration) {
			pick.setInvalidReason(InvalidReason.TRAVEL_TIME, "Customer " + pick.getExternID() + " - Traveltime required: " + time);
			deli.setInvalidReason(InvalidReason.TRAVEL_TIME, "Customer " + deli.getExternID() + " - Traveltime required: " + time);
			return false;
		}

		// Check time window

		// Arrival time at pickup
		float[] depTW = depot.getTimeWindow(0);
		float arrTime = depTW[0] + travelTime;
		float[] pickTW = pick.getTimeWindow(arrTime);
		arrTime = Math.max(arrTime, pickTW[0]);
		// Check pickup time window
		if(arrTime > pickTW[1]) {
			pick.setInvalidReason(InvalidReason.TIME_WINDOW);
			deli.setInvalidReason(InvalidReason.TIME_WINDOW);
			return false;
		}

		// Arrival time at delivery
		float arrTime2 = arrTime + pick.getServiceTime() + travelTime2;
		float[] deliTW = deli.getTimeWindow(arrTime2);
		arrTime2 = Math.max(arrTime2, deliTW[0]);
		// Check pickup time window
		if(arrTime2 > deliTW[1]) {
			pick.setInvalidReason(InvalidReason.TIME_WINDOW);
			deli.setInvalidReason(InvalidReason.TIME_WINDOW);
			return false;
		}

		// Check depot time window
		if(arrTime2 + travelTime3 + deli.getServiceTime() > depTW[1]) {
			pick.setInvalidReason(InvalidReason.TIME_WINDOW);
			deli.setInvalidReason(InvalidReason.TIME_WINDOW);
			return false;
		}

		return true;
	}

	/**
	 * 
	 * @param nodes
	 * @return
	 */
	protected Map<Integer, int[]> collectPairs(Node[] nodes) {
		// Collect pairs of nodes of pickup and delivery (i.e. shipments)
		Map<Integer, int[]> map = new HashMap<>();

		for (int i = 0; i < nodes.length; i++) {
			Node node = nodes[i];

			if (node.getSiteType() != SiteType.CUSTOMER) 
				continue;

			int sIdx = node.getShipmentIdx();

			if (!map.containsKey(sIdx))
				map.put(sIdx, new int[]{-1,-1});

			int pos = -1;
			if (node.getDemand()[0] > 0) 
				pos = 0;
			else if (node.getDemand()[0] < 0) 
				pos = 1;
			else
				throw new IllegalArgumentException("Load type "	+ node.getLoadType().name()	+ 
						" is not applicable for PDP optimization.");

			int[] pair = map.get(sIdx);
			if (pair[pos] == -1) 
				pair[pos] = i;
		}

		return map;
	}
}
