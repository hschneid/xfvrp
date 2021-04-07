package xf.xfvrp.opt.construct;

import util.collection.ListMap;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * General construction procedure
 * - Nearest allocation of customers to depots in MDVRP
 * - Optimization with Multiple Lamda Savings
 * - Choice of best solution 
 * 
 * @author hschneid
 *
 */
public class XFVRPConst extends XFVRPOptBase {

	private final XFVRPSavingsLamda savings = new XFVRPSavingsLamda();

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.model.XFBase#execute(de.fhg.iml.vlog.xftour.model.XFNode[])
	 */
	@Override
	public Solution execute(Solution solution) {
		// Prepare: nearest allocation of customer to depots
		ListMap<Integer, Node> allocMap = allocateNearestDepot(solution);

		// Separate giantRoute into pieces of nearest allocated customers
		List<Node> giantList = new ArrayList<>();

		int depIDGlobal = 0;
		for (int depIdx : allocMap.keySet()) {
			Node dep = model.getNodes()[depIdx];
			List<Node> customers = allocMap.get(depIdx);

			// Prepare customer list: customers with same block are placed togehter
			customers.sort((arg0, arg1) -> {
				int diff = arg0.getPresetBlockIdx() - arg1.getPresetBlockIdx();
				if(diff == 0)
					diff = arg0.getPresetBlockPos() - arg1.getPresetBlockPos();
				return diff;
			});

			// Create temp giant tour with only one depot and allocated customers
			Solution gT = buildGiantRouteForOptimization(dep, customers);

			// Run optimizers for each piece and choose best
			gT = savings.execute(gT, model, statusManager);

			// Concatenate piece to new giant tour
			for (Node n : gT.getGiantRoute()) {
				if(n.getSiteType() == SiteType.DEPOT)
					giantList.add(Util.createIdNode(dep, depIDGlobal++));
				else
					giantList.add(n);
			}
		}

		Solution newSolution = new Solution();
		newSolution.setGiantRoute(giantList.toArray(new Node[giantList.size()]));
		return NormalizeSolutionService.normalizeRoute(newSolution, model);
	}

	/**
	 * @param giantRoute
	 * @return
	 */
	private ListMap<Integer, Node> allocateNearestDepot(Solution solution) {
		ListMap<Integer, Node> allocMap = ListMap.create();

		List<Node> depotList = Arrays.stream(model.getNodes())
				.filter(node -> node.getSiteType() == SiteType.DEPOT)
				.collect(Collectors.toList());

		Node[] giantTour = solution.getGiantRoute();
		for (int i = 0; i < giantTour.length; i++) {
			Node n = giantTour[i];

			if(n.getSiteType() == SiteType.CUSTOMER) {

				int bestIdx = findNearestDepot(depotList, n);

				allocMap.put(bestIdx, n);

				i = allocateCustomersToNearestDepot(allocMap, depotList, giantTour, i, bestIdx);
			}
		}

		return allocMap;
	}

	private int allocateCustomersToNearestDepot(ListMap<Integer, Node> allocMap, List<Node> depotList, Node[] giantTour,
			int i, int bestIdx) {
		if(depotList.size() == 1)
			return i;

		for (int j = i + 1; j < giantTour.length; j++) {
			Node nextN = giantTour[j];
			if(nextN.getSiteType() == SiteType.DEPOT)
				return i;

			allocMap.put(bestIdx, nextN);

			i++;
		}

		return i;
	}

	private int findNearestDepot(List<Node> depotList, Node n) {
		int bestIdx = -1;
		float bestDistance = Float.MAX_VALUE;
		if(depotList.size() > 1) {
			for (Node d : depotList) {
				float distance = getDistance(d, n);
				if(distance < bestDistance) {
					bestDistance = distance;
					bestIdx = d.getIdx();
				}
			}
		} else if(depotList.size() == 1) {
			bestIdx = depotList.get(0).getIdx();
		}

		if(bestIdx == -1)
			throw new IllegalStateException();
		
		return bestIdx;
	}

	/**
	 * @param dep
	 * @param customers
	 * @return
	 */
	private Solution buildGiantRouteForOptimization(Node dep, List<Node> customers) {
		Node[] gT = new Node[customers.size() * 2 + 1];

		int idx = 0;
		int depID = 0;
		int lastBlockIdx = BlockNameConverter.UNDEF_BLOCK_IDX;
		for (int i = 0; i < customers.size(); i++) {
			int blockIdx = customers.get(i).getPresetBlockIdx();

			// Default blocks or Change of block index lead to a new route
			if (lastBlockIdx == BlockNameConverter.DEFAULT_BLOCK_IDX || blockIdx != lastBlockIdx)
				gT[idx++] = Util.createIdNode(dep, depID++);

			gT[idx++] = customers.get(i);

			lastBlockIdx = blockIdx;
		}
		gT[idx++] = Util.createIdNode(dep, depID);

		Solution solution = new Solution();
		solution.setGiantRoute(Arrays.copyOf(gT, idx));

		return solution;
	}
}
