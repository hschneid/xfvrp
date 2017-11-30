package xf.xfvrp.opt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import util.collection.ListMap;
import xf.xfvrp.RoutingDataBag;
import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.VehiclePriorityInitialiser;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.RouteReport;
import xf.xfvrp.report.build.ReportBuilder;

/**
 *   Mixed fleet heuristic
 *   Choose biggest vehicle type and optimize with no
 *   fleet size limitation. Afterwards the k best
 *   routes are chosen. The customers on the trashed
 *   routes are the base for the next run with next
 *   vehicle type.
 *   
 * @author hschneid
 *
 */
public class FullRouteMixedFleetHeuristic {

	private final FullRouteMixedFleetSelector selector = new FullRouteMixedFleetSelector();

	public List<XFVRPSolution> execute(
			Node[] nodes,
			Vehicle[] vehicles,
			Function<RoutingDataBag, XFVRPSolution> routePlanningFunction,
			Metric metric,
			XFVRPParameter parameter,
			Vehicle invalidVehicle,
			StatusManager statusManager) {
		List<Node> unplannedNodes = Arrays.asList(nodes);

		vehicles = VehiclePriorityInitialiser.execute(vehicles);

		List<XFVRPSolution> vehicleSolutions = new ArrayList<>();
		for (Vehicle veh : vehicles) {
			statusManager.fireMessage(StatusCode.RUNNING, "Run with vehicle "+veh.name+" started.");

			// Optimize all nodes with current vehicle type
			XFVRPSolution solution = routePlanningFunction.apply(new RoutingDataBag(unplannedNodes.toArray(new Node[0]), veh));

			// Point out best routes for this vehicle type
			List<RouteReport> bestRoutes = selector.getBestRoutes(veh, new ReportBuilder().getReport(solution));

			if(bestRoutes.size() > 0) {
				// Add selected routes to overall best solution
				vehicleSolutions.add(reconstructGiantRoute(bestRoutes, solution.getModel()));

				// Remove customers from best routes for next planning stage
				unplannedNodes = getUnusedNodes(bestRoutes, unplannedNodes);
			}
		}

		// Insert invalid and unplanned nodes into solution 
		vehicleSolutions.add(insertUnplannedNodes(unplannedNodes, vehicles, metric, parameter, invalidVehicle, statusManager));
		
		return vehicleSolutions;
	}

	/**
	 * This method removes customers which are found in the given solution
	 * from the nodeList object, which is managed by executeRoutePlanning() method.
	 * 
	 * @param routes
	 * @param customerList 
	 */
	private List<Node> getUnusedNodes(List<RouteReport> routes, List<Node> unplannedNodes) {	
		Map<String, Node> nodeIdxMap = unplannedNodes.stream().collect(Collectors.toMap(Node::getExternID, node -> node, (v1, v2) -> v1));

		List<Node> usedNodes = routes.stream()
				.flatMap(route -> route.getEvents().stream())
				.filter(event -> event.getSiteType().equals(SiteType.CUSTOMER))
				.map(event -> event.getID())
				.filter(id -> nodeIdxMap.containsKey(id))
				.map(id -> nodeIdxMap.get(id))
				.collect(Collectors.toList());

		List<Node> unusedNodes = new ArrayList<>(unplannedNodes);
		unusedNodes.removeAll(usedNodes);
		return unusedNodes;
	}

	/**
	 * This method constructs a giant tour for a given list of route reports
	 * (can be found in solution objects).
	 * 
	 * @param routes Tour reports from getReport()
	 * @param model
	 */
	private XFVRPSolution reconstructGiantRoute(List<RouteReport> routes, XFVRPModel model) {
		Map<String, Node> nodeMap = new HashMap<>();
		Arrays.stream(model.getNodes()).forEach(n -> nodeMap.put(n.getExternID(), n));

		int depotId = 0;
		ArrayList<Node> al = new ArrayList<>();
		for (RouteReport r : routes) {
			List<Event> events = r.getEvents();
			for (int i = 0; i < events.size(); i++) {
				Event e = events.get(i);

				// Jump over Depots with empty routes
				if(e.getSiteType() == SiteType.DEPOT
						&& i + 1 < events.size()
						&& events.get(i + 1).getSiteType() == SiteType.DEPOT)
					continue;

				Node node = nodeMap.get(e.getID());

				// Depots
				if(e.getSiteType() == SiteType.DEPOT)
					al.add(Util.createIdNode(node, depotId++));
				// Other nodes (PAUSE is no structural node type. It is only inserted for evaluation issues in reports.)
				else if(e.getLoadType() != LoadType.PAUSE)
					al.add(node);
			}
		}

		Solution solution = new Solution();
		solution.setGiantRoute(al.stream().toArray(Node[]::new));

		return new XFVRPSolution(solution, model);
	}

