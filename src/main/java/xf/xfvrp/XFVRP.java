package xf.xfvrp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.collection.ListMap;
import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.VehiclePriorityInitialiser;
import xf.xfvrp.base.xfvrp.XFVRP_Parameter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.XFVRPOptSplitter;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.opt.XFVRPSolution;
import xf.xfvrp.opt.init.ModelBuilder;
import xf.xfvrp.opt.init.precheck.PreCheckException;
import xf.xfvrp.opt.init.precheck.PreCheckService;
import xf.xfvrp.opt.init.solution.InitialSolutionBuilder;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;
import xf.xfvrp.report.RouteReportSummary;
import xf.xfvrp.report.build.ReportBuilder;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * XFVRP is central user interface for this suite. 
 * It combines all methods for data import, optimization execution, parameters and
 * retrieval of solutions.
 * 
 * The modeling of this class represents a state machine, where iteratively several 
 * methods must be called. The execution method take all inserted data and parameters
 * and start the optimizers.
 * 
 * 
 * @author hschneid
 * 
 */
public class XFVRP extends XFVRP_Parameter {

	private static Logger logger = LoggerFactory.getLogger(XFVRP.class);
	
	/* List of optimization procedures */
	private List<XFVRPOptBase> optList = new ArrayList<>();

	/* Last model for the last created solution */
	private XFVRPModel lastModel;

	/* Solutions - List of generated solutions per each vehicle type*/
	private final List<XFVRPSolution> vehicleSolutionList = new ArrayList<>();

	/**
	 * Calculates the VRP with the before inserted data
	 * by addDepot(), addCustomer(), addMetric() and 
	 * addVehicle() or the parameters setCapacity() and setMaxRouteDuration()
	 * @throws PreCheckException 
	 */
	public void executeRoutePlanning() throws PreCheckException {
		logger.info("XFVRP started");

		statusManager.fireMessage(StatusCode.RUNNING, "XFVRP started");
		statusManager.setStartTime();

		// Flush import buffer
		importer.finishImport();
		vehicleSolutionList.clear();

		// Copy imported data to internal data structure
		Vehicle[] vehicles = importer.getVehicles();
		Node[] nodes = importer.getNodes(vehicles, statusManager);

		// Check of input parameter
		{
			if(importer.getDepotList().size() == 0) {
				statusManager.fireMessage(StatusCode.ABORT, "No depot is given.");
				throw new IllegalArgumentException("No depot is given.");
			}
			if(vehicles.length == 0) {
				statusManager.fireMessage(StatusCode.ABORT, "No vehicle information were set.");
				throw new IllegalArgumentException("No vehicle information were set.");
			}
		}

		boolean[] plannedCustomers = new boolean[nodes.length];

		// Mixed fleet heuristic
		// Choose biggest vehicle type and optimize with no
		// fleet size limitation. Afterwards the k best
		// routes are chosen. The customers on the trashed 
		// routes are the base for the next run with next
		// vehicle type.
		vehicles = VehiclePriorityInitialiser.execute(vehicles);
		for (Vehicle veh : vehicles) {
			statusManager.fireMessage(StatusCode.RUNNING, "Run with vehicle "+veh.name+" started.");

			// Optimize all nodes with current vehicle type
			XFVRPSolution solution = executeRoutePlanning(nodes, veh, plannedCustomers);

			// Point out best routes for this vehicle type
			List<RouteReport> bestRoutes = getBestRoutes(veh, new ReportBuilder().getReport(solution));

			if(bestRoutes.size() > 0) {
				// Add selected routes to overall best solution
				vehicleSolutionList.add(new XFVRPSolution(reconstructGiantRoute(bestRoutes, lastModel), lastModel));

				// Remove customers from best routes for next planning stage
				removeUsedCustomers(bestRoutes, nodes, plannedCustomers);
			}
		}

		// Insert invalid and unplanned nodes into solution 
		// (customerList = unplannedList)
		insertUnplannedNodes(nodes, plannedCustomers, vehicles);

		statusManager.fireMessage(StatusCode.FINISHED, "XFVRP finished sucessfully.");
	}

