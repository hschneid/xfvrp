package xf.xfvrp.opt.fleetmix;

import xf.xfvrp.RoutingDataBag;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.compartment.CompartmentType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.VehiclePriorityInitialiser;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.report.RouteReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * Mixed fleet heuristic
 * <p>
 * Choose biggest vehicle type and optimize with no
 * fleet size limitation. Afterwards the k best
 * routes are chosen. The customers on the trashed
 * routes are the base for the next run with next
 * vehicle type.
 *
 * @author hschneid
 */
public class DefaultMixedFleetHeuristic extends MixedFleetHeuristicBase implements IMixedFleetHeuristic {

    private final MixedFleetSelector selector = new MixedFleetSelector();

    @Override
    public List<Solution> execute(
            Node[] nodes,
            CompartmentType[] compartmentTypes,
            Vehicle[] vehicles,
            RoutePlanningFunction routePlanningFunction,
            Metric metric,
            XFVRPParameter parameter,
            StatusManager statusManager) throws XFVRPException {
        List<Node> unplannedNodes = Arrays.asList(nodes);

        vehicles = VehiclePriorityInitialiser.execute(vehicles);

        List<Solution> vehicleSolutions = new ArrayList<>();
        for (Vehicle veh : vehicles) {
            statusManager.fireMessage(StatusCode.RUNNING, "Run with vehicle " + veh.getName() + " started.");

            // Optimize all nodes with current vehicle type
            Solution solution = routePlanningFunction.apply(new RoutingDataBag(unplannedNodes.toArray(new Node[0]), compartmentTypes, veh));

            // Point out best routes for this vehicle type
            List<RouteReport> bestRoutes = getSelector().getBestRoutes(veh, getReportBuilder().getReport(solution));

            if (bestRoutes.size() > 0) {
                // Add selected routes to overall best solution
                vehicleSolutions.add(reconstructSolution(bestRoutes, solution.getModel()));

                // Remove customers from best routes for next planning stage
                unplannedNodes = getUnusedNodes(bestRoutes, unplannedNodes);
            }

            // If no customers are left, stop!
            if (getCustomers(unplannedNodes).size() == 0)
                break;
        }

        // Insert invalid and unplanned nodes into solution
        Solution unplannedNodesSolution = insertUnplannedNodes(unplannedNodes, compartmentTypes, metric, parameter, statusManager);
        if (unplannedNodesSolution != null)
            vehicleSolutions.add(unplannedNodesSolution);

        return vehicleSolutions;
    }

    @Override
    public MixedFleetSelector getSelector() {
        return selector;
    }
}
