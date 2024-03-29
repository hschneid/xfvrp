package xf.xfvrp.opt.init.precheck.pdp;

import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class PDPPreCheckService {

	private static boolean IGNORE_IMPROPER_AMOUNTS = false;

	public Node[] precheck(Node[] nodes, Vehicle vehicle) {
		Map<SiteType, List<Node>> nodesPerType = getNodesPerType(nodes);

		List<Node> customers = getCustomers(nodesPerType);

		removeUncompleteShipments(customers);

		Map<Integer, Node[]> shipments = getShipments(customers);

		checkShipments(shipments, customers);

		checkCapacity(customers, vehicle);

		return getValidNodes(customers, nodesPerType);
	}

	private Map<SiteType, List<Node>> getNodesPerType(Node[] nodes) {
		return Arrays.stream(nodes).collect(Collectors.groupingBy(Node::getSiteType));
	}

	private List<Node> getCustomers(Map<SiteType, List<Node>> nodesPerType) {
		return new ArrayList<>(nodesPerType.get(SiteType.CUSTOMER));
	}

	private void removeUncompleteShipments(List<Node> customers) {
		// No shipment id
		new ArrayList<>(customers).stream()
		.filter(c -> (c.getShipID() == null || c.getShipID().length() == 0))
		.forEach(c -> {
			c.setInvalidReason(InvalidReason.PDP_INCOMPLETE);
			customers.remove(c);
		});

		// Incomplete shipment
		new ArrayList<>(customers).stream().collect(Collectors.groupingBy(Node::getShipmentIdx))
		.values()
		.stream()
		.filter(list -> list.size() < 2)
		.flatMap(Collection::stream)
		.forEach(c -> {
			c.setInvalidReason(InvalidReason.PDP_INCOMPLETE);
			customers.remove(c);
		});

		// Too much nodes on shipment
		new ArrayList<>(customers).stream().collect(Collectors.groupingBy(Node::getShipmentIdx))
		.values()
		.stream()
		.filter(list -> list.size() > 2)
		.flatMap(Collection::stream)
		.forEach(c -> {
			c.setInvalidReason(InvalidReason.PDP_ILLEGAL_NUMBER_OF_CUSTOMERS_PER_SHIPMENT);
			customers.remove(c);
		});
	}

	private Map<Integer, Node[]> getShipments(List<Node> customers) {
		// Collect pairs of nodes of pickup and delivery (i.e. shipments)
		return customers.stream()
				.collect(Collectors.groupingBy(Node::getShipmentIdx))
				.values()
				.stream()
				.filter(list -> list.size() == 2)
				.map(list -> {
					Node[] pair = new Node[2];

					pair[0] = (list.get(0).getDemand()[0] > 0) ? list.get(0) : list.get(1);
					pair[1] = (list.get(0).getDemand()[0] < 0) ? list.get(0) : list.get(1);

					return pair;
				})
				.collect(Collectors.toMap(k -> k[0].getShipmentIdx(), v -> v, (v1, v2) -> v1));
	}

	private void checkShipments(Map<Integer, Node[]> shipments, List<Node> customers) {
		for (Node[] pair : shipments.values()) {
			Node src = pair[0];
			Node dst = pair[1];

			if (src.getDemand()[0] != -dst.getDemand()[0]) {
				if (IGNORE_IMPROPER_AMOUNTS) {
					float max = Math.max(Math.abs(src.getDemand()[0]), Math.abs(dst.getDemand()[0]));
					src.setDemand(max);
					dst.setDemand(-max);
				} else {
					src.setInvalidReason(InvalidReason.PDP_IMPROPER_AMOUNTS);
					customers.remove(src);
					dst.setInvalidReason(InvalidReason.PDP_IMPROPER_AMOUNTS);
					customers.remove(dst);
				}
			}

		}
	}

	private void checkCapacity(List<Node> customers, Vehicle vehicle) {
		for (Node customer : customers) {
			checkCapacityOfCustomer(customer, vehicle);
		}
	}

	private void checkCapacityOfCustomer(Node cust, Vehicle vehicle) {
		float[] demands = cust.getDemand();
		float[] capacities = vehicle.getCapacity();

		for (int compartment = 0; compartment < demands.length; compartment++) {
			float capacity = capacities[compartment];
			if(demands[compartment] > capacity) {
				cust.setInvalidReason(
						InvalidReason.PDP_IMPROPER_AMOUNTS,
						String.format("Demand of single customer is too big for vehicle. Customer %s - Compartment id = %d demand = %f available capacity = %f",
								cust.getExternID(), compartment, demands[compartment], capacity)
				);
				return;
			}
		}
	}

	private Node[] getValidNodes(List<Node> customers, Map<SiteType, List<Node>> nodesPerType) {
		List<Node> nodes = new ArrayList<>();
		nodes.addAll(nodesPerType.get(SiteType.DEPOT));
		if(nodesPerType.containsKey(SiteType.REPLENISH) && nodesPerType.get(SiteType.REPLENISH).size() > 0)
			nodes.addAll(nodesPerType.get(SiteType.REPLENISH));
		nodes.addAll(customers);

		return nodes.toArray(new Node[0]);
	}
}
