package xf.xfvrp.opt.init.solution.pdp;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.NormalizeSolutionService;
import xf.xfvrp.base.Util;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.init.check.pdp.PDPCheckService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class PDPInitialSolutionBuilder {

	public Solution build(XFVRPModel model) {
		Map<Integer, Node[]> shipments = getValidNodes(model); 

		Solution solution = buildSolution(shipments, model);

		return NormalizeSolutionService.normalizeRoute(solution, model);
	}

	private Solution buildSolution(Map<Integer, Node[]> shipments, XFVRPModel model) {
		Node[] nodes = model.getNodes();
		PDPCheckService checkService = new PDPCheckService();
		
		if (nodes.length == 0) {
			return new Solution(model);
		}
		
		List<Node> gL = new ArrayList<>();
		int[] depotIdx = new int[]{0};
		int[] maxIdx = new int[]{0};

		shipments.values().forEach(shipment -> {
			if(!checkService.check(model, shipment)) {
				return;
			}

			// Create a trivial route with a depot, pickup and delivery.
			gL.add(Util.createIdNode(nodes[depotIdx[0]], maxIdx[0]++));
			gL.add(shipment[0]);
			gL.add(shipment[1]);

			depotIdx[0] = ((depotIdx[0] + 1) % model.getNbrOfDepots());
		});

		if (gL.size() > 0) 
			gL.add(Util.createIdNode(nodes[depotIdx[0]], maxIdx[0]++));

		Solution solution = new Solution(model);
		solution.setGiantRoute(gL.toArray(new Node[0]));
		return solution;
	}

	private Map<Integer, Node[]> getValidNodes(XFVRPModel model) {
		// Collect pairs of nodes of pickup and delivery (i.e. shipments)
		return Arrays.stream(model.getNodes())
				.collect(Collectors.groupingBy(Node::getShipmentIdx))
				.values()
				.stream()
				.filter(list -> list.size() == 2)
				.map(list -> {
					Node[] pair = new Node[2];

					pair[0] = (list.get(0).getDemand()[0] > 0) ? list.get(0) : list.get(1);
					pair[1] = (list.get(0).getDemand()[0] < 0) ? list.get(0) : list.get(1);

					return pair;
				})
				.collect(Collectors.toMap(k -> k[0].getShipmentIdx(), v -> v, (v1, v2) -> v1));

	}
}
