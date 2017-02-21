package xf.xfvrp.base;

import java.util.ArrayList;
import java.util.List;

import xf.xfvrp.opt.Solution;


/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Utility class holds indifferent methods for processing on
 * the giant tour array. All methods are static.
 * 
 * @author hschneid
 *
 */
public class Util {

	/**
	 * Normalizes the giant tour by applying the following methods
	 * - Removing empty routes
	 * - Adding empty routes for each available depot (multi depot) 
	 * 
	 * @param giantRoute node sequence with unknown number of empty routes
	 * @return node sequence with one empty routes for each depot site.
	 */
	public static Node[] normalizeRouteWithoutDepotId(Node[] giantTour, XFVRPModel model) {
		Node[] nodeArr = model.getNodeArr();
		final int nbrOfDepots = model.getNbrOfDepots();

		List<Node> list = new ArrayList<>();

		int maxDepotId = 0;

		// Remove empty routes and reindex depot nodes (depotId)
		for (int i = 0; i < giantTour.length - 1; i++) {
			final SiteType currType = giantTour[i].getSiteType();
			final SiteType nextType = giantTour[i+1].getSiteType();

			if(currType == SiteType.DEPOT && nextType == SiteType.DEPOT) 
				continue;

			if(currType == SiteType.REPLENISH && nextType == SiteType.DEPOT)
				continue;

			list.add(giantTour[i]);

			// If next node is replenish node and current node depot or replenish node,
			// then ignore the next node.
			if(currType == SiteType.DEPOT && nextType == SiteType.REPLENISH) 
				i++;
			else if(currType == SiteType.REPLENISH && nextType == SiteType.REPLENISH) 
				i++;
		}

		// Bei leeren Touren, schließe direkt ab.
		if(giantTour.length != 0) {
			list.add(giantTour[giantTour.length - 1]);
		}

		// Erzeuge für jedes Depot eine leere Tour
		for (int i = 0; i < nbrOfDepots; i++)
			list.add(Util.createIdNode(nodeArr[i], maxDepotId++));

		// Create additional empty route with all replenish nodes on it
		if(model.getNbrOfReplenish() > 0) {
			for (int i = nbrOfDepots; i < nbrOfDepots + model.getNbrOfReplenish(); i++)
				list.add(nodeArr[i].copy());
			list.add(Util.createIdNode(nodeArr[0], maxDepotId++));
		}

		return list.toArray(new Node[list.size()]);
	}
	
	/**
	 * Normalizes the giant tour by applying the following methods
	 * - Removing empty routes
	 * - Adding empty routes for each available depot (multi depot) 
	 * - Reindex the depotIDs which are used for build a solution report
	 * 
	 * @param giantRoute node sequence with unknown number of empty routes
	 * @return node sequence with one empty routes for each depot site.
	 */
	public static Solution normalizeRoute(Solution solution, XFVRPModel model) {
		Node[] nodeArr = model.getNodeArr();
		final int nbrOfDepots = model.getNbrOfDepots();

		List<Node> list = new ArrayList<>();

		int maxDepotId = 0;

		// Remove empty routes and reindex depot nodes (depotId)
		Node[] giantTour = solution.getGiantRoute();
		for (int i = 0; i < giantTour.length - 1; i++) {
			final SiteType currType = giantTour[i].getSiteType();
			final SiteType nextType = giantTour[i+1].getSiteType();

			if(currType == SiteType.DEPOT && nextType == SiteType.DEPOT) 
				continue;

			if(currType == SiteType.DEPOT)
				giantTour[i].setDepotId(maxDepotId++);

			if(currType == SiteType.REPLENISH && nextType == SiteType.DEPOT)
				continue;

			list.add(giantTour[i]);

			// If next node is replenish node and current node depot or replenish node,
			// then ignore the next node.
			if(currType == SiteType.DEPOT && nextType == SiteType.REPLENISH) 
				i++;
			else if(currType == SiteType.REPLENISH && nextType == SiteType.REPLENISH) 
				i++;
		}

		// Bei leeren Touren, schließe direkt ab.
		if(giantTour.length != 0) {
			giantTour[giantTour.length - 1].setDepotId(maxDepotId++);
			list.add(giantTour[giantTour.length - 1]);
		}

		// Erzeuge für jedes Depot eine leere Tour
		for (int i = 0; i < nbrOfDepots; i++)
			list.add(Util.createIdNode(nodeArr[i], maxDepotId++));

		// Create additional empty route with all replenish nodes on it
		if(model.getNbrOfReplenish() > 0) {
			for (int i = nbrOfDepots; i < nbrOfDepots + model.getNbrOfReplenish(); i++)
				list.add(nodeArr[i].copy());
			list.add(Util.createIdNode(nodeArr[0], maxDepotId++));
		}

		Solution newSolution = new Solution();
		newSolution.setGiantRoute(list.toArray(new Node[list.size()]));
		return newSolution;
	}

	/**
	 * @param giantTour
	 * @return The sum of all track lengths in the giant tour
	 */
	public static float getDistSum(Node[] giantTour, XFVRPModel model) {
		float distSum = 0;
		Node lastN = null;
		for (Node n : giantTour) {
			if(lastN == null) {
				lastN = n;
				continue;
			}

			distSum += model.getDistance(lastN, n);

			lastN = n;
		}

		return distSum;
	}

	/**
	 * Counts the number of tours in a given giant tour.
	 * 
	 * @param tour
	 * @return a positive value including 0
	 */
	public static int getTourCount(Node[] tour) {
		int count = 0;
		for (int i = 1; i < tour.length; i++) {
			if(tour[i-1].getSiteType() == SiteType.DEPOT && tour[i].getSiteType() == SiteType.CUSTOMER)
				count++;
		}
		return count;
	}

	/**
	 * In single depot problems a giant tour contains only one depot multiple times. But each
	 * version of the depot in giant tour is a deep copy with a unique depotId.
	 * 
	 * This method creates the deep copy instance and assigns the depotId.
	 * 
	 * @param depot Current depot
	 * @param newId Last assigned depotId
	 * @return The copy of depot object
	 */
	public static Node createIdNode(Node depot, int newId) {
		Node n = depot.copy();
		n.setDepotId(newId);
		return n;
	}

	/**
	 * 
	 * @param giantRoute
	 */
	public static void checkDepotID(Node[] giantRoute) {
		int[] depotIdCnt = new int[giantRoute.length];
		for (int i = 0; i < giantRoute.length; i++) {
			if(giantRoute[i].getSiteType() == SiteType.DEPOT) {
				if(depotIdCnt[giantRoute[i].getDepotId()] >= 1)
					System.out.println("XX "+giantRoute[i].getDepotId());
				depotIdCnt[giantRoute[i].getDepotId()]++;
			}
		}
	}
}
