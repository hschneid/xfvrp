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
 * The swap neighborhood search improves a solution
 * by exchanging two nodes at their positions. The size
 * of the neighborhood is O(n^2).
 * 
 * @author hschneid
 *
 */
public class XFVRPSwap extends XFVRPOptImpBase {

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
			int i = (int) val[0];
			int j = (int) val[1];

			exchange(solution, i, j);

			Quality result = checkIt(solution);
			if(result != null && result.getFitness() < bestResult.getFitness())
				return result;
			
			exchange(solution, i, j);
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
		for (int i = 1; i < giantTour.length - 1; i++) {
			for (int j = i + 1; j < giantTour.length - 1; j++) {
				float val = 0;
				
				// Swap of two depots (single depot problem) is not necessary
				if(giantTour[i].getSiteType() == SiteType.DEPOT &&
						giantTour[j].getSiteType() == SiteType.DEPOT)
					continue;

				if(j - i == 1) {
					// Berechne Entfernungen f�r IST
					val += getDistanceForOptimization(giantTour[i-1], giantTour[i]);
					val += getDistanceForOptimization(giantTour[i], giantTour[j]);
					val += getDistanceForOptimization(giantTour[j], giantTour[j+1]);
					// Berechne Entfernungen f�r SOLL
					val -= getDistanceForOptimization(giantTour[i-1], giantTour[j]);
					val -= getDistanceForOptimization(giantTour[j], giantTour[i]);
					val -= getDistanceForOptimization(giantTour[i], giantTour[j+1]);						
				} else {
					// Berechne Entfernungen f�r IST
					val += getDistanceForOptimization(giantTour[i-1], giantTour[i]);
					val += getDistanceForOptimization(giantTour[i], giantTour[i+1]);
					val += getDistanceForOptimization(giantTour[j-1], giantTour[j]);
					val += getDistanceForOptimization(giantTour[j], giantTour[j+1]);

					// Berechne Entfernungen f�r SOLL
					val -= getDistanceForOptimization(giantTour[i-1], giantTour[j]);
					val -= getDistanceForOptimization(giantTour[j], giantTour[i+1]);
					val -= getDistanceForOptimization(giantTour[j-1], giantTour[i]);
					val -= getDistanceForOptimization(giantTour[i], giantTour[j+1]);
				}
				if(val > epsilon)
					improvingStepList.add(new float[]{i, j, val});
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
		int lastDepotIdx = 0;
		for (int i = 1; i < depotMarkArr.length; i++) {
			if(giantTour[i].getSiteType() == SiteType.DEPOT)
				lastDepotIdx = giantTour[i].getIdx();
			depotMarkArr[i] = lastDepotIdx;
		}

		// Suche alle verbessernden L�sungen
		for (int a = 1; a < giantTour.length - 1; a++) {
			if(giantTour[a].getSiteType() == SiteType.DEPOT)
				continue;

			int markA = depotMarkArr[a];
			for (int b = a + 1; b < giantTour.length - 1; b++) {
				int markB = depotMarkArr[b];
				float val = 0;

				if(giantTour[b].getSiteType() == SiteType.DEPOT)
					continue;
				
				if(b - a == 1) {
					// Berechne Entfernungen f�r IST
					val += getDistance(giantTour[a-1], giantTour[a], markA);
					val += getDistance(giantTour[a], giantTour[b], markA);
					val += getDistance(giantTour[b], giantTour[b+1], markB);
					// Berechne Entfernungen f�r SOLL
					val -= getDistance(giantTour[a-1], giantTour[b], markA);
					val -= getDistance(giantTour[b], giantTour[a], markA);
					val -= getDistance(giantTour[a], giantTour[b+1], markB);						
				} else {
					// Berechne Entfernungen f�r IST
					val += getDistance(giantTour[a-1], giantTour[a], markA);
					val += getDistance(giantTour[a], giantTour[a+1], markA);
					val += getDistance(giantTour[b-1], giantTour[b], markB);
					val += getDistance(giantTour[b], giantTour[b+1], markB);

					// Berechne Entfernungen f�r SOLL
					val -= getDistance(giantTour[a-1], giantTour[b], markA);
					val -= getDistance(giantTour[b], giantTour[a+1], markA);
					val -= getDistance(giantTour[b-1], giantTour[a], markB);
					val -= getDistance(giantTour[a], giantTour[b+1], markB);
				}
				if(val > 0)
					improvingStepList.add(new float[]{a, b, val});
			}
		}
	}
}
