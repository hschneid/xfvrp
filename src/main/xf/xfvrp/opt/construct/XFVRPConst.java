package xf.xfvrp.opt.construct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import util.collection.ListMap;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.opt.XFVRPOptBase;

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
	public Node[] execute(Node[] giantRoute) {
		// Prepare: nearest allocation of customer to depots
		ListMap<Integer, Node> allocMap = allocateNearestDepot(giantRoute);

		// Separate giantRoute into pieces of nearest allocated customers
		List<Node> giantList = new ArrayList<>();

		int depIDGlobal = 0;
		for (int depIdx : allocMap.keySet()) {
			Node dep = model.getNodeArr()[depIdx];
			List<Node> customers = allocMap.get(depIdx);

			// Prepare customer list: customers with same block are placed togehter
			customers.sort((c1, c2) -> 
			(c1.getPresetBlockIdx() - c2.getPresetBlockIdx() == 0) ?
					c1.getPresetBlockPos() - c2.getPresetBlockPos() :
						c1.getPresetBlockIdx() - c2.getPresetBlockIdx()
					);

			// Create temp giant route with only one depot and allocated customers
			Node[] gT = buildGiantRouteForOptimization(dep, customers);

			// Run optimizers for each piece and choose best
			gT = savings.execute(gT, model, statusManager);

			// Concatenate piece to new giant route
			for (Node n : gT) {
				if(n.getSiteType() == SiteType.DEPOT)
					giantList.add(Util.createIdNode(dep, depIDGlobal++));
				else
					giantList.add(n);
			}
		}

		return Util.normalizeRoute(giantList.toArray(new Node[giantList.size()]), model);
	}

	/**
	 * @param giantRoute
	 * @return
	 */
	private ListMap<Integer, Node> allocateNearestDepot(Node[] giantTour) {
		ListMap<Integer, Node> allocMap = ListMap.create();
		{
			List<Node> depotList = Arrays
					.stream(model.getNodeArr())
					.filter(n -> n.getSiteType() == SiteType.DEPOT)
					.collect(Collectors.toList());

			for (int i = 0; i < giantTour.length; i++) {
				Node n = giantTour[i];

				if(n.getSiteType() == SiteType.CUSTOMER) {

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
					} else {
						bestIdx = depotList.get(0).getIdx();
					}

					if(bestIdx == -1)
						throw new IllegalStateException();

					allocMap.put(bestIdx, n);

					if(depotList.size() > 1) {
						// All other nodes in this route have to be
						// put to the same depot
						for (int j = i + 1; j < giantTour.length; j++) {
							Node nextN = giantTour[j];
							if(nextN.getSiteType() == SiteType.DEPOT)
								break;

							allocMap.put(bestIdx, nextN);

							i++;
						}
					}
				}
			}
		}
		return allocMap;
	}

	/**
	 * @param dep
	 * @param customers
	 * @return
	 */
	private Node[] buildGiantRouteForOptimization(Node dep, List<Node> customers) {
		Node[] gT = new Node[customers.size() * 2 + 1];

		int idx = 0;
		int depID = 0;
		int lastBlockIdx = BlockNameConverter.UNDEF_BLOCK_IDX;
		for (int i = 0; i < customers.size(); i++) {
			int blockIdx = customers.get(i).getPresetBlockIdx();

			// Default blocks or Change of block index lead to a new route
			if(lastBlockIdx == BlockNameConverter.DEFAULT_BLOCK_IDX || blockIdx != lastBlockIdx)
				gT[idx++] = Util.createIdNode(dep, depID++);

			gT[idx++] = customers.get(i);

			lastBlockIdx = blockIdx;
		}
		gT[idx++] = Util.createIdNode(dep, depID++);

		return Arrays.copyOf(gT, idx);
	}
}
