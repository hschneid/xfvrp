package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.Node;

import static xf.xfvrp.base.SiteType.DEPOT;
import static xf.xfvrp.base.SiteType.REPLENISH;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class ActiveNodeAnalyzer {

	/**
	 * Searches in the giant route for nodes which can be ignored during
	 * evalution. This can be the case for empty routes or unnecessary
	 * replenishments.
	 *
	 * @return list of active (true) or disabled (false) nodes in giant route
	 */
	public static Node[] getActiveNodes(Node[] route) {
		boolean[] activeFlags = new boolean[route.length];
		if(route.length == 0)
			return new Node[0];
		if(route.length == 2 && route[0].getSiteType() == DEPOT && route[1].getSiteType() == DEPOT)
			return route;

		int lastNodeIdx = 0;
		Node lastNode = route[lastNodeIdx];
		activeFlags[0] = true;
		int nbrOfInactiveNodes = 0;
		for (int i = 1; i < activeFlags.length; i++) {
			activeFlags[i] = true;

			Node currNode = route[i];

			if(currNode.getSiteType() == DEPOT &&
					lastNode.getSiteType() == DEPOT) {
				activeFlags[lastNodeIdx] = false; nbrOfInactiveNodes++;
			} else if(currNode.getSiteType() == REPLENISH &&
					lastNode.getSiteType() == REPLENISH) {
				activeFlags[lastNodeIdx] = false; nbrOfInactiveNodes++;
			} else if(currNode.getSiteType() == DEPOT &&
					lastNode.getSiteType() == REPLENISH) {
				activeFlags[lastNodeIdx] = false; nbrOfInactiveNodes++;
			} else if(currNode.getSiteType() == REPLENISH &&
					lastNode.getSiteType() == DEPOT) {
				activeFlags[i] = false; nbrOfInactiveNodes++;
			}

			if(activeFlags[i]) {
				lastNode = currNode;
				lastNodeIdx = i;
			}
		}
		// Start depot is always active
		if(!activeFlags[0]) nbrOfInactiveNodes--;
		activeFlags[0] = true;


		return extract(route, activeFlags, nbrOfInactiveNodes);
	}

	private static Node[] extract(Node[] route, boolean[] activeFlags, int nbrOfInactiveNodes) {
		Node[] activeRoute = new Node[route.length - nbrOfInactiveNodes];

		int activeRouteIdx = 0;
		for (int oldRouteIdx = 0; oldRouteIdx < route.length; oldRouteIdx++) {
			if(activeFlags[oldRouteIdx])
				activeRoute[activeRouteIdx++] = route[oldRouteIdx];
		}

		return activeRoute;
	}
}
