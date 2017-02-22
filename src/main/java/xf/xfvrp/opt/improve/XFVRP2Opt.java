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
 * Standard 2-opt search by inverting the giant tour
 * 
 * Implementation follows best ascent pattern by fully evaluating the n^2 neighborhood.
 * While improvements can be found, first all possible improving solutions are calculated. 
 * Then each of them are checked concerning all constraints.
 *
 * @author hschneid
 */
public class XFVRP2Opt extends XFVRPOptImpBase {
	
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
			int i = (int) val[0];
			int j = (int) val[1];

			swap(solution, i, j);

			Quality result = check(solution, loadingFootprint);
			if(result != null && result.getFitness() < bestResult.getFitness())
				return result;

			swap(solution, i, j);
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
			float distI = getDistanceForOptimization(giantTour[i-1], giantTour[i]);
			
			for (int j = i + 1; j < giantTour.length - 1; j++) {
				float val = 0;
				// Bestimme die L�nge von i-1 nach i
				val += distI;
				// Bestimme die L�nge von j nach j+1
				val += getDistanceForOptimization(giantTour[j], giantTour[j+1]);
				// Bestimme die L�nge von i-1 nach j
				val -= getDistanceForOptimization(giantTour[i-1], giantTour[j]);
				// Bestimme die L�nge von i nach j+1
				val -= getDistanceForOptimization(giantTour[i], giantTour[j+1]);
				if(val > epsilon) {
					improvingStepList.add(new float[]{i, j, val});
				}
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
		for (int i = 1; i < giantTour.length - 1; i++) {
			int markA = depotMarkArr[i];
			int markAM = depotMarkArr[i-1];
			for (int j = i + 1; j < giantTour.length - 1; j++) {
				int markB = depotMarkArr[j];
				int markBP = depotMarkArr[j+1];

				float val = 0;
				// Bestimme die L�nge von i-1 nach i
				val += getDistance(giantTour[i-1], giantTour[i], markAM, markA);
				// Bestimme die L�nge von j nach j+1
				val += getDistance(giantTour[j], giantTour[j+1], markB, markBP);
				// Bestimme die L�nge von i-1 nach j
				val -= getDistance(giantTour[i-1], giantTour[j], markAM, markB);
				// Bestimme die L�nge von i nach j+1
				val -= getDistance(giantTour[i], giantTour[j+1], markA, markBP);
				if(val > epsilon) {
					improvingStepList.add(new float[]{i, j, val});
				}
			}
		}
	}
}
