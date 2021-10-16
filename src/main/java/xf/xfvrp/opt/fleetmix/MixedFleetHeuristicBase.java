package xf.xfvrp.opt.fleetmix;

import util.collection.ListMap;
import xf.xfvrp.base.*;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.fleximport.InvalidVehicle;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPSolution;
import xf.xfvrp.opt.fleetmix.IMixedFleetHeuristic.RoutePlanningFunction;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.RouteReport;
import xf.xfvrp.report.build.ReportBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class MixedFleetHeuristicBase {
	private final ReportBuilder reportBuilder = new ReportBuilder();

	public abstract IMixedFleetSelector getSelector();

	public abstract List<XFVRPSolution> execute(
			Node[] nodes,
			Vehicle[] vehicles,
			RoutePlanningFunction routePlanningFunction,
			Metric metric,
			XFVRPParameter parameter,
			StatusManager statusManager) throws XFVRPException;

	/**
	 * This method removes customers which are found in the given solution
	 * from the nodeList object, which is managed by executeRoutePlanning() method.
	 */
	protected List<Node> getUnusedNodes(List<RouteReport> routes, List<Node> unplannedNodes) {
		Map<String, Node> nodeIdxMap = unplannedNodes.stream().collect(Collectors.toMap(Node::getExternID, node -> node, (v1, v2) -> v1));

		List<Node> usedNodes = routes.stream()
				.flatMap(route -> route.getEvents().stream())
				.filter(event -> event.getSiteType().equals(SiteType.CUSTOMER))
				.map(Event::getID)
				.filter(nodeIdxMap::containsKey)
				.map(nodeIdxMap::get)
				.collect(Collectors.toList());

		List<Node> unusedNodes = new ArrayList<>(unplannedNodes);
		unusedNodes.removeAll(usedNodes);
		return unusedNodes;
	}

	/**
	 * This method constructs a giant tour for a given list of route reports
	 * (can be found in solution objects).
	 */
	protected XFVRPSolution reconstructGiantRoute(List<RouteReport> routes, XFVRPModel model) {
		Map<String, Node> nodeMap = Arrays.stream(model.getNodes()).collect(Collectors.toMap(Node::getExternID, node -> node, (v1, v2) -> v1));

		AtomicInteger depotId = new AtomicInteger(0);
		Node[] giantRoute = routes.stream()
				.map(RouteReport::getEvents)
				.filter(events -> events.size() > 2)
				.flatMap(events -> events.stream().sequential())
				// Other nodes (PAUSE is no structural node type. It is only inserted for evaluation issues in reports.)
				.filter(event -> event.getLoadType() != LoadType.PAUSE)
				.map(event -> nodeMap.get(event.getID()))
				.map(node -> {
					if (node.getSiteType() == SiteType.DEPOT)
						return Util.createIdNode(node, depotId.getAndIncrement());
					return node;
				})
				.toArray(Node[]::new);

		return new XFVRPSolution(getSolution(giantRoute), model);
	}

	protected XFVRPSolution insertUnplannedNodes(
			List<Node> unplannedNodes,
			Metric metric,
			XFVRPParameter parameter,
			StatusManager statusManager) throws XFVRPException {
		// Get unplanned or invalid customers
		List<Node> unplannedCustomers = getCustomers(unplannedNodes);
		if (unplannedCustomers.size() == 0) {
			return null;
		}

		statusManager.fireMessage(StatusCode.RUNNING, String.format("Invalid or unplanned nodes are inserted in result. (nbr of invalid nodes = %d)", unplannedCustomers.size()));

		// Set unplanned reason, for valid nodes
		unplannedCustomers.stream()
				.filter(n -> n.getInvalidReason() == InvalidReason.NONE)
				.forEach(n -> n.setInvalidReason(InvalidReason.UNPLANNED));

		// Set local index
		Node[] nodes = unplannedNodes.toArray(new Node[0]);
		IntStream.range(0, nodes.length).forEach(i -> nodes[i].setIdx(i));

		Solution giantRoute = buildGiantRouteForInvalidNodes(unplannedCustomers, nodes[0], statusManager);

		Vehicle invalidVehicle = InvalidVehicle.createInvalid(unplannedCustomers.get(0).getDemand().length);

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
	 */
	public Solution buildGiantRouteForInvalidNodes(List<Node> unplannedNodes, Node depot, StatusManager statusManager) {
		if (unplannedNodes.size() == 0)
			return getSolution(null);

		Node[] giantRoute = new Node[unplannedNodes.size() * 2 + 2];

		// Cluster blocked nodes
		List<Node> unplannedSingles = new ArrayList<>();
		ListMap<Integer, Node> unplannedBlocks = ListMap.create();
		unplannedNodes.forEach(node -> {
			if (node.getPresetBlockIdx() != BlockNameConverter.DEFAULT_BLOCK_IDX)
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
			block.sort(Comparator.comparingInt(Node::getPresetBlockPos));

			// Add nodes of block in preset sequence ordering
			for (Node node : block) {
				statusManager.fireMessage(StatusCode.EXCEPTION, "Warning: Invalid node " + node.toString() + " Reason: " + node.getInvalidReason());
				giantRoute[i++] = node;
			}
			giantRoute[i++] = Util.createIdNode(depot, maxDepotId++);
		}

		return getSolution(Arrays.copyOf(giantRoute, i));
	}

	protected List<Node> getCustomers(List<Node> allNodes) {
		return allNodes.stream()
				.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
				.collect(Collectors.toList());
	}

	public ReportBuilder getReportBuilder() {
		return reportBuilder;
	}

	private Solution getSolution(Node[] giantRoute) {
		if (giantRoute == null)
			giantRoute = new Node[0];

		Solution solution = new Solution();
		solution.setGiantRoute(giantRoute);
		return solution;
	}
}
