package xf.xfvrp;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.compartment.CompartmentType;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class RoutingDataBag {

	Node[] nodes;
	CompartmentType[] compartmentTypes;
	Vehicle vehicle;

	public RoutingDataBag(Node[] nodes, CompartmentType[] compartmentTypes, Vehicle vehicle) {
		this.nodes = nodes;
		this.compartmentTypes = compartmentTypes;
		this.vehicle = vehicle;
	}

}
