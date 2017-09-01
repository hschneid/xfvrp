package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;

public class ActiveNodeAnalyzer {

	
	/**
	 * Searches in the giant route for nodes which can be ignored during
	 * evalution. This can be the case for empty routes or unnecessary
	 * replenishments.
	 * 
	 * @param giantRoute
	 * @return list of active (true) or disabled (false) nodes in giant route
	 */
	public static boolean[] getActiveNodes(Node[] giantRoute) {
		boolean[] activeNodes = new boolean[giantRoute.length];
		if(giantRoute.length == 0)
			return activeNodes;

		int lastNodeIdx = 0;
		Node lastNode = giantRoute[lastNodeIdx];
		activeNodes[0] = true;
		for (int i = 1; i < activeNodes.length; i++) {
			activeNodes[i] = true;

			Node currNode = giantRoute[i];

			if(currNode.getSiteType() == SiteType.DEPOT && 
					lastNode.getSiteType() == SiteType.DEPOT)
				activeNodes[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.REPLENISH &&
					lastNode.getSiteType() == SiteType.REPLENISH)
				activeNodes[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.DEPOT &&
					lastNode.getSiteType() == SiteType.REPLENISH)
				activeNodes[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.REPLENISH &&
					lastNode.getSiteType() == SiteType.DEPOT)
				activeNodes[i] = false;

			if(activeNodes[i]) {
				lastNode = currNode;
				lastNodeIdx = i;
			}
		}

		return activeNodes;
	}
}
