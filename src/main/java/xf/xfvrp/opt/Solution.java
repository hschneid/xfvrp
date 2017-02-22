package xf.xfvrp.opt;

import java.util.Arrays;

import xf.xfvrp.base.Node;

public class Solution {

	private Node[] giantRoute;

	/**
	 * @return the giantRoute
	 */
	public Node[] getGiantRoute() {
		return giantRoute;
	}

	/**
	 * @param giantRoute the giantRoute to set
	 */
	public void setGiantRoute(Node[] giantRoute) {
		this.giantRoute = giantRoute;
	}
	
	public Solution copy() {
		Solution solution = new Solution();
		solution.setGiantRoute(Arrays.copyOf(giantRoute, giantRoute.length));
		
		return solution;
	}
	
}