	/**
	 * Calculates a single vehicle VRP for a given vehicle with all
	 * announced optimization procedures.
	 * 
	 * @param depotList
	 * @param customerList 
	 * @param veh Container with parameters for capacity and route duration
	 * @param plannedCustomers Marker for customers which are planned already in other stages
	 * @throws PreCheckException 
	 */
	private XFVRPSolution executeRoutePlanning(Node[] globalNodes, Vehicle veh, boolean[] plannedCustomers) throws PreCheckException {
		Node[] nodes = new PreCheckService().precheck(globalNodes, veh, plannedCustomers, parameter);
		XFVRPModel model = new ModelBuilder().build(nodes, veh, metric, parameter);
		Solution route = new InitialSolutionBuilder().build(model, parameter);

		// VRP optimizations, if initiated route has appropriate length
		if(route.getGiantRoute().length > 0) {
			/*
			 * For each given optimization procedure the current
			 * route plan is searched for optimizations. If route
			 * splitting is allowed, big route plans with a big 
			 * number of routes is splitted into smaller route plans.
			 * This is a speed up. 
			 */
			for (XFVRPOptBase xfvrp : optList) {				
				statusManager.fireMessage(StatusCode.RUNNING, "Optimiziation for algorithm "+xfvrp.getClass().getSimpleName() + " started.");

				try {
					if(parameter.isRouteSplittingAllowed() && xfvrp.isSplittable)
						route = new XFVRPOptSplitter().execute(route, model, statusManager, xfvrp);
					else
						route = xfvrp.execute(route, model, statusManager);
				} catch(UnsupportedOperationException usoex) {
					statusManager.fireMessage(StatusCode.EXCEPTION, "Splitting encountert problem:\n"+usoex.getMessage());
				}
			}

			// Normalization of last result
			route = NormalizeSolutionService.normalizeRoute(route, model);
		}
		
		lastModel = model;
		return new XFVRPSolution(route, model);
	}

	

	/**
	 * This method searches the best k routes in a given solution
	 * for a given vehicle. k is the number of available vehicles. The
	 * objective for best route is the cost per amount.
	 * 
	 * @param veh Container with given parameters
	 * @param rep Solution as report object
	 * @return List of best routes in solution report
	 */
	private List<RouteReport> getBestRoutes(Vehicle veh, Report rep) {
		// Get the quality of routes by the report informations
		List<Object[]> sortList = new ArrayList<>();
		for (RouteReport tRep : rep.getRoutes()) {
			RouteReportSummary summary = tRep.getSummary();
			float time = summary.getDuration();
			float amount = summary.getPickup() + summary.getDelivery();
			float quality = (veh.fixCost + (veh.varCost * time)) / amount;
			if(summary.getDelay() > 0)
				quality = Float.MAX_VALUE;
			sortList.add(new Object[]{new Float(quality), tRep});
		}

		// Sort the routes by their quality
		sortList.sort((o1, o2) -> {
			float v1 = ((Float)o1[0]).floatValue();
			float v2 = ((Float)o2[0]).floatValue();
			if(v1 > v2) return 1;
			if(v1 < v2) return -1;
			return 0;
		});

		// Reduce routes to the n best routes
		List<RouteReport> list = sortList.stream()
				.map(val -> (RouteReport) val[1])
				.limit(veh.nbrOfAvailableVehicles)
				.collect(Collectors.toList());

		return list;
	}

	/**
	 * This method constructs a giant tour for a given list of route reports
	 * (can be found in solution objects).
	 * 
	 * @param routes Tour reports from getReport()
	 * @param model
	 */
	private Solution reconstructGiantRoute(List<RouteReport> routes, XFVRPModel model) {
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
		return solution;
	}

