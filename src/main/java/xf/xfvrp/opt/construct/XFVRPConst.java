package xf.xfvrp.opt.construct;

import util.collection.ListMap;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.init.solution.vrp.VRPInitialSolutionBuilder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * General construction procedure
 * - Nearest allocation of customers to depots in MDVRP
 * - Optimization with Multiple Lambda Savings
 * - Choice of best solution 
 * 
 * @author hschneid
 *
 */
public class XFVRPConst extends XFVRPOptBase {

	private final XFVRPSavingsLamda savings = new XFVRPSavingsLamda();
	private final VRPInitialSolutionBuilder solutionBuilder = new VRPInitialSolutionBuilder();

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Solution execute(Solution solution) throws XFVRPException {
		// Prepare: nearest allocation of customer to depots
		ListMap<Integer, Node> depotAllocations = allocateNearestDepot(solution);

		Solution newSolution = new Solution(solution.getModel());

		for (int depIdx : depotAllocations.keySet()) {
			Node dep = model.getNodes()[depIdx];
			List<Node> customers = depotAllocations.get(depIdx);

			// Prepare customer list: customers with same block are placed together
			customers.sort(
					Comparator.comparingInt(Node::getPresetBlockIdx)
							.thenComparingInt(Node::getPresetBlockPos)
			);

			// Create temp giant tour with only one depot and allocated customers
			Solution savingsSolution = solutionBuilder.generateSolution(dep, customers, this.model);

			// Run optimizers for each piece and choose best
			savingsSolution = savings.execute(savingsSolution, model, statusManager);

			newSolution.addRoutes(savingsSolution.getRoutes());
		}

		return NormalizeSolutionService.normalizeRoute(newSolution);
	}

	private ListMap<Integer, Node> allocateNearestDepot(Solution solution) throws XFVRPException {
		ListMap<Integer, Node> depotAllocations = ListMap.create();

		Node[] depots = Arrays.copyOf(model.getNodes(), model.getNbrOfDepots());

		for (int routeIdx = 0; routeIdx < solution.getRoutes().length; routeIdx++) {
			Node[] route = solution.getRoutes()[routeIdx];
			int firstCustomerIdx = getFirstCustomerInRoute(route);
			if(firstCustomerIdx == -1)
				continue;

			int bestDepotIdx = findNearestDepot(depots, route[firstCustomerIdx]);
			allocateCustomersOfRoute(route, bestDepotIdx, depotAllocations);
		}

		return depotAllocations;
	}

	private int getFirstCustomerInRoute(Node[] route) {
		for (int i = 0; i < route.length; i++) {
			if(route[i].getSiteType() == SiteType.CUSTOMER)
				return i;
		}
		return -1;
	}

	private void allocateCustomersOfRoute(Node[] route, int bestDepotIdx, ListMap<Integer, Node> depotAllocations) {
		for (int i = route.length - 1; i >= 0; i--) {
			if(route[i].getSiteType() == SiteType.CUSTOMER) {
				depotAllocations.put(bestDepotIdx, route[i]);
			}
		}
	}

	private int findNearestDepot(Node[] depots, Node customer) throws XFVRPException {
		int bestIdx = -1;
		if(depots.length > 1) {
			float bestDistance = Float.MAX_VALUE;
			for (Node depot : depots) {
				// Check allowed depot for this customer
				if(customer.getPresetDepotList().size() > 0 &&
						!customer.getPresetDepotList().contains(depot.getIdx())) {
					continue;
				}

				float distance = getDistance(depot, customer);
				if(distance < bestDistance) {
					bestDistance = distance;
					bestIdx = depot.getIdx();
				}
			}
		} else if(depots.length == 1) {
			bestIdx = depots[0].getIdx();
		}

		return bestIdx;
	}
}
