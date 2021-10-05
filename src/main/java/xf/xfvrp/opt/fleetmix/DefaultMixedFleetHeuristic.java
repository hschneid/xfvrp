package xf.xfvrp.opt.fleetmix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xf.xfvrp.RoutingDataBag;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.VehiclePriorityInitialiser;
import xf.xfvrp.opt.XFVRPSolution;
import xf.xfvrp.report.RouteReport;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Mixed fleet heuristic
 *
 * Choose biggest vehicle type and optimize with no
 * fleet size limitation. Afterwards the k best
 * routes are chosen. The customers on the trashed
 * routes are the base for the next run with next
 * vehicle type.
 *
 * @author hschneid
 *
 */
public class DefaultMixedFleetHeuristic extends MixedFleetHeuristicBase implements IMixedFleetHeuristic {
	
	@Override
	public List<XFVRPSolution> execute(
			Node[] nodes,
			Vehicle[] vehicles,
			RoutePlanningFunction routePlanningFunction,
			Metric metric,
			XFVRPParameter parameter,
			StatusManager statusManager) throws XFVRPException {
		List<Node> unplannedNodes = Arrays.asList(nodes);

		vehicles = VehiclePriorityInitialiser.execute(vehicles);

		List<XFVRPSolution> vehicleSolutions = new ArrayList<>();
		for (Vehicle veh : vehicles) {
			statusManager.fireMessage(StatusCode.RUNNING, "Run with vehicle "+veh.name+" started.");

			// Optimize all nodes with current vehicle type
			XFVRPSolution solution = routePlanningFunction.apply(new RoutingDataBag(unplannedNodes.toArray(new Node[0]), veh));

			// Point out best routes for this vehicle type
			List<RouteReport> bestRoutes = getSelector().getBestRoutes(veh, getReportBuilder().getReport(solution));

			if(bestRoutes.size() > 0) {
				// Add selected routes to overall best solution
				vehicleSolutions.add(reconstructGiantRoute(bestRoutes, solution.getModel()));

				// Remove customers from best routes for next planning stage
				unplannedNodes = getUnusedNodes(bestRoutes, unplannedNodes);
			}
		}

		// Insert invalid and unplanned nodes into solution
		XFVRPSolution unplannedNodesSolution = insertUnplannedNodes(unplannedNodes, metric, parameter, statusManager);
		if(unplannedNodesSolution != null)
			vehicleSolutions.add(unplannedNodesSolution);

		return vehicleSolutions;
	}
	
}
