package xf.xfvrp.opt.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import xf.xfvrp.base.InvalidReason;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPBase;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.monitor.StatusCode;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.XFVRPOptType;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This is the standard initialization of the giant tour
 * out of the read data. The first thing of an optimization
 * process is the creation of such a giant tour. Hereby all
 * orders/customers will be arranged in a way, that each
 * order/customer is visited by its own tour.
 * 
 * If multiple depots are available, each single route has
 * an alternating depot.
 * 
 * Some construction optimization methods ignore the giant tours
 * and rebuild their own. But for reasons of convenience in
 * all cases a giant tour is initialized by this class.
 * 
 * If orders/customers can not be served in a valid way with the
 * given restrictions, they are excluded in an invalid list. These
 * orders/customers are reinserted in a later step of optimization.
 * 
 * @author hschneid
 *
 */
public class XFInit extends XFVRPBase<XFVRPModel> {

	private List<Node> invalidNodes;

	/**
	 * Builds the giant route.<br>
	 * A list of nodes, where the depot is allowed to be placed multiple times.
	 * Each area surrounded by two depots is called route. A giant route has to
	 * start and end with a depot.
	 * 
	 * Multiple depots are inserted in a alternating sequence.
	 * 
	 * If nodes cannot be served within the given constraints, these nodes
	 * are excluded from optimization in the invalidNodes list.
	 * 
	 * @param model
	 * @param statusManager
	 * @param invalidNodes
	 * @return giant route 
	 */
	public Node[] initGiantRoute(XFVRPModel model, StatusManager statusManager, List<Node> invalidNodes) {
		this.invalidNodes = invalidNodes;
		this.statusManager = statusManager;
		this.model = model;

		return execute(null);
	}

	/**
	 * Structural checks of the nodes without model 
	 * 
	 * @param nodes
	 * @param vehicle
	 * @param plannedCustomers 
	 * @return list of valid nodes
	 */
	public Node[] precheck(final Node[] nodes, Vehicle vehicle, boolean[] plannedCustomers) {
		if(nodes.length == 0) {
			statusManager.fireMessage(StatusCode.ABORT, "No nodes found.");
			throw new IllegalArgumentException("No nodes found.");
		}

		// Fetch block informations
		Map<Integer, List<Node>> blockMap = 
				Arrays.stream(nodes)
				.filter(n -> n.getSiteType() == SiteType.CUSTOMER)
				.filter(n-> n.getPresetBlockIdx() != BlockNameConverter.DEFAULT_BLOCK_IDX)
				.collect(Collectors.groupingBy(n -> n.getPresetBlockIdx()));

		// Already planned customers (true = planned, false = unplanned, DEPOTS/REPLENISH always false)
		List<Node> nodeList = IntStream
				.range(0, plannedCustomers.length)
				.filter(i -> !plannedCustomers[i])
				.mapToObj(i -> nodes[i])
				.collect(Collectors.toList());

		// Check if customer is allowed for this vehicle type
		for (Node node : nodes) {
			if(node.getSiteType() == SiteType.CUSTOMER) {
				if(!node.getPresetBlockVehicleList().isEmpty() && !node.getPresetBlockVehicleList().contains(vehicle.idx)) {
					// Remove invalid customer from list
					node.setInvalidReason(InvalidReason.WRONG_VEHICLE_TYPE);
					nodeList.remove(node);

					// Remove all customers from block of invalid customer
					if(blockMap.containsKey(node.getPresetBlockIdx())) {
						blockMap
						.get(node.getPresetBlockIdx())
						.forEach(n -> {
							n.setInvalidReason(InvalidReason.WRONG_VEHICLE_TYPE);
							nodeList.remove(n);
						});
					}
				}
			} 
		}

		// TODO: PDP shipments

		return nodeList.toArray(new Node[0]);
	}

