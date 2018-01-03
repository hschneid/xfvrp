package xf.xfvrp.base;

import java.util.ArrayList;
import java.util.List;

import xf.xfvrp.opt.Solution;

public class NormalizeSolutionService {

	/**
	 * Normalizes the giant tour by applying the following methods
	 * - Removing empty routes
	 * - Adding empty routes for each available depot (multi depot) 
	 * - Reindex the depotIDs which are used for build a solution report
	 */
	public static Solution normalizeRoute(Solution solution, XFVRPModel model) {
		int maxDepotId = 0;

		maxDepotId = removeEmptyRoutes(solution, maxDepotId);

		maxDepotId = addEmptyRoutes(solution, model, maxDepotId);

		maxDepotId = addReplenishRoute(solution, model, maxDepotId);

		return solution;
	}

	private static int removeEmptyRoutes(Solution solution, int maxDepotId) {
		for (int routeIndex = 0; routeIndex < solution.getRoutes().length; routeIndex++) {
			Node[] route = solution.getRoutes()[routeIndex];

			if(route.length == 0) {
				solution.deleteRoute(routeIndex);
				routeIndex--;
				continue;
			}
			
			removeEmptyReplenishments(routeIndex, solution);
		}

		return maxDepotId;
	}

	private static void removeEmptyReplenishments(int routeIndex, Solution solution) {
		Node[] route = solution.getRoutes()[routeIndex];
		
		// Remove empty routes and reindex depot nodes (depotId)
		List<Node> cleanedRoute = new ArrayList<>();
		for (int i = 0; i < route.length - 1; i++) {
			final SiteType currType = route[i].getSiteType();
			final SiteType nextType = route[i+1].getSiteType();

			if(currType == SiteType.REPLENISH && nextType == SiteType.REPLENISH) 
				i++;
			
			cleanedRoute.add(route[i]);
		}
		
		if(cleanedRoute.size() != route.length) {
			solution.setRoute(routeIndex, cleanedRoute.toArray(new Node[0]));
		}
	}

	private static int addEmptyRoutes(Solution solution, XFVRPModel model, int maxDepotId) {
		Node[] nodes = model.getNodes();
		int nbrOfDepots = model.getNbrOfDepots();

		// Erzeuge für jedes Depot eine leere Tour
		for (int i = 0; i < nbrOfDepots; i++) {
			Node depot = Util.createIdNode(nodes[i], maxDepotId++);
			solution.addRoute(new Node[]{depot, depot});
		}

		return maxDepotId;
	}

	private static int addReplenishRoute(Solution solution, XFVRPModel model, int maxDepotId) {
		Node[] nodes = model.getNodes();
		int idxOfFirstReplenishInNodes = model.getNbrOfDepots();
		int idxOfLastReplenishInNodes = model.getNbrOfReplenish() + idxOfFirstReplenishInNodes;

		if(model.getNbrOfReplenish() == 0) {
			return maxDepotId;
		}

		// Create additional empty route with all replenish nodes on it
		Node[] newRoute = new Node[model.getNbrOfReplenish() + 2];
		Node depot = Util.createIdNode(nodes[0], maxDepotId++);
		int idx = 0;
		
		newRoute[idx++] = depot;
		for (int i = idxOfFirstReplenishInNodes; i < idxOfLastReplenishInNodes; i++) {
			newRoute[idx++] = nodes[i].copy();
		}
		newRoute[idx++] = depot;
		
		solution.addRoute(newRoute);

		return maxDepotId;
	}

}
