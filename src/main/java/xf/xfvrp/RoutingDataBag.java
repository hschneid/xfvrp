package xf.xfvrp;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;

/**
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class RoutingDataBag {
	Node[] nodes;
	Vehicle vehicle;

	public RoutingDataBag(Node[] nodes, Vehicle vehicle) {
		this.nodes = nodes;
		this.vehicle = vehicle;
	}

}
