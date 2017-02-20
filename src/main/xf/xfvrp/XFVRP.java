package xf.xfvrp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import util.collection.ListMap;
import xf.xfpdp.XFPDPInit;
import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.fleximport.CustomerData;
import xf.xfvrp.base.fleximport.DepotData;
import xf.xfvrp.base.fleximport.InternalCustomerData;
import xf.xfvrp.base.fleximport.InternalDepotData;
import xf.xfvrp.base.fleximport.InternalReplenishData;
import xf.xfvrp.base.fleximport.ReplenishData;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.metric.MapMetric;
import xf.xfvrp.base.metric.internal.AcceleratedMetricTransformator;
import xf.xfvrp.base.metric.internal.FixCostMetricTransformator;
import xf.xfvrp.base.metric.internal.OpenRouteMetricTransformator;
import xf.xfvrp.base.metric.internal.PresetMetricTransformator;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.VehiclePriorityInitialiser;
import xf.xfvrp.base.xfvrp.XFVRP_Parameter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.XFVRPOptSplitter;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.opt.XFVRPSolution;
import xf.xfvrp.opt.init.XFInit;
import xf.xfvrp.report.Event;
import xf.xfvrp.report.Report;
import xf.xfvrp.report.RouteReport;
import xf.xfvrp.report.RouteReportSummary;

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
	 */
	public void executeRoutePlanning() {
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
			List<RouteReport> bestRoutes = getBestRoutes(veh, solution.getReport());

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
	 */
	private XFVRPSolution executeRoutePlanning(Node[] globalNodes, Vehicle veh, boolean[] plannedCustomers) {
		Node[] nodes = null;

		// Precheck
		if(parameter.isWithPDP())
			nodes = new XFPDPInit().precheck(globalNodes, plannedCustomers);
		else
			nodes = new XFInit().precheck(globalNodes, veh, plannedCustomers);

		// Init
		XFVRPModel model = init(nodes, veh);

		// Init giant route
		Solution route = null;

		if(parameter.isWithPDP())
			route = new XFPDPInit().buildInitPDP(model, new ArrayList<Node>());
		else
			route = new XFInit().initGiantRoute(model, statusManager, new ArrayList<Node>());

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

			// Normilization of last result
			route = Util.normalizeRoute(route, model);
		}
		
		// Fertige Tour
		lastModel = model;
		return new XFVRPSolution(route, model);
	}

	/**
	 * Transforms the read data into a model, which can be used
	 * for optimization.
	 * 
	 * @param nodes
	 * @param veh Contains parameters for capacity, max route duration and others.
	 * @return Returns a model, which can be used for optimization procedures.
	 * @throws IllegalArgumentException
	 */
	private XFVRPModel init(Node[] nodes, Vehicle veh) throws IllegalArgumentException {
		statusManager.fireMessage(StatusCode.RUNNING, "Initialisation of instance for vehicle "+veh.name);

		// Set local node index
		for (int i = 0; i < nodes.length; i++)
			nodes[i].setIdx(i);

		// Metric transformations
		InternalMetric internalMetric = AcceleratedMetricTransformator.transform(metric, nodes, veh);
		if(parameter.isOpenRouteAtStart() || parameter.isOpenRouteAtEnd())
			internalMetric = OpenRouteMetricTransformator.transform(internalMetric, nodes, parameter);

		// Metric transformations for optimization
		InternalMetric optMetric = internalMetric;
		optMetric = FixCostMetricTransformator.transform(internalMetric, nodes, veh);
		optMetric = PresetMetricTransformator.transform(optMetric, nodes);

		statusManager.fireMessage(StatusCode.RUNNING, "Nbr of nodes : "+nodes.length);

		return new XFVRPModel(nodes, internalMetric, optMetric, veh, parameter);
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
		Arrays.stream(model.getNodeArr()).forEach(n -> nodeMap.put(n.getExternID(), n));
		
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
				rep.importReport(sol.getReport());

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

	/**
	 * !!Experimental!!
	 * @return
	 */
	public String exportToString() {
		importer.finishImport();

		StringBuffer sb = new StringBuffer();
		// Nodes
		List<Node> nodeList = new ArrayList<>();
		int idx = 0;
		for (InternalDepotData data : importer.getDepotList()) {
			nodeList.add(data.createDepot(idx++));
			sb.append(data.exportToString());
		}
		for (InternalReplenishData data : importer.getReplenishList()) {
			nodeList.add(data.createReplenishment(idx++));
			sb.append(data.exportToString());
		}
		for (InternalCustomerData data : importer.getCustomerList()) {
			nodeList.add(data.createCustomer(idx++));
			sb.append(data.exportToString());
		}

		// Vehicles
		for (Vehicle veh : importer.getVehicles())
			sb.append(veh.exportToString());

		// Parameter

		// Distances
		sb.append("DISTANCES\n");
		for (Node n1 : nodeList)
			sb.append(n1.getExternID()+"\t");
		sb.append("\n");

		Vehicle veh = importer.getVehicles()[0];
		for (Node n1 : nodeList) {
			for (Node n2 : nodeList)
				sb.append(metric.getDistance(n1, n2, veh)+"\t");
			sb.append("\n");
		}
		sb.append("TIMES\n");
		for (Node n1 : nodeList) {
			for (Node n2 : nodeList)
				sb.append(metric.getTime(n1, n2, veh)+"\t");
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * !!Experimental!!
	 * @param s
	 */
	public void importFromString(String s) {
		try {
			BufferedReader br = new BufferedReader(new StringReader(s));
			int status = 0;

			MapMetric metric = new MapMetric();
			List<String> matrixNodes = new ArrayList<>();
			int cnt = 0;

			//:TODO Remove this code. It is only for correcting incorrect test data set
			Map<String, Float> shipAmountCorrection = new HashMap<>();

			while(br.ready()) {
				String[] arr;
				try {
					arr = br.readLine().split("\t");
				} catch (Exception e) {break;}

				if(status != 1 && arr[0].equals("DISTANCES")) {
					status = 1;
					arr = br.readLine().split("\t");
					for (int i = 0; i < arr.length; i++)
						matrixNodes.add(arr[i]);
					cnt = 0;
					continue;
				} else if(status != 2 && arr[0].equals("TIMES")) {
					status = 2;
					cnt = 0;
					continue;
				}

				if(status == 0) {
					if(arr[0].equals("DEPOT")) {
						DepotData d = this.addDepot()
								.setExternID(arr[1])
								.setGeoId(Integer.parseInt(arr[1]))
								.setXlong(Float.parseFloat(arr[3]))
								.setYlat(Float.parseFloat(arr[4]));
						String[] arr2 = arr[5].split("\\$");
						for (String ss : arr2) {
							if(ss.length() == 0)
								continue;
							String[] arr3 = ss.split("#");
							d.setTimeWindow(Float.parseFloat(arr3[0]), Float.parseFloat(arr3[1]));
						}

					} else if (arr[0].equals("CUSTOMER")) {
						//:TODO Remove this code. It is only for correcting incorrect test data set
						if(shipAmountCorrection.containsKey(arr[10]))
							arr[7] = (shipAmountCorrection.get(arr[10]) * -1)+";";
						else
							shipAmountCorrection.put(arr[10], readFromString(arr[7])[0]);

						CustomerData d = this.addCustomer()
								.setExternID(arr[1])
								.setGeoId(Integer.parseInt(arr[1])) // Instead of geoid, externid is set for geoid, because distance metric only knows extern id
								.setXlong(Float.parseFloat(arr[3]))
								.setYlat(Float.parseFloat(arr[4]))
								.setLoadType(LoadType.valueOf(arr[6]))
								.setDemand(readFromString(arr[7]))
								.setServiceTime(Float.parseFloat(arr[8]))
								.setServiceTimeForSite(Float.parseFloat(arr[9]))
								.setShipID(arr[10])
								.setNbrOfPackages(Integer.parseInt(arr[11]))
								.setHeightOfPackage(Integer.parseInt(arr[12]))
								.setWidthOfPackage(Integer.parseInt(arr[13]))
								.setLengthOfPackage(Integer.parseInt(arr[14]))
								.setWeightOfPackage(Float.parseFloat(arr[15]))
								.setLoadBearingOfPackage(Float.parseFloat(arr[16]))
								.setStackingGroupOfPackage(Integer.parseInt(arr[17]))
								.setContainerTypeOfPackage(Integer.parseInt(arr[18]))
								.setPresetBlockName(arr[19])
								//:TODO Remove this code. It is only for correcting incorrect test data set
								.setPresetBlockPos(-1)
								//								.setPresetBlockPos(Integer.parseInt(arr[20]))
								.setPresetBlockRank(Integer.parseInt(arr[21]));

						// Time windows
						String[] arr2 = arr[5].split("\\$");
						for (String ss : arr2) {
							if(ss.length() == 0)
								continue;
							String[] arr3 = ss.split("#");
							d.setTimeWindow(Float.parseFloat(arr3[0]), Float.parseFloat(arr3[1]));
						}

						// Blocking of Vehicle type
						Set<String> set = new HashSet<>();
						arr2 = arr[22].split("\\$");
						for (String ss : arr2) {
							if(ss.length() == 0)
								continue;
							set.add(ss);
						}
						d.setPresetBlockVehicleList(set);

						// BlackList
						set = new HashSet<>();
						arr2 = arr[23].split("\\$");
						for (String ss : arr2) {
							if(ss.length() == 0)
								continue;
							set.add(ss);
						}
						d.setPresetRoutingBlackList(set);
					} else if (arr[0].equals("REPLENISH")) {
						ReplenishData d = this.addReplenishment()
								.setExternID(arr[1])
								.setGeoId(Integer.parseInt(arr[1]))
								.setXlong(Float.parseFloat(arr[3]))
								.setYlat(Float.parseFloat(arr[4]));
						String[] ar2 = arr[5].split("\\$");
						for (String ss : ar2) {
							if(ss.length() == 0)
								continue;
							String[] arr3 = ss.split("#");
							d.setTimeWindow(Float.parseFloat(arr3[0]), Float.parseFloat(arr3[1]));
						}
					} else if (arr[0].equals("VEHICLE")) {
						this.addVehicle()
						.setName(arr[1])
						.setCapacity(readFromString(arr[2]))
						.setFixCost(Float.parseFloat(arr[3]))
						.setVarCost(Float.parseFloat(arr[4]))
						.setCount(Integer.parseInt(arr[5]))
						.setMaxRouteDuration(Float.parseFloat(arr[6]))
						.setMaxStopCount(Integer.parseInt(arr[7]))
						.setMaxWaitingTime(Float.parseFloat(arr[8]))
						.setVehicleMetricId(Integer.parseInt(arr[9]))
						.setCapacityOfVesselFirst(Float.parseFloat(arr[10]))
						.setCapacity2OfVesselFirst(Float.parseFloat(arr[11]))
						.setHeightOfVesselFirst(Integer.parseInt(arr[12]))
						.setWidthOfVesselFirst(Integer.parseInt(arr[13]))
						.setLengthOfVesselFirst(Integer.parseInt(arr[14]))
						.setCapacityOfVesselSecond(Float.parseFloat(arr[15]))
						.setCapacity2OfVesselSecond(Float.parseFloat(arr[16]))
						.setHeightOfVesselSecond(Integer.parseInt(arr[17]))
						.setWidthOfVesselSecond(Integer.parseInt(arr[18]))
						.setLengthOfVesselSecond(Integer.parseInt(arr[19]))
						.setMaxDrivingTimePerShift(Float.parseFloat(arr[20]))
						.setWaitingTimeBetweenShifts(Float.parseFloat(arr[21]));
					}
				} else if(status == 1) {
					int src = Integer.parseInt(matrixNodes.get(cnt));
					for (int i = 0; i < matrixNodes.size(); i++) {
						metric.addDist(src, Integer.parseInt(matrixNodes.get(i)), Float.parseFloat(arr[i])/1000.0f);
					}
					cnt++;
				} else if(status == 2) {
					int src = Integer.parseInt(matrixNodes.get(cnt));
					for (int i = 0; i < matrixNodes.size(); i++) 
						metric.addTime(src, Integer.parseInt(matrixNodes.get(i)), Float.parseFloat(arr[i]));
					cnt++;
				}
			}

			this.setMetric(metric);

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param string
	 * @return
	 */
	private static float[] readFromString(String string) {
		String[] chunk = string.split(";");
		List<Float> al = new ArrayList<>();
		try {
			for (int i = 0; i < chunk.length; i++)
				al.add(Float.parseFloat(chunk[i]));
		} catch (Exception e) {}

		float[] arr = new float[al.size()];
		for (int i = 0; i < al.size(); i++)
			arr[i] = al.get(i);
		return arr;
	}
}
