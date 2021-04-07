package xf.xfvrp.opt.init.solution.vrp;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.init.PresetSolutionBuilder;
import xf.xfvrp.opt.init.check.vrp.CheckService;

import java.util.*;

/**
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Creates a trivial solution out of the model.
 *
 * The solution must be feasible/valid, but no optimization is
 * applied.
 *
 * @author hschneid
 *
 */
public class VRPInitialSolutionBuilder {

	public Solution build(XFVRPModel model, List<Node> invalidNodes, StatusManager statusManager) throws XFVRPException {
		List<Node> validNodes = getValidCustomers(model, invalidNodes); 

		Solution solution = buildSolution(validNodes, model, statusManager);

		NormalizeSolutionService.normalizeRoute(solution, model);

		return solution;
	}

	/**
	 * Builds the giant tour. All invalid nodes are filtered out before.
	 * 
	 * @param nodes List of nodes which are valid
	 * @param model Current model of nodes, distances and parameters
	 * @return Current route plan of single trips per customer
	 */
	private Solution buildSolution(List<Node> nodes, XFVRPModel model, StatusManager statusManager) {
		if(nodes == null) {
			return new Solution();
		}
		
		// If user has given a predefined solution
		if(model.getParameter().getPredefinedSolutionString() != null)
			return new PresetSolutionBuilder().build(nodes, model, statusManager);
	
		return generateSolution(nodes, model);
	}

	private Solution generateSolution(List<Node> nodes, XFVRPModel model) {
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
		for (int i = model.getNbrOfDepots() + model.getNbrOfReplenish(); i < nodes.size(); i++) {
			Node currNode = nodes.get(i);
	
			// Reduce allowed depots to preset allowed depots
			List<Integer> allowedDepots = getAllowedDepots(currNode, depots);
	
			// Add a depot after each change of block or unblocked customer
			final int blockIdx = currNode.getPresetBlockIdx();
			if(blockIdx == BlockNameConverter.DEFAULT_BLOCK_IDX || blockIdx != lastBlockIdx) {
				// Get an index for an element of allowed depots
				int idx = depotIdx % allowedDepots.size();
				// Add depot with new own id
				gL.add(Util.createIdNode(depotMap.get(allowedDepots.get(idx)), maxIdx++));
			}

			// Add customer
			gL.add(currNode);

			depotIdx++;
			lastBlockIdx = blockIdx;
		}
		// Add last depot
		gL.add(Util.createIdNode(nodes.get(depotIdx % depots.size()), maxIdx));

		Solution solution = new Solution();
		solution.setGiantRoute(gL.toArray(new Node[0]));
		return solution;
	}

	private List<Integer> getAllowedDepots(Node currNode, Set<Integer> depots) {
		if(currNode.getPresetDepotList().size() > 0) {
			Set<Integer> allowedDepots = new HashSet<>(depots);
			allowedDepots.retainAll(currNode.getPresetDepotList());
			return new ArrayList<>(allowedDepots);
		}

		return new ArrayList<>(depots);
	}

	private List<Node> getValidCustomers(XFVRPModel model, List<Node> invalidNodes) throws XFVRPException {
		SolutionBuilderDataBag solutionBuilderDataBag = new CheckService().check(model, invalidNodes);

		// If all customers are invalid for this vehicle and parameters optimization has to be skipped.
		if(solutionBuilderDataBag.getValidCustomers().size() == 0) {
			return null;
		}

		// Consider Preset Rank and Position
		solutionBuilderDataBag.getValidCustomers().sort(
				Comparator
						.comparingInt(Node::getPresetBlockIdx)
						.thenComparingInt(Node::getPresetBlockPos)
		);

		List<Node> validNodes = new ArrayList<>();
		validNodes.addAll(solutionBuilderDataBag.getValidDepots());
		validNodes.addAll(solutionBuilderDataBag.getValidReplenish());
		validNodes.addAll(solutionBuilderDataBag.getValidCustomers());

		return validNodes;
	}

}
