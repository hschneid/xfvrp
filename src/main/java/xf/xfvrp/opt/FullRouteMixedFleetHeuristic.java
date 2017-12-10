package xf.xfvrp.opt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
import xf.xfvrp.base.fleximport.InternalVehicleData;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.VehiclePriorityInitialiser;
import xf.xfvrp.report.RouteReport;
import xf.xfvrp.report.build.ReportBuilder;

/**
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
public class FullRouteMixedFleetHeuristic {

	private FullRouteMixedFleetSelector selector = new FullRouteMixedFleetSelector();

	public List<XFVRPSolution> execute(
			Node[] nodes,
			Vehicle[] vehicles,
			Function<RoutingDataBag, XFVRPSolution> routePlanningFunction,
			Metric metric,
			XFVRPParameter parameter,
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
		vehicleSolutions.add(insertUnplannedNodes(unplannedNodes, metric, parameter, statusManager));

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
		Map<String, Node> nodeMap = Arrays.stream(model.getNodes()).collect(Collectors.toMap(Node::getExternID, node -> node, (v1, v2) -> v1));

		AtomicInteger depotId = new AtomicInteger(0);
		Node[] giantRoute = routes.stream()
				.map(route -> route.getEvents())
				.filter(events -> events.size() > 2)
				.flatMap(events -> events.stream().sequential())
				// Other nodes (PAUSE is no structural node type. It is only inserted for evaluation issues in reports.)
				.filter(event -> event.getLoadType() != LoadType.PAUSE)
				.map(event -> nodeMap.get(event.getID()))
				.map(node -> {
					if(node.getSiteType() == SiteType.DEPOT)
						return Util.createIdNode(node, depotId.getAndIncrement());
					return node;
				})
				.toArray(Node[]::new);

		return new XFVRPSolution(getSolution(giantRoute), model);
	}

	private XFVRPSolution insertUnplannedNodes(
			List<Node> unplannedNodes,
			Metric metric, 
			XFVRPParameter parameter,
			StatusManager statusManager) {

		// Get unplanned or invalid nodes
		List<Node> unplannedCustomers = unplannedNodes.stream()
				.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
				.collect(Collectors.toList());

		if(unplannedCustomers.size() == 0) {
			statusManager.fireMessage(StatusCode.RUNNING, "Invalid or unplanned nodes are inserted in result. (nbr of invalid nodes = " + unplannedCustomers.size()+")");
			return null;
		}

		// Set unplanned reason, for valid nodes
		unplannedCustomers.stream()
		.filter(n -> n.getInvalidReason() == InvalidReason.NONE)
		.forEach(n -> n.setInvalidReason(InvalidReason.UNPLANNED));

		// Set local index
		Node[] nodes = unplannedNodes.toArray(new Node[0]);
		IntStream.range(0, nodes.length).forEach(i -> nodes[i].setIdx(i));

		Solution giantRoute = buildGiantRouteForInvalidNodes(unplannedCustomers, nodes[0], statusManager);

		Vehicle invalidVehicle = InternalVehicleData.createInvalid().createVehicle(-1);

		// Create solution with single routes for each invalid node
		InternalMetric internalMetric = AcceleratedMetricTransformator.transform(metric, nodes, invalidVehicle);

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
		if(unplannedNodes.size() == 0)
			return getSolution(null);

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

		return getSolution(Arrays.copyOf(giantRoute, i));
	}

	private Solution getSolution(Node[] giantRoute) {
		if(giantRoute == null)
			giantRoute = new Node[0];

		Solution solution = new Solution();
		solution.setGiantRoute(giantRoute);
		return solution;		
	}
}
