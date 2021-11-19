package xf.xfvrp.opt;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.monitor.StatusManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * General concept in classic Vehicle Routing is the independence of routes.
 * A change in one route leaves all decisions in another route valid. So the
 * separation of route plan in several blocks is possible and their union either.
 * 
 * This class provides methods to optimize a big route plan by separating it into
 * several blocks, where each block may contain an allowed number of customers.
 * Each block is optimized by a given optimization procedure. Last but not least
 * the routes of all blocks are joined.
 * 
 * The loss of separation is covered by iteration of this separation and union phases,
 * where the separation process is randomized. So the set of routes is different in each
 * iteration. If optimization can not found any further improvement in at least one block for
 * ALLOWED_NON_IMPROVES times, the search process will be terminated.
 * 
 * @author hschneid
 *
 */
public class XFVRPOptSplitter {

	private static final int ALLOWED_NBR_OF_CUSTOMERS_IN_BLOCK = 250;
	private static final int ALLOWED_NBR_OF_NON_IMPROVES = 2;

	private final ExecutorService ex = Executors.newFixedThreadPool(4);

	/**
	 * Executes the optimization by splitting a big route plan into smaller
	 * blocks with less routes and optimizes each block independently. Afterwards
	 * the optimized blocks are joined back. This process is iterated until no further
	 * improvement could be found.
	 */
	public Solution execute(final Solution solution, XFVRPModel model, StatusManager statusManager, XFVRPOptBase opt) throws XFVRPException {
		int allowedNbrOfBlocks = init(model);

		Solution best = solution.copy();
		float bestCost = getCost(solution, opt);

		int nbrOfTries = 0;
		while(nbrOfTries < ALLOWED_NBR_OF_NON_IMPROVES) {
			
			List<Solution> blocks = splitIntoBlocks(best, opt.getRandom(), allowedNbrOfBlocks);

			Solution newSolution = optimizeBlocks(model, statusManager, opt, blocks);

			float cost = getCost(newSolution, opt);
			
			nbrOfTries++;
			
			if(cost < bestCost) {
				best = newSolution;
				bestCost = cost;
				nbrOfTries = 0;
			}
		}

		return best;
	}

	private int init(XFVRPModel model) {
		return (int)Math.max(1, model.getNbrOfNodes() / (float)ALLOWED_NBR_OF_CUSTOMERS_IN_BLOCK);
	}

	private Solution optimizeBlocks(XFVRPModel model, StatusManager statusManager, XFVRPOptBase opt, List<Solution> blocks) throws XFVRPException {
		Solution newSolution = new Solution(model);

		List<Callable<Solution>> tasksList = new ArrayList<>();
		for (Solution block : blocks) {
			// Execute optimization in "Future"
			tasksList.add(() -> opt.execute(block, model, statusManager));
		}

		// Optimize block
		try {
			List<Future<Solution>> results = ex.invokeAll(tasksList);
			for(Future<Solution> result : results) {
				newSolution.addRoutes(result.get().getRoutes());
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		return newSolution;
	}

	/**
	 * Splits a big route plan into smaller blocks. The number of
	 * blocks is precalculated by the average number of customers
	 * in a block. A route can not be split into two routes.
	 */
	private static List<Solution> splitIntoBlocks(Solution solution, Random random, int allowedNbrOfBlocks) {
		List<Node[]>[] blocks = initBlocks(allowedNbrOfBlocks);

		fillRoutesIntoBlocks(solution, blocks, random);

		return convertToSolutions(solution, blocks);
	}

	private static List<Solution> convertToSolutions(Solution solution, List<Node[]>[] blocks) {
		return Arrays.stream(blocks)
				.filter(b -> b.size() > 0)
				.map(block -> {
					Solution sol = new Solution(solution.getModel());
					for (Node[] route : block) {
						sol.addRoute(route);
					}
					return sol;
				})
				.collect(Collectors.toList());
	}

	private static void fillRoutesIntoBlocks(Solution solution, List<Node[]>[] blocks, Random random) {
		for (Node[] route : solution) {
			// Each route is 1 block
			int blockIdx = random.nextInt(blocks.length);
			// Store routes per block
			blocks[blockIdx].add(route);
		}
	}

	private static List<Node[]>[] initBlocks(int allowedNbrOfBlocks) {
		List<Node[]>[] blocks = new ArrayList[allowedNbrOfBlocks];
		for (int i = 0; i < blocks.length; i++)
			blocks[i] = new ArrayList<>();
		return blocks;
	}

	/**
	 * Returns the cost of route plan by building up a report.
	 */
	private float getCost(Solution solution, XFVRPOptBase optBase) throws XFVRPException {
		Quality quality = optBase.check(solution);
		return quality.getCost();
	}
}