	private XFVRPSolution insertUnplannedNodes(
			List<Node> unplannedNodes,
			Vehicle[] vehicles,
			Metric metric, 
			XFVRPParameter parameter,
			Vehicle invalidVehicle,
			StatusManager statusManager) {

		// Get unplanned or invalid nodes
		List<Node> unplannedCustomers = unplannedNodes.stream()
				.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
				.collect(Collectors.toList());

		if(unplannedCustomers.size() > 0) {
			// Set unplanned reason, for valid nodes
			unplannedCustomers.stream()
			.filter(n -> n.getInvalidReason() == InvalidReason.NONE)
			.forEach(n -> n.setInvalidReason(InvalidReason.UNPLANNED));

			// Build new node array only with unplanned customers
			List<Node> nodeList = unplannedNodes.stream()
					.filter(n -> n.getSiteType() == SiteType.DEPOT || n.getSiteType() == SiteType.REPLENISH)
					.collect(Collectors.toList());
			nodeList.addAll(unplannedCustomers);

			// Set local index
			IntStream.range(0, nodeList.size()).forEach(i -> nodeList.get(i).setIdx(i));

			Node[] nodes = nodeList.toArray(new Node[0]);

			Solution giantRoute = buildGiantRouteForInvalidNodes(unplannedCustomers, nodes[0], statusManager);

			// Create solution with single routes for each invalid node
			InternalMetric internalMetric = AcceleratedMetricTransformator.transform(metric, nodes, vehicles[0]);
			
			return new XFVRPSolution(
							giantRoute,
							new XFVRPModel(
									nodes,
									internalMetric,
									internalMetric, 
									invalidVehicle,
									parameter
									)
							);
		}
		
		statusManager.fireMessage(StatusCode.RUNNING, "Invalid or unplanned nodes are inserted in result. (nbr of invalid nodes = " + unplannedCustomers.size()+")");
		
		return null;
	}

	/**
	 * Invalid customers are excluded from optimization. Afterwards they
	 * have to be included to the result. This method builds a giant route with
	 * the excluded customer nodes. 
	 * For a given set of invalid nodes, a giant tour is created, where each invalid
	 * node is on a single route.
	 * 
	 * @param unplannedNodes Nodes which can not be processed in optimization
	 * @param depotList List of depot nodes. Only first depot node is used for invalid nodes.
	 * @return Giant route with single routes for each invalid node.
	 */
	private Solution buildGiantRouteForInvalidNodes(List<Node> unplannedNodes, Node depot, StatusManager statusManager) {
		Node[] giantRoute = new Node[unplannedNodes.size() * 2 + 2];

		// Cluster blocked nodes
		List<Node> unplannedSingles = new ArrayList<>();
		ListMap<Integer, Node> unplannedBlocks = ListMap.create();
		unplannedNodes.forEach(node -> {
			if(node.getPresetBlockIdx() != BlockNameConverter.DEFAULT_BLOCK_IDX)
				unplannedBlocks.put(node.getPresetBlockIdx(), node);
			else
				unplannedSingles.add(node);
		});

		// A dummy depot is needed. So first depot is taken, which is obligatory.
		int maxDepotId = 0;
		giantRoute[0] = Util.createIdNode(depot, maxDepotId++);
		int i = 1;

		// Add invalid nodes with no block preset on single routes
		for (Node node : unplannedSingles) {
			statusManager.fireMessage(StatusCode.EXCEPTION, "Warning: Invalid node " + node.toString() + " Reason: " + node.getInvalidReason());
			giantRoute[i++] = node;
			giantRoute[i++] = Util.createIdNode(depot, maxDepotId++);
		}

		// Add invalid nodes with block preset
		for (List<Node> block : unplannedBlocks.values()) {
			// Right order of preset sequence positions
			block.sort((o1, o2) -> o1.getPresetBlockPos() - o2.getPresetBlockPos());

			// Add nodes of block in preset sequence ordering
			for (Node node : block) {
				statusManager.fireMessage(StatusCode.EXCEPTION, "Warning: Invalid node " + node.toString() + " Reason: " + node.getInvalidReason());
				giantRoute[i++] = node;
			}
			giantRoute[i++] = Util.createIdNode(depot, maxDepotId++);
		}

		Solution solution = new Solution();
		solution.setGiantRoute(Arrays.copyOf(giantRoute, i));
		return solution;
	}

}