	/**
	 * 
	 * @param giantRoute
	 * @return
	 */
	@Override
	protected Node[] execute(Node[] giantRoute) {
		List<Node> validNodes = new ArrayList<>();
		List<Node> validDepots = new ArrayList<>();
		List<Node> validReplenish = new ArrayList<>();
		
		Map<Integer, List<Node>> blockMap = Arrays.stream(model.getNodeArr()).collect(Collectors.groupingBy(g -> g.getPresetBlockIdx()));

		OUTER:
			for (int blockIdx : blockMap.keySet()) {
				List<Node> nodeList = blockMap.get(blockIdx);
				Set<Integer> seqPosSet = new HashSet<>();

				// Sort nodes with smallest block position at first
				nodeList.sort((o1, o2) -> o1.getPresetBlockPos() - o2.getPresetBlockPos());

				for (Node node : nodeList) {
					if(node.getSiteType() == SiteType.DEPOT)
						validDepots.add(node);
					else if(node.getSiteType() == SiteType.REPLENISH)
						validReplenish.add(node);
					// Check customer data
					else if(checkCustomer(node, model)) {
						validNodes.add(node);
						if(node.getPresetBlockRank() < 0)
							throw new IllegalArgumentException("The sequence rank " + node.getPresetBlockRank() + " in block " + node.getPresetBlockIdx() + " is lower than zero, which is forbidden.");
						if(node.getPresetBlockPos() < -1)
							throw new IllegalArgumentException("The sequence position " + node.getPresetBlockPos() + " in block " + node.getPresetBlockIdx() + " is lower than zero, which is forbidden.");
						if(node.getPresetBlockPos() >=0 && seqPosSet.contains(node.getPresetBlockPos()))
							throw new IllegalArgumentException("The sequence position " + node.getPresetBlockPos() + " in block " + node.getPresetBlockIdx() + " is given multiple times, which is forbidden.");
						seqPosSet.add(node.getPresetBlockPos());
					} else {
						// If this is not the default block and one node
						// of this block is invalid, then all nodes are 
						// need to be set to invalid.
						if(blockIdx != BlockNameConverter.DEFAULT_BLOCK_IDX) {
							// Copy invalid reason to all nodes of block
							for (Node n : nodeList)
								if(n != node)
									n.setInvalidReason(
											node.getInvalidReason(),
											"Customer " + n.getExternID() + " is invalid because block " + node.getPresetBlockIdx() + " is invalid. See invalid argument of customer "+node.getExternID()
											);

							invalidNodes.addAll(nodeList);
							continue OUTER;
						}
						invalidNodes.add(node);
					}
				}

				// Check non default blocks
				if(blockIdx != BlockNameConverter.DEFAULT_BLOCK_IDX && !checkBlock(nodeList, model)) {
					validNodes.removeAll(nodeList);
					invalidNodes.addAll(nodeList);
				}

				checkMaxWaiting(nodeList, model);
			}

		// If all customer nodes are invalid for this vehicle and parameters
		// optimization has to be skipped.
		if(validNodes.size() == 0)
			return new Node[0];

		// Consider Preset Rank and Position
		validNodes.sort((s1, s2) -> {
			int diff = s1.getPresetBlockIdx() - s2.getPresetBlockIdx();
			if(diff == 0)
				diff = s1.getPresetBlockPos() - s2.getPresetBlockPos();
			return diff;
		});

		// Set the ordering of nodes
		// 1. Depots
		// 2. Replenishments
		// 3. Customers
		validDepots.addAll(validReplenish);
		validDepots.addAll(validNodes);
		validNodes = validDepots;

		// If user has given a predefined solution
		if(model.getParameter().getPredefinedSolutionString() != null)
			return buildPredefinedGiantRoute(validNodes, model);

		// Else build a trivial solution with each customer (or block of customers) at one route
		return buildGiantRoute(validNodes, model);
	}

	/**
	 * Checks a customers whether it can be served within all constraints.
	 * If one constraint is violated, the customer is invalid for optimization.
	 * 
	 * The checked constraints are:
	 *  - A customer have to be allowed for this vehicle type
	 *  - No customer must have more demand than max loading capacity
	 *  - All customers must be reached from one depot (see Multiple Depots) directly within their time windows.
	 *  - The route duration for direct service from one depot (see Multiple Depots) must be smaller than maximal route duration.
	 * 
	 * If a customer leads to an invalid route plan, then the cause for this is written into the invalid reason at the customer object 
	 * 
	 * @param cust Customer node
	 * @param model Model with all necessary data
	 * @return Can the customer be served within given constraints?
	 */
	private boolean checkCustomer(Node cust, XFVRPModel model) {
		if(cust.getSiteType() != SiteType.CUSTOMER)
			throw new IllegalStateException("XFInit - Check of customer for no customer node.");
	
		// Check each depot if this customer can be serviced by this depot
		// with valid constraints
		boolean canBeValid = false;
		for (int i = 0; i < model.getNbrOfDepots(); i++) {
			Node depot = model.getNodeArr()[i];
	
			float travelTime = model.getTime(depot, cust);
			float travelTime2 = model.getTime(cust, depot);		
	
			// Check route duration with this customer
			float time = travelTime + travelTime2 + cust.getServiceTime();
			if(time > model.getVehicle().maxRouteDuration){
				cust.setInvalidReason(InvalidReason.TRAVEL_TIME, "Customer " + cust.getExternID() + " - Traveltime required: " + time);
				continue;
			}
	
			// Check time window
			float[] depTW = depot.getTimeWindow(0);
			float departureAtDepot = Math.max(depTW[0], cust.getEarliestPickupTimeAtDepot());
			float arrTime = departureAtDepot + travelTime;
			float[] custTW = cust.getTimeWindow(arrTime);
			arrTime = Math.max(arrTime, custTW[0]);
			// Check later than customer closing time
			if(arrTime > custTW[1]) {
				cust.setInvalidReason(InvalidReason.TIME_WINDOW);
				continue;
			}
			// Check later than depot closing time
			if(arrTime + travelTime2 + cust.getServiceTime() > depTW[1]) {
				cust.setInvalidReason(InvalidReason.TIME_WINDOW);
				continue;
			}
	
			canBeValid = true;
		}
	
		// Time Windows or duration
		if(!canBeValid)			
			return false;
	
		// Capacities
		float[] demandArr = cust.getDemand();
		float[] capArr = model.getVehicle().capacity;
	
		for (int i = 0; i < Math.min(demandArr.length, capArr.length); i++) {
			if(	demandArr[i] > capArr[i]) {
				cust.setInvalidReason(
						InvalidReason.CAPACITY,
						"Customer " + cust.getExternID() + " - Capacity " + (i + 1) + " demand: " +capArr[i]+" required: "+demandArr[i]
						);
				return false;
			} 				
		}
	
		return true;
	}

