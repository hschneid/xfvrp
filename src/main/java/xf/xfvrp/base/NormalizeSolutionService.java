package xf.xfvrp.base;

import xf.xfvrp.base.quality.RouteQuality;
import xf.xfvrp.opt.Solution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class NormalizeSolutionService {

	/**
	 * Normalizes the giant tour by applying the following methods
	 * - Removing empty routes
	 * - Adding empty routes for each available depot (multi depot) 
	 * - Reindex the depotIDs which are used for build a solution report
	 */
	public static Solution normalizeRoute(Solution solution) {
		removeEmptyRoutes(solution);

		int maxDepotId = 0;
		maxDepotId = addEmptyRoutes(solution, maxDepotId);

		addReplenishRoute(solution, maxDepotId);

		updateOverhangingRoutes(solution);

		return solution;
	}

	/**
	 * Normalizing a solution with additional effort to get rid of obsolete entries
	 * - Really remove empty routes
	 * - Then call normalizing
	 */
	public static void normalizeRouteWithCleanup(Solution solution) {
		shrinkRoutes(solution);
		normalizeRoute(solution);
	}

	/**
	 * The number of open routes will be shrinked to only full routes.
	 * After this method there are no empty routes in the solution.
	 * It is done by copying routes to the begin of array and cut off the rest.
	 */
	private static void shrinkRoutes(Solution solution) {
		Node[][] routes = solution.getRoutes();
		int lastNonEmptyRoute = 0;
		for (int i = 0; i < routes.length; i++) {
			if(routes[i].length <= 2) {
				// Current route is empty, search for next non-empty route
				int nextNonEmptyRoute = getNonEmptyRoute(routes, i);
				if(nextNonEmptyRoute != -1) {
					// Swap nextNonEmptyRoute to currentEmptyRoute
					Node[] nonEmptyRoute = routes[nextNonEmptyRoute];
					solution.setRoute(nextNonEmptyRoute, routes[i]);
					solution.setRoute(i, nonEmptyRoute);

					Quality nonEmptyQuality = solution.getRouteQualities()[nextNonEmptyRoute];
					solution.setRouteQuality(nextNonEmptyRoute, solution.getRouteQualities()[i]);
					solution.setRouteQuality(i, new RouteQuality(i, nonEmptyQuality));

				} else {
					// No more non empty routes available
					break;
				}
			}
			lastNonEmptyRoute++;
		}

		// Purge all routes after lastNonEmptyRoute
		solution.retainRoutes(lastNonEmptyRoute);
	}

	private static int getNonEmptyRoute(Node[][] routes, int i) {
		for (int j = i + 1; j < routes.length; j++) {
			if(routes[j].length > 2) {
				return j;
			}
		}
		return -1;
	}

	private static void removeEmptyRoutes(Solution solution) {
		for (int routeIndex = 0; routeIndex < solution.getRoutes().length; routeIndex++) {
			Node[] route = solution.getRoutes()[routeIndex];

			if(route.length == 0)
				continue;

			// If route contains no customers, remove route
			if(isEmptyRoute(route)) {
				solution.deleteRoute(routeIndex);
				continue;
			}

			removeObsoleteReplenishments(routeIndex, solution);
		}
	}

	private static void removeObsoleteReplenishments(int routeIndex, Solution solution) {
		Node[] route = solution.getRoutes()[routeIndex];

		// Remove empty routes
		List<Node> cleanedRoute = new ArrayList<>();
		for (int i = 0; i < route.length - 1; i++) {
			final SiteType currType = route[i].getSiteType();
			final SiteType nextType = route[i+1].getSiteType();

			if(currType == SiteType.REPLENISH && nextType == SiteType.REPLENISH)
				i++;

			cleanedRoute.add(route[i]);
		}
		cleanedRoute.add(route[route.length - 1]);

		if(cleanedRoute.size() != route.length) {
			solution.setRoute(routeIndex, cleanedRoute.toArray(new Node[0]));
		}
	}

	private static int addEmptyRoutes(Solution solution, int maxDepotId) {
		Node[] nodes = solution.getModel().getNodes();
		int nbrOfDepots = solution.getModel().getNbrOfDepots();

		// Erzeuge für jedes Depot eine leere Tour
		for (int i = 0; i < nbrOfDepots; i++) {
			Node depot = Util.createIdNode(nodes[i], maxDepotId++);
			solution.addRoute(new Node[]{depot, depot});
		}

		return maxDepotId;
	}

	private static void addReplenishRoute(Solution solution, int maxDepotId) {
		XFVRPModel model = solution.getModel();
		Node[] nodes = model.getNodes();
		int idxOfFirstReplenishInNodes = model.getNbrOfDepots();
		int idxOfLastReplenishInNodes = model.getNbrOfReplenish() + idxOfFirstReplenishInNodes;

		if(model.getNbrOfReplenish() == 0) {
			return;
		}

		// Create additional empty route with all replenish nodes on it
		Node[] newRoute = new Node[model.getNbrOfReplenish() + 2];
		Node depot = Util.createIdNode(nodes[0], maxDepotId);
		int idx = 0;

		newRoute[idx++] = depot;
		for (int i = idxOfFirstReplenishInNodes; i < idxOfLastReplenishInNodes; i++) {
			newRoute[idx++] = nodes[i].copy();
		}
		newRoute[idx] = depot;

		solution.addRoute(newRoute);
	}

	private static void updateOverhangingRoutes(Solution solution) {
		int[] nbrOfRoutes = Arrays.copyOf(solution.getNbrRoutesOfDepot(), solution.getNbrRoutesOfDepot().length);
		boolean[] isOverhang = solution.getOverhangRoutes();
		Node[][] routes = solution.getRoutes();

		for (int i = routes.length - 1; i >= 0; i--) {
			// If route is not empty AND the number of routes is greater than allowed -> overhang!
			if(routes[i] != null && routes[i].length > 0 &&
					nbrOfRoutes[routes[i][0].getIdx()] > routes[i][0].getMaxNbrOfRoutes()) {
				isOverhang[i] = true;
				nbrOfRoutes[routes[i][0].getIdx()]--;
			} else {
				isOverhang[i] = false;
			}
		}
	}

	private static boolean isEmptyRoute(Node[] route) {
		if(route.length <= 2)
			return true;

		if(route[1].getSiteType() == SiteType.CUSTOMER ||
				route[route.length - 2].getSiteType() == SiteType.CUSTOMER
		)
			return false;

		for (Node node : route) {
			if (node.getSiteType() == SiteType.CUSTOMER)
				return false;
		}

		return true;
	}

}
