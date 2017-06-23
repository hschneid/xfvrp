package xf.xfvrp.base;

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
