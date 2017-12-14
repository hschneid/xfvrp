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
		List<Node> list = new ArrayList<>();
		Node[] giantTour = solution.getGiantRoute();

		int maxDepotId = 0;

		maxDepotId = removeEmptyRoutes(list, giantTour, maxDepotId);

		maxDepotId = addEmptyRoutes(list, model, maxDepotId);

		maxDepotId = addReplenishRoute(list, model, maxDepotId);

		Solution newSolution = new Solution();
		newSolution.setGiantRoute(list.toArray(new Node[list.size()]));
		return newSolution;
	}

	private static int removeEmptyRoutes(List<Node> list, Node[] giantRoute, int maxDepotId) {
		if(giantRoute.length == 0)
			return maxDepotId;

		// Remove empty routes and reindex depot nodes (depotId)
		for (int i = 0; i < giantRoute.length - 1; i++) {
			final SiteType currType = giantRoute[i].getSiteType();
			final SiteType nextType = giantRoute[i+1].getSiteType();

			if(currType == SiteType.DEPOT && nextType == SiteType.DEPOT) 
				continue;

			if(currType == SiteType.DEPOT)
				giantRoute[i].setDepotId(maxDepotId++);

			if(currType == SiteType.REPLENISH && nextType == SiteType.DEPOT)
				continue;

			list.add(giantRoute[i]);

			// If next node is replenish node and current node depot or replenish node,
			// then ignore the next node.
			if(currType == SiteType.DEPOT && nextType == SiteType.REPLENISH) 
				i++;
			else if(currType == SiteType.REPLENISH && nextType == SiteType.REPLENISH) 
				i++;
		}

		// Bei leeren Touren, schließe direkt ab.
		giantRoute[giantRoute.length - 1].setDepotId(maxDepotId++);
		list.add(giantRoute[giantRoute.length - 1]);

		return maxDepotId;
	}

	private static int addEmptyRoutes(List<Node> list, XFVRPModel model, int maxDepotId) {
		Node[] nodes = model.getNodes();
		int nbrOfDepots = model.getNbrOfDepots();

		// Remove last depot in list.
		list.remove(list.size() - 1);

		// Erzeuge für jedes Depot eine leere Tour
		for (int i = 0; i < nbrOfDepots; i++)
			list.add(Util.createIdNode(nodes[i], maxDepotId++));

		// Duplicate last added depot to realize the empty route
		list.add(Util.createIdNode(nodes[nbrOfDepots - 1], maxDepotId++));

		return maxDepotId;
	}

	private static int addReplenishRoute(List<Node> list, XFVRPModel model, int maxDepotId) {
		Node[] nodes = model.getNodes();
		int idxOfFirstReplenishInNodes = model.getNbrOfDepots();
		int idxOfLastReplenishInNodes = model.getNbrOfReplenish() + idxOfFirstReplenishInNodes;

		if(model.getNbrOfReplenish() == 0) {
			return maxDepotId;
		}

		// Create additional empty route with all replenish nodes on it
		for (int i = idxOfFirstReplenishInNodes; i < idxOfLastReplenishInNodes; i++)
			list.add(nodes[i].copy());

		list.add(Util.createIdNode(nodes[0], maxDepotId++));

		return maxDepotId;
	}

}
