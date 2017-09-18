package xf.xfvrp.opt.init.precheck.pdp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;

public class PDPPreCheckService {

	public static boolean IGNORE_IMPROPER_AMOUNTS = false;
	
	public Node[] precheck(Node[] nodes, Vehicle vehicle, boolean[] plannedCustomers) {
		Map<SiteType, List<Node>> nodesPerType = getNodesPerType(nodes);
		
		List<Node> customers = getCustomers(plannedCustomers, nodesPerType);
		
		removeUncompleteShipments(customers);
		
		Map<Integer, Node[]> shipments = getShipments(customers);
		
		checkShipments(shipments, customers);
		
		checkCapactiy(customers, vehicle);
		
		return getValidNodes(customers, nodesPerType);
	}

	private Map<SiteType, List<Node>> getNodesPerType(Node[] nodes) {
		Map<SiteType, List<Node>> nodesPerType = Arrays.stream(nodes)
				.collect(Collectors.groupingBy(k -> k.getSiteType()));
		return nodesPerType;
	}

	private List<Node> getCustomers(boolean[] plannedCustomers, Map<SiteType, List<Node>> nodesPerType) {
		return nodesPerType.get(SiteType.CUSTOMER)
				.stream()
				.filter(n -> plannedCustomers[n.getGlobalIdx()] == false)
				.collect(Collectors.toList());
	}

	private void removeUncompleteShipments(List<Node> customers) {
		// No shipment id
		customers.stream()
		.filter(c -> c.getShipID() == null)
		.forEach(c -> {
			c.setInvalidReason(InvalidReason.PDP_INCOMPLETE);
			customers.remove(c);
		});
		
		// Incomplete shipment
		customers.stream().collect(Collectors.groupingBy(k -> k.getShipmentIdx()))
		.values()
		.stream()
		.filter(list -> list.size() < 2)
		.flatMap(list -> list.stream())
		.forEach(c -> {
			c.setInvalidReason(InvalidReason.PDP_INCOMPLETE);
			customers.remove(c);
		});
		
		// Too much nodes on shipment
		customers.stream().collect(Collectors.groupingBy(k -> k.getShipmentIdx()))
		.values()
		.stream()
		.filter(list -> list.size() > 2)
		.flatMap(list -> list.stream())
		.forEach(c -> {
			c.setInvalidReason(InvalidReason.PDP_ILLEGAL_NUMBER_OF_CUSTOMERS_PER_SHIPMENT);
			customers.remove(c);
		});
	}
	
	private Map<Integer, Node[]> getShipments(List<Node> customers) {
		// Collect pairs of nodes of pickup and delivery (i.e. shipments)
		return customers.stream()
				.collect(Collectors.groupingBy(k -> k.getShipmentIdx()))
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
		shipments.values().forEach(pair -> {
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
					return;
				}
			}
		});
	}
	
	/**
	 * 
	 * @param node
	 * @param amountVal
	 * @return
	 */
	private void checkCapactiy(List<Node> customers, Vehicle vehicle) {
		for (Node customer : customers) {
			for (int j = 0; j < customer.getDemand().length; j++)
				if(customer.getDemand()[j] > vehicle.capacity[j]) {
					customer.setInvalidReason(InvalidReason.PDP_IMPROPER_AMOUNTS);
					break;
				}
		}
	}

	private Node[] getValidNodes(List<Node> customers, Map<SiteType, List<Node>> nodesPerType) {
		List<Node> nodes = new ArrayList<>();
		nodes.addAll(nodesPerType.get(SiteType.DEPOT));
		nodes.addAll(nodesPerType.get(SiteType.REPLENISH));
		nodes.addAll(customers);
		
		return nodes.toArray(new Node[0]);
	}
}