	/**
	 * This method removes customers which are found in the given solution
	 * from the nodeList object, which is managed by executeRoutePlanning() method.
	 * 
	 * @param routes
	 * @param customerList 
	 */
	private void removeUsedCustomers(List<RouteReport> routes, Node[] nodes, boolean[] plannedCustomers) {
		Map<String, Integer> nodeIdxMap = new HashMap<>();
		Arrays.stream(nodes).forEach(n -> nodeIdxMap.put(n.getExternID(), n.getGlobalIdx()));

		for (RouteReport tRep : routes) {
			for (Event e : tRep.getEvents()) {
				String locID = e.getID();

				// Accept only customer nodes (loadType = PICKUP and DELIVERY)
				if(SiteType.CUSTOMER.equals(e.getSiteType())) {
					if(!nodeIdxMap.containsKey(locID)) {
						// If customer on route is not in route list, throw error 
						statusManager.fireMessage(StatusCode.ABORT, locID+" not found!");
						throw new IllegalStateException(locID+" not found!");
					}

					plannedCustomers[nodeIdxMap.get(locID)] = true;
				}
			}
		}
	}

	/**
	 * 
	 * @param globalNodes
	 * @param plannedCustomers
	 * @param vehicles
	 */
	private void insertUnplannedNodes(
			Node[] globalNodes,
			boolean[] plannedCustomers,
			Vehicle[] vehicles) {

		// Get unplanned or invalid nodes
		List<Node> unplanned = Arrays.stream(globalNodes)
				.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
				.filter(n -> plannedCustomers[n.getGlobalIdx()] == false)
				.collect(Collectors.toList());

		if(unplanned.size() > 0) {
			// Set unplanned reason, for valid nodes
			unplanned.stream()
				.filter(n -> n.getInvalidReason() == InvalidReason.NONE)
				.forEach(n -> n.setInvalidReason(InvalidReason.UNPLANNED));

			// Build new node array only with unplanned customers
			List<Node> nodeList = Arrays.stream(globalNodes)
				.filter(n -> n.getSiteType() == SiteType.DEPOT || n.getSiteType() == SiteType.REPLENISH)
				.collect(Collectors.toList());
			nodeList.addAll(unplanned);
			
			// Set local index
			IntStream.range(0, nodeList.size()).forEach(i -> nodeList.get(i).setIdx(i));

			Node[] nodes = nodeList.toArray(new Node[0]);

			Solution giantRoute = buildGiantRouteForInvalidNodes(unplanned, nodes[0]);

			// Create solution with single routes for each invalid node
			InternalMetric internalMetric = AcceleratedMetricTransformator.transform(metric, nodes, vehicles[0]);
			vehicleSolutionList.add(
					new XFVRPSolution(
							giantRoute,
							new XFVRPModel(
									nodes,
									internalMetric,
									internalMetric, 
									importer.invalidVehicle.createVehicle(-1),
									parameter
									)
							)
					);
		}

		statusManager.fireMessage(StatusCode.RUNNING, "Invalid or unplanned nodes are inserted in result. (nbr of invalid nodes = " + unplanned.size()+")");
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
	private Solution buildGiantRouteForInvalidNodes(List<Node> unplannedNodes, Node depot) {
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

	/**
	 * Uses the last planned solution and turns it
	 * into a report representation.
	 * 
	 * All route plan informations can be akquired by this report.
	 * 
	 * @return A report data structure with detailed information about the route plan or null if no solution was calculated.
	 */
	public Report getReport() {
		if(vehicleSolutionList.size() > 0) {

			Report rep = new Report(lastModel);
			for (XFVRPSolution sol : vehicleSolutionList)
				rep.importReport(new ReportBuilder().getReport(sol));

			return rep;
		}
		return null;
	}
	
	/**
	 * Adds a certain optimization algorithm out
	 * of the spectrum of accessible methods in
	 * the enumeration XFVRPOptType.
	 * 
	 * @param type algorithm type. it can't be null.
	 */
	public void addOptType(XFVRPOptType type) {
		if(type != null)
			optList.add(type.createInstance());
	}

	/**
	 * Clears all added optimization algorithms
	 */
	public void clearOptTypes() {
		optList.clear();
	}
}
