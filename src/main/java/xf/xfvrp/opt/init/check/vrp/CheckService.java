package xf.xfvrp.opt.init.check.vrp;

import xf.xfvrp.base.*;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.BlockPositionConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.XFVRPOptType;
import xf.xfvrp.opt.init.solution.vrp.SolutionBuilderDataBag;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 
 * Copyright (c) 2012-2020 Holger Schneider
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

	private XFVRPOptBase optimizationMethod = XFVRPOptType.RELOCATE.createInstance();
	private CheckCustomerService checkCustomerService = new CheckCustomerService();

	public CheckService() throws XFVRPException {
	}

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
	 */
	public SolutionBuilderDataBag check(XFVRPModel model, List<Node> invalidNodes) throws XFVRPException {
		SolutionBuilderDataBag solutionBuilderDataBag = new SolutionBuilderDataBag();

		Map<Integer, List<Node>> blocks = getBlocks(model);

		checkBlocks(blocks, solutionBuilderDataBag, invalidNodes, model);

		return solutionBuilderDataBag;
	}

	private Map<Integer, List<Node>> getBlocks(XFVRPModel model) {
		return Arrays
				.stream(model.getNodes())
				.collect(Collectors.groupingBy(Node::getPresetBlockIdx));
	}

	private void checkBlocks(Map<Integer, List<Node>> blocks, SolutionBuilderDataBag solutionBuilderDataBag, List<Node> invalidNodes, XFVRPModel model) throws XFVRPException {
		for (Map.Entry<Integer, List<Node>> entry : blocks.entrySet()) {
			int blockIdx = entry.getKey();
			List<Node> nodesOfBlock = entry.getValue();
			solutionBuilderDataBag.resetKnownSequencePositions();

			nodesOfBlock.sort(Comparator.comparingInt(Node::getPresetBlockPos));

			boolean isValid = checkNodesOfBlock(blockIdx, nodesOfBlock, solutionBuilderDataBag, invalidNodes, model);
			if(!isValid) 
				continue;

			// Check non default blocks
			checkBlock(solutionBuilderDataBag, invalidNodes, model, blockIdx, nodesOfBlock);

			checkMaxWaiting(nodesOfBlock, model);
		}
	}

	private boolean checkNodesOfBlock(int blockIdx, List<Node> nodesOfBlock, SolutionBuilderDataBag solutionBuilderDataBag, List<Node> invalidNodes, XFVRPModel model) throws XFVRPException {
		for (Node node : nodesOfBlock) {
			if(node.getSiteType() == SiteType.DEPOT)
				solutionBuilderDataBag.getValidDepots().add(node);
			else if(node.getSiteType() == SiteType.REPLENISH)
				solutionBuilderDataBag.getValidReplenish().add(node);
			else if(checkCustomerService.checkCustomer(node, model, solutionBuilderDataBag)) {
				solutionBuilderDataBag.getValidCustomers().add(node);
				if(node.getPresetBlockPos() != BlockPositionConverter.UNDEF_POSITION)
					solutionBuilderDataBag.getKnownSequencePositions().add(node.getPresetBlockPos());
			} else {
				// Customer is not valid

				// If this is not the default block and one node
				// of this block is invalid, then all nodes are
				// set to invalid.
				if(blockIdx != BlockNameConverter.DEFAULT_BLOCK_IDX) {
					setNodesOfBlockInvalid(nodesOfBlock, invalidNodes, node);
					solutionBuilderDataBag.getValidCustomers().removeAll(nodesOfBlock);

					return false;
				}

				invalidNodes.add(node);
			}
		}

		return true;
	}

	private void checkBlock(
			SolutionBuilderDataBag solutionBuilderDataBag,
			List<Node> invalidNodes,
			XFVRPModel model,
			int blockIdx,
			List<Node> nodesOfBlock) throws XFVRPException {
		if(blockIdx != BlockNameConverter.DEFAULT_BLOCK_IDX) {
			if(!checkBlock(nodesOfBlock, model)) {
				solutionBuilderDataBag.getValidCustomers().removeAll(nodesOfBlock);
				invalidNodes.addAll(nodesOfBlock);
			}
		}
	}

	private void setNodesOfBlockInvalid(List<Node> nodesOfBlock, List<Node> invalidNodes, Node node) {
		for (Node n : nodesOfBlock)
			// First node of block has already a detailed information of invalidity
			if(n != node)
				n.setInvalidReason(
						node.getInvalidReason(),
						"Customer " + n.getExternID() + " is invalid because block " + node.getPresetBlockIdx() + " is invalid. See invalid argument of customer "+node.getExternID()
						);
		invalidNodes.addAll(nodesOfBlock);
	}

	/**
	 * Checks a situation where no customer can be reached in waiting time limit.
	 */
	private void checkMaxWaiting(List<Node> nodeList, XFVRPModel model) {
		// TODO Auto-generated method stub

	}

	/**
	 * Checks the block constraints like
	 * the block can be set into a single route without constraint violation
	 */
	private boolean checkBlock(List<Node> nodesOfBlock, XFVRPModel model) throws XFVRPException {
		// A block can be allocated to each depot in multi depot problems
		for (int i = 0; i < model.getNbrOfDepots(); i++) {

			// Build a route with all nodes of a block
			Node[] route = buildCheckRoute(nodesOfBlock, model.getNodes()[i]);

			// Check with Relocate optimization, if there is a sequence of nodes
			// in the block, which are valid for the constraints.
			Solution solution = new Solution();
			solution.addRoute(route);

			solution = optimizationMethod.execute(solution, model, null);

			Quality q = optimizationMethod.check(solution);
			
			// If any depot is okay, then everything is fine
			if(q.getPenalty() == 0)
				return true;
		}
		return false;
	}

	private Node[] buildCheckRoute(List<Node> nodesOfBlock, Node depot) {
		Node[] route = new Node[nodesOfBlock.size() + 2];
		
		int idx = 0;
		
		route[idx++] = Util.createIdNode(depot, 0);
		for (int j = nodesOfBlock.size() - 1; j >= 0; j--) {
			if(nodesOfBlock.get(j).getSiteType() == SiteType.CUSTOMER)
				route[idx++] = nodesOfBlock.get(j); 
		}
		route[idx++] = Util.createIdNode(depot, 1);
		
		return Arrays.copyOf(route, idx);
	}

}
