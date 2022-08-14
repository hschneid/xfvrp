package xf.xfvrp.opt.fleetmix;

import util.collection.ListMap;
import xf.xfvrp.base.*;
import xf.xfvrp.base.compartment.CompartmentType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.fleximport.InvalidVehicle;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.metric.Metric;
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.fleetmix.IMixedFleetHeuristic.RoutePlanningFunction;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.RouteReport;
import xf.xfvrp.report.build.ReportBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @author hschneid
 *
 */
public abstract class MixedFleetHeuristicBase {

	private final ReportBuilder reportBuilder = new ReportBuilder();

	public abstract IMixedFleetSelector getSelector();

	public abstract List<Solution> execute(
			Node[] nodes,
			CompartmentType[] compartmentTypes,
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
	 * This method transforms an optimization result (report) into an input for new optimization
	 */
	protected Solution reconstructSolution(List<RouteReport> routes, XFVRPModel model) {
		Map<String, Node> nodeMap = Arrays.stream(model.getNodes()).collect(Collectors.toMap(Node::getExternID, node -> node, (v1, v2) -> v1));

		// Remove empty routes
		routes = routes.stream()
				.filter(route -> route.getEvents().size() > 2)
				.collect(Collectors.toList());

		Node[][] newRoutes = new Node[routes.size()][];
		AtomicInteger depotId = new AtomicInteger(0);
		for (int i = 0, routesSize = routes.size(); i < routesSize; i++) {
			RouteReport route = routes.get(i);
			newRoutes[i] = route.getEvents()
					.stream()
					// Other nodes (PAUSE is no structural node type. It is only inserted for evaluation issues in reports.)
					.filter(event -> event.getLoadType() != LoadType.PAUSE)
					.map(event -> nodeMap.get(event.getID()))
					.map(node -> {
						if (node.getSiteType() == SiteType.DEPOT)
							return Util.createIdNode(node, depotId.getAndIncrement());
						return node;
					})
					.toArray(Node[]::new);
		}

		return getSolution(newRoutes, model);
	}

	protected Solution insertUnplannedNodes(
			List<Node> unplannedNodes,
			CompartmentType[] compartmentTypes,
			Metric metric,
			XFVRPParameter parameter,
			StatusManager statusManager) throws XFVRPException {

		// Get unplanned or invalid nodes
		List<Node> unplannedCustomers = unplannedNodes.stream()
				.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
				.collect(Collectors.toList());

		if(unplannedCustomers.size() == 0) {
			statusManager.fireMessage(StatusCode.RUNNING, "Invalid or unplanned nodes are inserted in result. (nbr of invalid nodes = 0)");
			return null;
		}

		// Set unplanned reason, for valid nodes
		unplannedCustomers.stream()
				.filter(n -> n.getInvalidReason() == InvalidReason.NONE)
				.forEach(n -> n.setInvalidReason(InvalidReason.UNPLANNED));

		// Create solution with single routes for each invalid node
		Node[] nodes = unplannedNodes.toArray(new Node[0]);
		IntStream.range(0, nodes.length).forEach(i -> nodes[i].setIdx(i));
		Vehicle invalidVehicle = InvalidVehicle.createInvalid(unplannedCustomers.get(0).getDemand().length);
		InternalMetric internalMetric = AcceleratedMetricTransformator.transform(metric, nodes, invalidVehicle);
		XFVRPModel model = new XFVRPModel(
				nodes,
				compartmentTypes,
				internalMetric,
				internalMetric,
				invalidVehicle,
				parameter
		);

		return buildSolutionForInvalidNodes(unplannedCustomers, nodes[0], model, statusManager);
	}

	/**
	 * Invalid customers are excluded from optimization. Afterwards they
	 * have to be included to the result. This method builds a solution with
	 * the excluded customer nodes.
	 * For a given set of invalid nodes, a new route is created, where each invalid
	 * node is on a single route.
	 */
	protected Solution buildSolutionForInvalidNodes(List<Node> unplannedNodes, Node depot, XFVRPModel model, StatusManager statusManager) {
		if(unplannedNodes.size() == 0)
			return getSolution(null, null);

		// Cluster blocked nodes
		List<Node> unplannedSingles = new ArrayList<>();
		ListMap<Integer, Node> unplannedBlocks = ListMap.create();
		unplannedNodes.forEach(node -> {
			if(node.getPresetBlockIdx() != BlockNameConverter.DEFAULT_BLOCK_IDX)
				unplannedBlocks.put(node.getPresetBlockIdx(), node);
			else
				unplannedSingles.add(node);
		});

		int maxDepotId = 0;
		List<Node[]> invalidRoutes = new ArrayList<>();

		// Add invalid nodes with no block preset on single routes
		for (Node node : unplannedSingles) {
			statusManager.fireMessage(StatusCode.EXCEPTION, "Warning: Invalid node " + node.toString() + " Reason: " + node.getInvalidReason());
			Node[] route = new Node[3];
			route[0] = Util.createIdNode(depot, maxDepotId++);
			route[1] = node;
			route[2] = route[0];
			invalidRoutes.add(route);
		}

		// Add invalid nodes with block preset
		for (List<Node> block : unplannedBlocks.values()) {
			// Right order of preset sequence positions
			block.sort(Comparator.comparingInt(Node::getPresetBlockPos));

			Node[] route = new Node[block.size() + 2];

			// Add nodes of block in preset sequence ordering
			route[0] = Util.createIdNode(depot, maxDepotId++);
			for (int k = 0, blockSize = block.size(); k < blockSize; k++) {
				Node node = block.get(k);
				statusManager.fireMessage(StatusCode.EXCEPTION, "Warning: Invalid node " + node.toString() + " Reason: " + node.getInvalidReason());
				route[k + 1] = node;
			}
			route[route.length - 1] = route[0];

			invalidRoutes.add(route);
		}

		return getSolution(invalidRoutes.toArray(new Node[0][]), model);
	}

	protected List<Node> getCustomers(List<Node> allNodes) {
		return allNodes.stream()
				.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
				.collect(Collectors.toList());
	}

	public ReportBuilder getReportBuilder() {
		return reportBuilder;
	}

	private Solution getSolution(Node[][] routes, XFVRPModel model) {
		if(routes == null)
			routes = new Node[0][0];

		Solution solution = new Solution(model);
		for (Node[] route : routes) {
			solution.addRoute(route);
		}
		return solution;
	}

}
