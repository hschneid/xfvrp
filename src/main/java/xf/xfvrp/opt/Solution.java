package xf.xfvrp.opt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;

public class Solution {

	private Node[][] routes;

	/**
	 * @return the giantRoute
	 */
	public Node[] getGiantRoute() {
		if(routes.length == 0)
			return new Node[0];
		
		List<Node> giantRoute = new ArrayList<>();
		for (Node[] route : routes) {
			for (int i = 0; i < route.length - 1; i++) {
				giantRoute.add(route[i]);
			}
		}
		
		Node[] lastRoute = routes[routes.length - 1];
		giantRoute.add(lastRoute[lastRoute.length - 1]);
		
		return giantRoute.toArray(new Node[0]);
	}

	/**
	 * @param giantRoute the giantRoute to set
	 */
	public void setGiantRoute(Node[] giantRoute) {
		if(giantRoute.length == 0) {
			routes = new Node[0][0];
			return;
		}
		
		List<List<Node>> list = new ArrayList<>();
		List<Node> currentRoute = new ArrayList<>();
		currentRoute.add(giantRoute[0]);
		Node lastDepot = giantRoute[0];
		for (int i = 1; i < giantRoute.length; i++) {
			Node currentNode = giantRoute[i];
			if(currentNode.getSiteType() == SiteType.DEPOT) {
				currentRoute.add(lastDepot);
				list.add(currentRoute);
				currentRoute = new ArrayList<>();
				
				if(i < giantRoute.length - 1)
					currentRoute.add(currentNode);
				
				lastDepot = currentNode;
			} else {
				currentRoute.add(currentNode);
			}
		}
		if(currentRoute.size() > 0)
			list.add(currentRoute);
		
		routes = new Node[list.size()][];
		for (int i = 0; i < list.size(); i++)
			routes[i] = list.get(i).toArray(new Node[0]);
	}

	/**
	 * 
	 * @return
	 */
	public Solution copy() {
		Solution solution = new Solution();
		Node[][] copyRoutes = new Node[routes.length][];
		for (int i = 0; i < routes.length; i++)
			copyRoutes[i] = Arrays.copyOf(routes[i], routes[i].length);		
		solution.routes = copyRoutes;
		
		return solution;
	}
	
}
