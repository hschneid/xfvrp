package xf.xfvrp.opt;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.quality.RouteQuality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class Solution implements Iterable<Node[]> {

	private final XFVRPModel model;

	private Node[][] routes = new Node[1][0];
	private RouteQuality[] routeQualities = new RouteQuality[] { new RouteQuality(0, null) };
	private Quality totalQuality = new Quality(null);

	private final List<RouteQuality> invalidatedRoutesQualities = new ArrayList<>();

	public Solution(XFVRPModel model) {
		this.model = model;
	}

	public XFVRPModel getModel() {
		return model;
	}

	public Node[][] getRoutes() {
		return routes;
	}

	public RouteQuality[] getRouteQualities() {
		return routeQualities;
	}

	public Quality getQuality() {
		return totalQuality;
	}

	public void deleteRoute(int routeIndex) {
		routes[routeIndex] = new Node[0];

		totalQuality.sub(routeQualities[routeIndex]);
		routeQualities[routeIndex] = new RouteQuality(0, null);
	}

	public void addRoute(Node[] newRoute) {
		// Replace empty slot with new route, if available
		for (int i = 0; i < routes.length; i++) {
			if(routes[i].length == 0) {
				routes[i] = newRoute;
				return;
			}
		}

		// Otherwise, enlarge routes and place new route at the end
		routes = Arrays.copyOf(routes, routes.length + 1);
		routes[routes.length - 1] = newRoute;

		routeQualities = Arrays.copyOf(routeQualities, routeQualities.length + 1);
		routeQualities[routeQualities.length - 1] = new RouteQuality(routeQualities.length - 1, null);
	}

	public void addRoutes(Node[][] newRoutes) {
		for (int i = 0; i < newRoutes.length; i++) {
			if(newRoutes[i] != null)
				addRoute(newRoutes[i]);
		}
	}

	public void setRoute(int routeIndex, Node[] route) {
		routes[routeIndex] = route;
	}

	/**
	 * Updates the quality of a certain route.
	 *
	 * This means, the sub the current quality and add the new one.
	 */
	public void setRouteQuality(int routeIndex, Quality quality) {
		totalQuality.sub(routeQualities[routeIndex]);

		routeQualities[routeIndex] = (RouteQuality) quality;
		totalQuality.add(quality);
	}

	/**
	 * This invalidates the quality of a certain route, so that
	 * it can be overwritten.
	 */
	public void invalidateRouteQuality(int routeIdx) {
		invalidatedRoutesQualities.add(new RouteQuality(
				routeIdx,
				routeQualities[routeIdx]
		));
	}

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

		for (int i = routes.length - 1; i >= 0; i--) {
			if(routes[i].length > 0) {
				giantRoute.add(routes[i][routes[i].length - 1]);
				break;
			}
		}

		return giantRoute.toArray(new Node[0]);
	}

	/**
	 * Converts a giant route into internal solution representation
	 * with multiple routes
	 */
	public void setGiantRoute(Node[] giantRoute) {
		if(giantRoute == null || giantRoute.length == 0) {
			routes = new Node[0][0];
			return;
		}

		List<List<Node>> list = new ArrayList<>();
		List<Node> currentRoute = new ArrayList<>();
		currentRoute.add(giantRoute[0]);
		Node lastDepot = giantRoute[0];
		for (int i = 1; i < giantRoute.length; i++) {
			Node currentNode = giantRoute[i];
			if(currentNode != null && currentNode.getSiteType() == SiteType.DEPOT) {
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
		routeQualities = new RouteQuality[list.size()];
		for (int i = 0; i < list.size(); i++) {
			routes[i] = list.get(i).toArray(new Node[0]);
			routeQualities[i] = new RouteQuality(i, null);
		}
		totalQuality = new Quality(null);
	}

	public Solution copy() {
		Solution solution = new Solution(this.model);

		Node[][] copyRoutes = new Node[routes.length][];
		for (int i = 0; i < routes.length; i++)
			copyRoutes[i] = Arrays.copyOf(routes[i], routes[i].length);
		solution.routes = copyRoutes;

		solution.routeQualities = new RouteQuality[routeQualities.length];
		for (int i = 0; i < routeQualities.length; i++)
			solution.routeQualities[i] = new RouteQuality(i, routeQualities[i]);
		solution.totalQuality = new Quality(totalQuality);

		return solution;
	}

	@Override
	public Iterator<Node[]> iterator() {
		return new SolutionRoutesIterator(routes);
	}

	/**
	 * Tells the solution, that the search is over and invalidated qualities can be purged.
	 */
	public void fixateQualities() {
		invalidatedRoutesQualities.clear();
	}

	/**
	 * Means, that the routes changed without reevaluation. The route qualities
	 * must be reset to the old value.
	 */
	public void resetQualities() {
		for (RouteQuality invalidatedQuality : invalidatedRoutesQualities) {
			setRouteQuality(invalidatedQuality.getRouteIdx(), invalidatedQuality);
		}
		invalidatedRoutesQualities.clear();
	}

	/**
	 * This will purge all routes over the number of routes to retain.
	 *
	 * This function is called to shrink the number or routes, which makes
	 * sense for the optimization. So for-loops will not test so often empty
	 * routes.
	 */
	public void retainRoutes(int nbrOfRoutesToRetain) {
		Node[][] newRoutes = new Node[nbrOfRoutesToRetain][];
		RouteQuality[] newRouteQualities = new RouteQuality[nbrOfRoutesToRetain];
		System.arraycopy(routes, 0, newRoutes, 0, nbrOfRoutesToRetain);
		System.arraycopy(routeQualities, 0, newRouteQualities, 0, nbrOfRoutesToRetain);

		this.routes = newRoutes;
		this.routeQualities = newRouteQualities;
	}

	public boolean isValid() {
		return routes.length > 0 && routes[0].length > 0;
	}
}
