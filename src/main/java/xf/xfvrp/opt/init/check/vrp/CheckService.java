package xf.xfvrp.opt.init.check.vrp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.opt.init.solution.vrp.SolutionBuilderDataBag;

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
public class CheckService {

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
	 * @param invalidNodes
	 * @return giant route 
	 */
	public SolutionBuilderDataBag check(XFVRPModel model, List<Node> invalidNodes) {
		SolutionBuilderDataBag solutionBuilderDataBag = new SolutionBuilderDataBag();

		Map<Integer, List<Node>> blocks = getBlocks(model);

		checkBlocks(blocks, solutionBuilderDataBag, invalidNodes, model);
		
		return solutionBuilderDataBag;
	}

	private Map<Integer, List<Node>> getBlocks(XFVRPModel model) {
		Map<Integer, List<Node>> blocks = Arrays
				.stream(model.getNodes())
				.collect(Collectors.groupingBy(k -> k.getPresetBlockIdx()));
		
		return blocks;
	}

	private void checkBlocks(Map<Integer, List<Node>> blocks, SolutionBuilderDataBag solutionBuilderDataBag, List<Node> invalidNodes, XFVRPModel model) {
		for (int blockIdx : blocks.keySet()) {
			List<Node> nodesOfBlock = blocks.get(blockIdx);
			solutionBuilderDataBag.resetKnownSequencePositions();

			nodesOfBlock.sort((c1, c2) -> c1.getPresetBlockPos() - c2.getPresetBlockPos());

			boolean isValid = checkNodesOfBlock(blockIdx, nodesOfBlock, solutionBuilderDataBag, invalidNodes, model);
			if(!isValid) 
				continue;

			// Check non default blocks
			checkBlock(solutionBuilderDataBag, invalidNodes, model, blockIdx, nodesOfBlock);

			checkMaxWaiting(nodesOfBlock, model);
		}
	}

	private boolean checkNodesOfBlock(int blockIdx, List<Node> nodesOfBlock, SolutionBuilderDataBag solutionBuilderDataBag, List<Node> invalidNodes, XFVRPModel model) {
		CheckCustomerService checkCustomerService = new CheckCustomerService();
		
		for (Node node : nodesOfBlock) {
			if(node.getSiteType() == SiteType.DEPOT)
				solutionBuilderDataBag.getValidDepots().add(node);
			else if(node.getSiteType() == SiteType.REPLENISH)
				solutionBuilderDataBag.getValidReplenish().add(node);
			else if(checkCustomerService.checkCustomer(node, model, solutionBuilderDataBag)) {
				solutionBuilderDataBag.getValidNodes().add(node);
				solutionBuilderDataBag.getKnownSequencePositions().add(node.getPresetBlockPos());
			} else {
				// Customer is not valid
				
				// If this is not the default block and one node
				// of this block is invalid, then all nodes are
				// set to invalid.
				if(blockIdx != BlockNameConverter.DEFAULT_BLOCK_IDX) {
					setNodesOfBlockInvalid(nodesOfBlock, invalidNodes, node);

					return false;
				}
				
				invalidNodes.add(node);
			}
		}

		return true;
	}

	private void checkBlock(SolutionBuilderDataBag solutionBuilderDataBag, List<Node> invalidNodes, XFVRPModel model,
			int blockIdx, List<Node> nodesOfBlock) {
		if(blockIdx != BlockNameConverter.DEFAULT_BLOCK_IDX && !checkBlock(nodesOfBlock, model)) {
			solutionBuilderDataBag.getValidNodes().removeAll(nodesOfBlock);
			invalidNodes.addAll(nodesOfBlock);
		}
	}

	private void setNodesOfBlockInvalid(List<Node> nodesOfBlock, List<Node> invalidNodes, Node node) {
		for (Node n : nodesOfBlock)
			if(n != node)
				n.setInvalidReason(
						node.getInvalidReason(),
						"Customer " + n.getExternID() + " is invalid because block " + node.getPresetBlockIdx() + " is invalid. See invalid argument of customer "+node.getExternID()
						);
		invalidNodes.addAll(nodesOfBlock);
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
	 * @param nodesOfBlock
	 * @param model
	 * @return isValid
	 */
	private boolean checkBlock(List<Node> nodesOfBlock, XFVRPModel model) {
		XFVRPOptBase opt = XFVRPOptType.RELOCATE.createInstance();

		boolean isValid = false;
		
		// A block can be allocated to each depot in multi depot problems
		for (int i = 0; i < model.getNbrOfDepots(); i++) {
			Node[] blockGiantRoute = new Node[nodesOfBlock.size() + 2];

			// Build a route with all nodes of a block
			blockGiantRoute[0] = Util.createIdNode(model.getNodes()[i], 0);
			for (int j = 0; j < nodesOfBlock.size(); j++)
				blockGiantRoute[j+1] = nodesOfBlock.get(j); 
			blockGiantRoute[nodesOfBlock.size() + 1] = Util.createIdNode(model.getNodes()[i], 1);

			// Check with Relocate optimization, if there is a sequence of nodes
			// in the block, which are valid for the constraints.
			Solution solution = new Solution();
			solution.setGiantRoute(blockGiantRoute);

			solution = opt.execute(solution, model, null);
			Quality q = opt.check(solution);
			if(q.getPenalty() == 0)
				isValid |= true;
		}
		return isValid;
	}

}
