package xf.xfvrp.opt.fleetmix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import xf.xfvrp.RoutingDataBag;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.fleximport.CompartmentCapacity;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.VehiclePriorityInitialiser;
import xf.xfvrp.opt.XFVRPSolution;
import xf.xfvrp.report.RouteReport;

public class InTurnMixedFleetHeuristic extends MixedFleetHeuristicBase implements IMixedFleetHeuristic{
	List<XFVRPSolution> vehicleSolutions;
	String fallbackVehicleName;
	Vehicle fallbackVehicle;
	
	@Override
	public List<XFVRPSolution> execute(Node[] nodes, Vehicle[] vehicles, RoutePlanningFunction routePlanningFunction,
									   Metric metric, XFVRPParameter parameter, StatusManager statusManager)
	throws XFVRPException {
		List<Node> unplannedNodes = Arrays.asList(nodes);
		
		if (fallbackVehicleName != null) {
			fallbackVehicle = Arrays.stream(vehicles).filter(v -> Objects.equals(v.name, fallbackVehicleName)).findFirst().get();
			vehicles = Arrays.stream(vehicles).filter(v -> !Objects.equals(v.name, fallbackVehicleName)).toArray(Vehicle[]::new);
		}
		vehicles = VehiclePriorityInitialiser.execute(vehicles);
		
		vehicleSolutions = new ArrayList<>();
		
		int[] vehCounts = Arrays.stream(vehicles).mapToInt(v -> v.nbrOfAvailableVehicles).toArray();
		int i = 0;
		while (countCustomersLeft(unplannedNodes) > 0 && countVehiclesLeft(vehCounts) > 0) {
			if (vehCounts[i] == 0) continue;
			
			Vehicle veh = vehicles[i];
			statusManager.fireMessage(StatusCode.RUNNING, "Run with vehicle " + veh.name + " started.");
			
			List<CompartmentCapacity> capacityPerCompartment = Arrays.asList(
					new CompartmentCapacity(),
					new CompartmentCapacity(),
					new CompartmentCapacity());
			Vehicle instance = new Vehicle(veh.idx, veh.name, 1, capacityPerCompartment,
					veh.maxRouteDuration, veh.maxStopCount, veh.maxWaitingTime, veh.fixCost, veh.varCost,
					veh.vehicleMetricId, veh.maxDrivingTimePerShift, veh.waitingTimeBetweenShifts, veh.priority);
			instance.setCapacity(veh.capacity.clone());
			
			unplannedNodes = route(routePlanningFunction, unplannedNodes, instance);
			
			vehCounts[i] = vehCounts[i] - 1;
			i = (i+1) % vehicles.length;
		}
		
		if (countCustomersLeft(unplannedNodes) > 0 && fallbackVehicleName != null) {
			statusManager.fireMessage(StatusCode.RUNNING, "No vehicle left and not every customer assigned -> using fallback vehicle");
			unplannedNodes = route(routePlanningFunction, unplannedNodes, fallbackVehicle);
		}
		
		// Insert invalid and unplanned nodes into solution
		XFVRPSolution unplannedNodesSolution = insertUnplannedNodes(unplannedNodes, metric, parameter, statusManager);
		if(unplannedNodesSolution != null)
			vehicleSolutions.add(unplannedNodesSolution);
		
		return vehicleSolutions;
	}
	
	private List<Node> route(RoutePlanningFunction routePlanningFunction, List<Node> unplannedNodes, Vehicle instance) {
		// Optimize all nodes with current vehicle type
		XFVRPSolution solution = routePlanningFunction.apply(new RoutingDataBag(unplannedNodes.toArray(new Node[0]), instance));
		
		// Point out best routes for this vehicle type
		List<RouteReport> bestRoutes = getSelector().getBestRoutes(instance, getReportBuilder().getReport(solution));
		
		if (bestRoutes.size() > 0) {
			// Add selected routes to overall best solution
			vehicleSolutions.add(reconstructGiantRoute(bestRoutes, solution.getModel()));
			
			// Remove customers from best routes for next planning stage
			unplannedNodes = getUnusedNodes(bestRoutes, unplannedNodes);
		}
		return unplannedNodes;
	}
	
	public void setFallbackVehicleName(String vehicleName) {
		this.fallbackVehicleName = vehicleName;
	}
	
	private int countVehiclesLeft(int[] vehCounts) {
		return Arrays.stream(vehCounts).sum();
	}
	
	private long countCustomersLeft(List<Node> unplannedNodes) {
		return unplannedNodes.stream().filter(f -> f.getSiteType().equals(SiteType.CUSTOMER)).count();
	}
}
