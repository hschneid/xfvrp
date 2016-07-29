package xf.xfvrp.opt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.monitor.StatusManager;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * General concept in classic Container Routing is the independence of routes.
 * A change in one route leaves all decisions in another route valid. So the
 * separation of route plan in several blocks is possible and their union either.
 * 
 * This class provides methods to optimize a big route plan by separating it into
 * several blocks, where each block may contains ALLOWED_NBR_OF_CUSTOMERS_IN_BLOCK
 * number of customers. Each block is optimized by a given optimization procedure.
 * Last but not least the routes of all blocks are joined.
 * 
 * The loss of separation is covered by iteration of this separation and union, where
 * the separation process is randomized. So the set of routes is different in each 
 * iteration. If optimization can not found an improvement in at least one block for
 * ALLOWED_NON_IMPROVES times, the search process will be terminated.
 * 
 * @author hschneid
 *
 */
public class XFVRPOptSplitter {

	private static int ALLOWED_NBR_OF_CUSTOMERS_IN_BLOCK = 250;
	
	private static int ALLOWED_NBR_OF_BLOCKS = 1;
	private static int ALLOWED_NON_IMPROVES = 2;
	private static Random rand = new Random(1234);

	/**
	 * Executes the optimization by splitting a big route plan into smaller
	 * blocks with less routes and optimizes each block independently. Afterwards
	 * the optimized blocks are joined back. This process is iterated until no further
	 * improvement could be found.
	 * 
	 * @param giantTour Current route plan
	 * @param model Model with nodes, distances and parameters
	 * @param opt Optimization procedure
	 * @param statusManager
	 * @return Optimized route plan 
	 */
	public Node[] execute(final Node[] giantTour, XFVRPModel model, StatusManager statusManager, XFVRPOptBase opt) {
		ALLOWED_NBR_OF_BLOCKS = (int)Math.max(1, model.getNbrOfNodes() / (float)ALLOWED_NBR_OF_CUSTOMERS_IN_BLOCK);
		
		Node[] best = Arrays.copyOf(giantTour, giantTour.length);
		float bestCost = getCost(giantTour, model);
		
		int nbrOfNonImproves = 0;
		while(true) {
			// Zerlege
			List<Node>[] blocks = splitIntoBlocks(best);

			// Optimiere
			List<Node> resultGiantList = new ArrayList<>();
			for (List<Node> giantList : blocks) {
				if(giantList.size() == 0)
					continue;
				Node[] gT = giantList.toArray(new Node[0]);
				opt.execute(gT, model, statusManager);
				resultGiantList.addAll(Arrays.asList(gT));
			}
			
			// Pr�fe auf Verbesserung
			float cost = getCost(resultGiantList.toArray(new Node[0]), model);
			if(cost < bestCost) {
				best = resultGiantList.toArray(new Node[0]);
				bestCost = cost;
				nbrOfNonImproves = 0;
			} else {
				nbrOfNonImproves++;
				if(nbrOfNonImproves > ALLOWED_NON_IMPROVES) {
					break;
				}
			}
		}

		return best;
	}

	/**
	 * Splits a big route plan into smaller blocks. The number of
	 * blocks is precalculated by the average number of customers
	 * in a block. A route can not be split into two routes.
	 * 
	 * @param giantRoute Current route plan
	 * @return List of blocks, where each block is a route plan (small giant tour)
	 */
	@SuppressWarnings("unchecked")
	private static List<Node>[] splitIntoBlocks(Node[] giantTour) {
		List<Node>[] blocks = new ArrayList[ALLOWED_NBR_OF_BLOCKS];
		for (int i = 0; i < blocks.length; i++)
			blocks[i] = new ArrayList<>();

		int lastDepot = 0;
		for (int i = 1; i < giantTour.length; i++) {
			if(giantTour[i].getSiteType() == SiteType.DEPOT) {
				int blockIdx = rand.nextInt(ALLOWED_NBR_OF_BLOCKS);
				for (int j = lastDepot; j < i; j++)
					blocks[blockIdx].add(giantTour[j]);
				lastDepot = i;
			}
		}
		
		for (int i = 0; i < blocks.length; i++)
			if(blocks[i].size() > 0)
				blocks[i].add(blocks[i].get(0));
		
		return blocks;
	}
	
	/**
	 * Returns the cost of route plan by building up a report.
	 * 
	 * @param giantRoute Current route plan
	 * @param model Current model with nodes, distances and parameters
	 * @return Fitness of current solution
	 */
	private float getCost(Node[] giantTour, XFVRPModel model) {
		XFVRPSolution sol = new XFVRPSolution(giantTour, model);
		return sol.getReport().getSummary().getCost();
	}
}
