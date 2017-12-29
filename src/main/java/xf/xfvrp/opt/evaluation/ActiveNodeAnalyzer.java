package xf.xfvrp.opt.evaluation;

import static xf.xfvrp.base.SiteType.DEPOT;
import static xf.xfvrp.base.SiteType.REPLENISH;

import java.util.ArrayList;
import java.util.List;

import xf.xfvrp.base.Node;

public class ActiveNodeAnalyzer {


	/**
	 * Searches in the giant route for nodes which can be ignored during
	 * evalution. This can be the case for empty routes or unnecessary
	 * replenishments.
	 * 
	 * @param route
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
		for (int i = 1; i < activeFlags.length; i++) {
			activeFlags[i] = true;

			Node currNode = route[i];

			if(currNode.getSiteType() == DEPOT && 
					lastNode.getSiteType() == DEPOT)
				activeFlags[lastNodeIdx] = false;
			else if(currNode.getSiteType() == REPLENISH &&
					lastNode.getSiteType() == REPLENISH)
				activeFlags[lastNodeIdx] = false;
			else if(currNode.getSiteType() == DEPOT &&
					lastNode.getSiteType() == REPLENISH)
				activeFlags[lastNodeIdx] = false;
			else if(currNode.getSiteType() == REPLENISH &&
					lastNode.getSiteType() == DEPOT)
				activeFlags[i] = false;

			if(activeFlags[i]) {
				lastNode = currNode;
				lastNodeIdx = i;
			}
		}

		return extract(route, activeFlags);
	}

	private static Node[] extract(Node[] route, boolean[] activeFlags) {
		List<Node> activeNodes = new ArrayList<>(); 
		for (int i = 0; i < route.length; i++) {
			if(activeFlags[i])
				activeNodes.add(route[i]);
		}
		
		return activeNodes.toArray(new Node[0]);
	}
}
