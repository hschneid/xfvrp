package xf.xfvrp;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;

public class RoutingDataBag {
	Node[] nodes;
	Vehicle vehicle;

	public RoutingDataBag(Node[] nodes, Vehicle vehicle) {
		this.nodes = nodes;
		this.vehicle = vehicle;
	}

}
