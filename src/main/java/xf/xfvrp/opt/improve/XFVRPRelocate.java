package xf.xfvrp.opt.improve;

import java.util.ArrayList;
import java.util.List;

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
public class XFVRPRelocate extends XFVRPOptImpBase {

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) {
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

			Quality result = checkIt(solution);
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
	 * @param giantRoute
	 * @param improvingStepList
	 */
	private void searchSingleDepot(Node[] giantTour, List<float[]> improvingStepList) {
		// Suche alle verbessernden L�sungen
		for (int src = 1; src < giantTour.length - 1; src++) {
			for (int dst = 1; dst < giantTour.length; dst++) {
				// Start darf kein Depot sein
				if(giantTour[src].getSiteType() == SiteType.DEPOT)
					continue;
				if(src == dst)
					continue;

				if(dst - src == 1)
					continue;

				float val = 0;
				// Einen nach vorne
				if(src - dst == 1) {
					// Berechne Entfernungen f�r IST
					val += getDistanceForOptimization(giantTour[src], giantTour[src+1]);
					val += getDistanceForOptimization(giantTour[dst], giantTour[src]);
					val += getDistanceForOptimization(giantTour[dst-1], giantTour[dst]);
					// Berechne Entfernungen f�r SOLL
					val -= getDistanceForOptimization(giantTour[dst], giantTour[src+1]);
					val -= getDistanceForOptimization(giantTour[src], giantTour[dst]);
					val -= getDistanceForOptimization(giantTour[dst-1], giantTour[src]);
				}
				else {
					// Berechne Entfernungen f�r IST
					val += getDistanceForOptimization(giantTour[src-1], giantTour[src]);
					val += getDistanceForOptimization(giantTour[src], giantTour[src+1]);
					val += getDistanceForOptimization(giantTour[dst-1], giantTour[dst]);
					// Berechne Entfernungen f�r SOLL
					val -= getDistanceForOptimization(giantTour[src-1], giantTour[src+1]);
					val -= getDistanceForOptimization(giantTour[src], giantTour[dst]);
					val -= getDistanceForOptimization(giantTour[dst-1], giantTour[src]);
				}

				if(val > epsilon)
					improvingStepList.add(new float[]{src, dst, val});
			}
		}
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
