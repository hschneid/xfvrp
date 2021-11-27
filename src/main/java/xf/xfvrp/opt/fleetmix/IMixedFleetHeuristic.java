package xf.xfvrp.opt.fleetmix;

import xf.xfvrp.RoutingDataBag;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.compartment.CompartmentType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.opt.Solution;

import java.util.List;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @author hschneid
 *
 */
public interface IMixedFleetHeuristic {

	interface RoutePlanningFunction {
		Solution apply(RoutingDataBag bag) throws XFVRPException;
	}
	
	List<Solution> execute(
			Node[] nodes,
			CompartmentType[] compartmentTypes,
			Vehicle[] vehicles,
			RoutePlanningFunction routePlanningFunction,
			Metric metric,
			XFVRPParameter parameter,
			StatusManager statusManager) throws XFVRPException;
	
}
