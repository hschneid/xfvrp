package xf.xfvrp.opt.improve;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.opt.Solution;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Contains the optimization algorithms
 * for 2-opt. In a node view one node can
 * be moved to any other position in the 
 * route plan.
 * 
 * In single depot problem only customer nodes
 * are relocated. In multiple depot problem customer
 * nodes and depots may be moved.
 * 
 * @author hschneid
 *
 */
public class XFVRPRelocate3 extends XFVRPOptImpBase {
	
	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) {
		final Set<String> loadingFootprint = getLoadingFootprint(solution);

		Node[] giantTour = solution.getGiantRoute();
		List<float[]> improvingStepList = new ArrayList<>();

		if(model.getNbrOfDepots() == 1)
			searchSingleDepot(giantTour, improvingStepList);
		else
			searchMultiDepot(giantTour, improvingStepList);

		// Sortier absteigend nach Potenzial
		sort(improvingStepList, 2);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			int src = (int) val[0];
			int dst = (int) val[1];

			move(solution, src, dst);

			Quality result = check(solution, loadingFootprint);
			if(result != null && result.getFitness() < bestResult.getFitness())
				return result;

			if(src > dst)
				move(solution, dst, src + 1);
			else
				move(solution, dst - 1, src);
		}

		return null;
	}

	/**
	 * Searches all improving valid steps in search space for
	 * a VRP with one depot.
	 * 
	 * @param route
	 * @param improvingStepList
	 */
	private void searchSingleDepot(Node[] route, List<float[]> improvingStepList) {
		// Split giant route into single route informations
		List<int[]> routeList = new ArrayList<>();
		{
			int[] arr = new int[] {0,-1};
			for (int i = 1; i < route.length; i++) 
				if(route[i].getSiteType() == SiteType.DEPOT) {
					arr[1] = i;
					routeList.add(arr);
					arr = new int[] {i,-1};
				}
		}

		for (int i = 0; i < routeList.size(); i++) {
			// Starting Depot position
			int startPos = routeList.get(i)[0];
			// Ending Depot position
			int endPos = routeList.get(i)[1];

			// Only positions in this route can be destination
			int pos = 1;
			for (int dst = startPos + 1; dst <= endPos; dst++) {

				// All Customers can be source
				for (int src = 1; src < route.length - 1; src++) {
					// Source node must not be a depot (too big change)
					if(route[src].getSiteType() == SiteType.DEPOT)
						continue;
					
					Node srcN = route[src];

					Node[] smallRoute = getSmallRoute(route, startPos, endPos, srcN, pos);

					// Skip if next node is src or zero move
					if(src == dst)
						dst++;
					if(dst - src == 1)
						if(dst < endPos - 1)
							dst++;
						else
							continue;

					// Check constraints for inserting SRC at pos DST
					smallRoute[pos] = srcN;
					
					Solution smallSolution = new Solution();
					smallSolution.setGiantRoute(smallRoute);
					
					if(check(smallSolution).getPenalty() > 0)
						continue;

					float val = 0;
					// Einen nach vorne
					if(src - dst == 1) {
						// Berechne Entfernungen f�r IST
						val += getDistanceForOptimization(route[src], route[src+1]);
						val += getDistanceForOptimization(route[dst], route[src]);
						val += getDistanceForOptimization(route[dst-1], route[dst]);
						// Berechne Entfernungen f�r SOLL
						val -= getDistanceForOptimization(route[dst], route[src+1]);
						val -= getDistanceForOptimization(route[src], route[dst]);
						val -= getDistanceForOptimization(route[dst-1], route[src]);
					}
					else {
						// Berechne Entfernungen f�r IST
						val += getDistanceForOptimization(route[src-1], route[src]);
						val += getDistanceForOptimization(route[src], route[src+1]);
						val += getDistanceForOptimization(route[dst-1], route[dst]);
						// Berechne Entfernungen f�r SOLL
						val -= getDistanceForOptimization(route[src-1], route[src+1]);
						val -= getDistanceForOptimization(route[src], route[dst]);
						val -= getDistanceForOptimization(route[dst-1], route[src]);
					}

					if(val > epsilon) {
						improvingStepList.add(new float[]{src, dst, val});
					}
				}
				
				pos++;
			}
		}
	}

	/**
	 * 
	 * @param route
	 * @param startPos
	 * @param endPos
	 * @param srcN
	 * @return
	 */
	private Node[] getSmallRoute(Node[] route, int startPos, int endPos, Node srcN, int freePos) {
		List<Node> list = new ArrayList<>();
		for (int i = startPos; i <= endPos; i++) {
			if(route[i] != srcN) {
				list.add(route[i]);
			}
		}
		list.add(freePos, null);
		return list.toArray(new Node[0]);
	}

	/**
	 * Searches all improving valid steps in search space for
	 * a VRP with multiple depots.
	 * 
	 * @param giantRoute
	 * @param improvingStepList
	 */
	private void searchMultiDepot(Node[] giantTour, List<float[]> improvingStepList) {
		int[] depotMarkArr = new int[giantTour.length];
		int lastDepotIdx = giantTour[0].getIdx();
		for (int i = 0; i < depotMarkArr.length; i++) {
			if(giantTour[i].getSiteType() == SiteType.DEPOT)
				lastDepotIdx = giantTour[i].getIdx();
			depotMarkArr[i] = lastDepotIdx;
		}

		// Suche alle verbessernden L�sungen
		for (int src = 1; src < giantTour.length - 1; src++) {
			int markA = depotMarkArr[src];

			// Start darf kein Depot sein
			if(giantTour[src].getSiteType() == SiteType.DEPOT)
				continue;

			for (int dst = 1; dst < giantTour.length - 1; dst++) {
				if(src == dst)
					continue;

				if(dst - src == 1)
					continue;

				int markB = depotMarkArr[dst-1];

				float val = 0;
				// Einen nach vorne
				if(src - dst == 1) {
					// Berechne Entfernungen f�r IST
					val += getDistance(giantTour[src], giantTour[src+1], markA);
					val += getDistance(giantTour[dst], giantTour[src], markA);
					val += getDistance(giantTour[dst-1], giantTour[dst], markB);
					// Berechne Entfernungen f�r SOLL
					val -= getDistance(giantTour[dst], giantTour[src+1], markA);
					val -= getDistance(giantTour[src], giantTour[dst], markB);
					val -= getDistance(giantTour[dst-1], giantTour[src], markB);
				}
				else {
					// Berechne Entfernungen f�r IST
					val += getDistance(giantTour[src-1], giantTour[src], markA);
					val += getDistance(giantTour[src], giantTour[src+1], markA);
					val += getDistance(giantTour[dst-1], giantTour[dst], markB);
					// Berechne Entfernungen f�r SOLL
					val -= getDistance(giantTour[src-1], giantTour[src+1], markA);
					val -= getDistance(giantTour[dst-1], giantTour[src], markB);
					val -= getDistance(giantTour[src], giantTour[dst], markB);
				}

				if(val > epsilon)
					improvingStepList.add(new float[]{src, dst, val});
			}
		}
	}
}