	/**
	 * Checks a situation where no customer can be reached in waiting time limit.
	 * 
	 * @param nodeList
	 * @param model
	 */
	private void checkMaxWaiting(List<Node> nodeList, XFVRPModel model) {
		// TODO Auto-generated method stub

	}

	/**
	 * Checks the block contraints like
	 * the block can be set into a single route without constraint violation
	 * 
	 * @param nodeList
	 * @param model
	 * @return
	 */
	private boolean checkBlock(List<Node> nodeList, XFVRPModel model) {
		XFVRPOptBase opt = XFVRPOptType.RELOCATE.createInstance();

		Collections.sort(nodeList, new Comparator<Node>() {
			@Override
			public int compare(Node arg0, Node arg1) {
				return arg0.getPresetBlockPos() - arg1.getPresetBlockPos();
			}
		});

		boolean returnVal = false;
		// A block can be allocated to each depot in multi depot problems
		for (int i = 0; i < model.getNbrOfDepots(); i++) {
			Node[] blockGiantRoute = new Node[nodeList.size() + 2];

			// Build a route with all nodes of a block
			blockGiantRoute[0] = Util.createIdNode(model.getNodeArr()[i], 0);
			for (int j = 0; j < nodeList.size(); j++)
				blockGiantRoute[j+1] = nodeList.get(j); 
			blockGiantRoute[nodeList.size() + 1] = Util.createIdNode(model.getNodeArr()[i], 1);

			// Check with Relocate optimization, if there is a sequence of nodes
			// in the block, which are valid for the constraints.
			blockGiantRoute = opt.execute(blockGiantRoute, model, statusManager);
			Quality q = opt.check(blockGiantRoute);
			if(q.getPenalty() == 0)
				returnVal |= true;
		}
		return returnVal;
	}

	/**
	 * Builds the giant tour. All invalid nodes are filtered out before.
	 * 
	 * @param nodes List of nodes which are valid
	 * @param model Current model of nodes, distances and parameters
	 * @return Current route plan of single trips per customer
	 */
	private Node[] buildGiantRoute(List<Node> nodes, XFVRPModel model) {
		if(nodes.size() == 0)
			return new Node[0];

		List<Node> gL = new ArrayList<>();

		// GlobalIndex -> Depot
		Map<Integer, Node> depotMap = new HashMap<>();
		for (int i = 0; i < model.getNbrOfDepots(); i++)
			depotMap.put(nodes.get(i).getGlobalIdx(), nodes.get(i));

		// Create single routes for each block or single customer without block
		int depotIdx = 0;
		int maxIdx = 0;
		int lastBlockIdx = Integer.MAX_VALUE;
		// Create single routes for each block or single customer without block
		// Consider preset depot
		Set<Integer> depots = new HashSet<>();
		for (Node dep : nodes.subList(0, model.getNbrOfDepots()))
			depots.add(dep.getGlobalIdx());
		Set<Integer> allowedDepots = new HashSet<>(depots); 
		for (int i = model.getNbrOfDepots() + model.getNbrOfReplenish(); i < nodes.size(); i++) {
			Node currNode = nodes.get(i);

			// Reduce allowed depots to preset allowed depots
			if(currNode.getPresetDepotList().size() > 0)
				allowedDepots.retainAll(currNode.getPresetDepotList());

			// Add a depot after each change of block or unblocked customer
			final int blockIdx = currNode.getPresetBlockIdx();
			if(blockIdx == BlockNameConverter.DEFAULT_BLOCK_IDX || blockIdx != lastBlockIdx) {
				// Get an index for an element of allowed depots
				int idx = depotIdx % allowedDepots.size();
				// Add depot with new own id
				gL.add(Util.createIdNode(depotMap.get(allowedDepots.toArray(new Integer[0])[idx]), maxIdx++));
				// Refill allowedDepots
				allowedDepots = new HashSet<>(depots);
			}

			// Add customer
			gL.add(currNode);

			depotIdx++;
			lastBlockIdx = blockIdx;
		}
		// Add last depot
		gL.add(Util.createIdNode(nodes.get(depotIdx % depots.size()), maxIdx++));

		// Create additional route with all replenishing nodes on it.
		for (int i = model.getNbrOfDepots(); i < model.getNbrOfDepots() + model.getNbrOfReplenish(); i++)
			gL.add(model.getNodeArr()[i].copy());
		gL.add(Util.createIdNode(nodes.get(depotIdx % depots.size()), maxIdx++));

		return gL.toArray(new Node[0]);
	}

