package xf.xfvrp.opt.init.precheck.vrp;

import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.base.preset.BlockNameConverter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This is the standard initialization of the giant tour
 * out of the read data. The first thing of an optimization
 * process is the creation of such a giant tour. Hereby all
 * orders/customers will be arranged in a way, that each
 * order/customer is visited by its own tour.
 * 
 * If multiple depots are available, each single route has
 * an alternating depot.
 * 
 * Some construction optimization methods ignore the giant tours
 * and rebuild their own. But for reasons of convenience in
 * all cases a giant tour is initialized by this class.
 * 
 * If orders/customers can not be served in a valid way with the
 * given restrictions, they are excluded in an invalid list. These
 * orders/customers are reinserted in a later step of optimization.
 * 
 * @author hschneid
 *
 */
public class VRPPreCheckService  {
	
	/**
	 * Structural checks of the nodes without model 
	 */
	public Node[] precheck(Node[] nodes, Vehicle vehicle) throws XFVRPException {
		checkFeasibility(nodes);

		// Fetch block informations
		Map<Integer, List<Node>> blocks = getBlocks(nodes);

		List<Node> plannedNodes = getPlannedNodes(nodes);

		// Check if customer is allowed for this vehicle type
		checkVehicleType(nodes, vehicle, blocks, plannedNodes);

		return plannedNodes.toArray(new Node[0]);
	}

	private void checkFeasibility(Node[] nodes) throws XFVRPException {
		if(nodes.length == 0) {
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_INPUT, "No nodes found.");
		}
	}

	private void checkVehicleType(Node[] nodes, Vehicle vehicle, Map<Integer, List<Node>> blocks, List<Node> plannedNodes) {
		for (Node node : nodes) {
			if(node.getSiteType() == SiteType.CUSTOMER) {
				if(!node.getPresetBlockVehicleList().isEmpty() && !node.getPresetBlockVehicleList().contains(vehicle.idx)) {
					// Remove invalid customer from nodes list
					removeNode(plannedNodes, node, InvalidReason.WRONG_VEHICLE_TYPE);

					// Remove all customers from block of invalid customer
					removeCustomersOfBlock(blocks, plannedNodes, node);
				}
			} 
		}
	}

	private void removeCustomersOfBlock(Map<Integer, List<Node>> blocks, List<Node> plannedNodes, Node node) {
		if(blocks.containsKey(node.getPresetBlockIdx())) {
			blocks.get(node.getPresetBlockIdx())
				.forEach(n -> removeNode(plannedNodes, n, InvalidReason.WRONG_VEHICLE_TYPE));
		}
	}

	private void removeNode(List<Node> plannedNodes, Node node, InvalidReason invalidReason) {
		node.setInvalidReason(invalidReason);
		plannedNodes.remove(node);
	}
	
	private Map<Integer, List<Node>> getBlocks(Node[] nodes) {
		return Arrays.stream(nodes)
			.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
			.filter(n-> n.getPresetBlockIdx() != BlockNameConverter.DEFAULT_BLOCK_IDX)
			.collect(Collectors.groupingBy(Node::getPresetBlockIdx));
	}

	private List<Node> getPlannedNodes(Node[] nodes) {
		// Already planned customers (true = planned, false = unplanned, DEPOTS/REPLENISH always false)
		return Arrays.asList(nodes);		
	}
}
