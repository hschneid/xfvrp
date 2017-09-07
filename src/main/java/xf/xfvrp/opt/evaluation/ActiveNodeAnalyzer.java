package xf.xfvrp.opt.evaluation;

import java.util.ArrayList;
import java.util.List;

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
	public static List<Node> getActiveNodes(Node[] giantRoute) {
		boolean[] activeFlags = new boolean[giantRoute.length];
		if(giantRoute.length == 0)
			return new ArrayList<>();

		int lastNodeIdx = 0;
		Node lastNode = giantRoute[lastNodeIdx];
		activeFlags[0] = true;
		for (int i = 1; i < activeFlags.length; i++) {
			activeFlags[i] = true;

			Node currNode = giantRoute[i];

			if(currNode.getSiteType() == SiteType.DEPOT && 
					lastNode.getSiteType() == SiteType.DEPOT)
				activeFlags[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.REPLENISH &&
					lastNode.getSiteType() == SiteType.REPLENISH)
				activeFlags[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.DEPOT &&
					lastNode.getSiteType() == SiteType.REPLENISH)
				activeFlags[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.REPLENISH &&
					lastNode.getSiteType() == SiteType.DEPOT)
				activeFlags[i] = false;

			if(activeFlags[i]) {
				lastNode = currNode;
				lastNodeIdx = i;
			}
		}

		List<Node> activeNodes = new ArrayList<>(); 
		for (int i = 0; i < giantRoute.length; i++) {
			if(activeFlags[i])
				activeNodes.add(giantRoute[i]);
		}
		
		return activeNodes;
	}
}