	/**
	 * 
	 * @param nodes
	 * @param model
	 * @return
	 */
	private Node[] buildPredefinedGiantRoute(List<Node> nodes, XFVRPModel model) {
		String predefinedSolutionString = model.getParameter().getPredefinedSolutionString();

		if(!checkPredefinedSolutionString(predefinedSolutionString))
			throw new IllegalStateException("The predefined solution string "+predefinedSolutionString+" is not valid.");

		// Seperate the solution string into the blocks
		predefinedSolutionString = predefinedSolutionString.substring(1, predefinedSolutionString.length() - 1);
		String[] predefinedBlocks = predefinedSolutionString.split("\\),\\(");
		predefinedBlocks[0] = predefinedBlocks[0].substring(1);
		predefinedBlocks[predefinedBlocks.length - 1] = predefinedBlocks[predefinedBlocks.length - 1].substring(0, predefinedBlocks[predefinedBlocks.length - 1].length() - 1);

		// Generate a map for each extern id to a node index
		Map<String, Integer> idMap = new HashMap<>();
		for (int i = 0; i < nodes.size(); i++)
			idMap.put(nodes.get(i).getExternID(), i);

		Set<Node> availableCustomers = new HashSet<>(nodes.subList(model.getNbrOfDepots() + model.getNbrOfReplenish(), nodes.size()));

		List<Node> giantRoute = new ArrayList<>();
		int depotIdx = 0;
		int depotId = 0;
		for (String block : predefinedBlocks) {
			String[] entries = block.split(",");

			// Every block has to start with a depot
			if(entries.length > 0 && idMap.containsKey(entries[0])) {
				Node n = nodes.get(idMap.get(entries[0]));
				if(n.getSiteType() == SiteType.DEPOT) {
					giantRoute.add(Util.createIdNode(n, depotId++));
				} else if (availableCustomers.contains(n)) {
					giantRoute.add(Util.createIdNode(nodes.get(depotIdx++), depotId++));
					depotIdx = depotIdx % model.getNbrOfDepots();
					giantRoute.add(n);
					availableCustomers.remove(n);
				} else {
					System.out.println(" Init warning - Node "+entries[0]+" is already in the solution.");
				}
			}

			// A block can hold customers and depots
			for (int i = 1; i < entries.length; i++) {
				if(idMap.containsKey(entries[i])) {
					Node n = nodes.get(idMap.get(entries[i]));
					if(n.getSiteType() == SiteType.DEPOT) {
						giantRoute.add(Util.createIdNode(n, depotId++));
						depotIdx = depotIdx % model.getNbrOfDepots();
					} else if (availableCustomers.contains(n)) {
						giantRoute.add(n);
						availableCustomers.remove(n);
					} else {
						System.out.println(" Init warning - Node "+entries[i]+" is already in the solution.");
					}
				} else {
					System.out.println(" Init warning - Node "+entries[i]+" is no valid customer (unknown).");
				}
			}

			// Every block has to end with a depot
			giantRoute.add(Util.createIdNode(nodes.get(depotIdx++), depotId++));
			depotIdx = depotIdx % model.getNbrOfDepots();
		}

		// Put the unassigned customers with single routes in the giant route
		for (Node customer : availableCustomers) {
			giantRoute.add(Util.createIdNode(nodes.get(depotIdx), depotId++));
			giantRoute.add(customer);
			depotIdx = ((depotIdx + 1) % model.getNbrOfDepots());
		}
		giantRoute.add(Util.createIdNode(nodes.get(depotIdx), depotId++));

		return giantRoute.toArray(new Node[0]);
	}

	/**
	 * 
	 * @param predefinedSolutionString
	 * @return
	 */
	private boolean checkPredefinedSolutionString(String predefinedSolutionString) {
		return predefinedSolutionString.matches("\\{(\\([^\\(\\)]+\\),)*(\\([^\\(\\)]+\\))+\\}");
	}
}
