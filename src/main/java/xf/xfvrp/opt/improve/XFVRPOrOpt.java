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
 * The OrOpt neighborhood removes from a solution
 * 3 edges, but the 3 inserted edges considers a
 * specific logic (other than 3-opt) so that a certain
 * segment of the route plan (a path) is relocated to
 * any position in the route plan.
 * 
 * To improve the performance of this huge neighborhood search
 * the length of the paths is restricted up to 4. Longer paths
 * than 4 are not relocated.
 * 
 * @author hschneid
 *
 */
public class XFVRPOrOpt extends XFVRPOptImpBase {

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
		sort(improvingStepList, 3);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			int a = (int) val[0];
			int b = (int) val[1];
			int l = (int) val[2];

			pathMove(solution, a, a + l, b);

			Quality result = check(solution, loadingFootprint);
			if(result != null && result.getFitness() < bestResult.getFitness())
				return result;

			if(b > a)
				pathMove(solution, b - l - 1, b - 1, a);
			else
				pathMove(solution, b, b + l, a + l + 1);
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
		for (int a = 1; a < giantTour.length - 4; a++) {
			for (int b = 1; b < giantTour.length - 1; b++) {
				if(a == b)
					continue;

				float val = findImprovementsSingleDepot(giantTour, a, b, 1);
				if(val > epsilon) improvingStepList.add(new float[]{a, b, 1, val});
				val = findImprovementsSingleDepot(giantTour, a, b, 2);
				if(val > epsilon) improvingStepList.add(new float[]{a, b, 2, val});
				val = findImprovementsSingleDepot(giantTour, a, b, 3);
				if(val > epsilon) improvingStepList.add(new float[]{a, b, 3, val});
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
		final int[] tourIdMarkArr = new int[giantTour.length];
		final int[] depotMarkArr = new int[giantTour.length];
		int lastDepotIdx = 0;
		int id = 0;
		for (int i = 1; i < tourIdMarkArr.length; i++) {
			if(giantTour[i].getSiteType() == SiteType.DEPOT)
				id++;
			tourIdMarkArr[i] = id;
			if(giantTour[i].getSiteType() == SiteType.DEPOT)
				lastDepotIdx = giantTour[i].getIdx();
			depotMarkArr[i] = lastDepotIdx;
		}

		// Suche alle verbessernden L�sungen
		for (int a = 1; a < giantTour.length - 4; a++) {
			for (int b = 1; b < giantTour.length - 1; b++) {
				if(a == b)
					continue;

				// Darf kein Depot sein
				if(giantTour[a].getSiteType() == SiteType.DEPOT)
					continue;
				
				float val;
				// Segmente m�ssen auf der selben Tour liegen
				if(tourIdMarkArr[a] == tourIdMarkArr[a + 1]) {
					val = findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 1);
					if(val > epsilon) improvingStepList.add(new float[]{a, b, 1, val});
				}
				if(tourIdMarkArr[a] == tourIdMarkArr[a + 2]) {
					val = findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 2);
					if(val > epsilon) improvingStepList.add(new float[]{a, b, 2, val});
				}
				if(tourIdMarkArr[a] == tourIdMarkArr[a + 3]) {
					val = findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 3);
					if(val > epsilon) improvingStepList.add(new float[]{a, b, 3, val});
				}
			}
		}
	}

	/**
	 * 
	 * @param giantRoute
	 * @param a
	 * @param b
	 * @param l
	 * @return
	 */
	private float findImprovementsSingleDepot(Node[] giantTour, int a, int b, int l) {
		if(a < b && b - a <= l + 1)
			return 0;

		int predA = a - 1;
		if(a-b == 1)
			predA = b;

		float old = getDistanceForOptimization(giantTour[predA], giantTour[a]) + 
		getDistanceForOptimization(giantTour[a + l], giantTour[a + l + 1]) +
		getDistanceForOptimization(giantTour[b - 1], giantTour[b]);

		return old - 
		(getDistanceForOptimization(giantTour[predA], giantTour[a + l + 1]) +
				getDistanceForOptimization(giantTour[b - 1], giantTour[a]) +
				getDistanceForOptimization(giantTour[a + l], giantTour[b]));
	}
	
	/**
	 * 
	 * @param giantRoute
	 * @param depotMarkArr 
	 * @param a
	 * @param b
	 * @param l
	 * @return
	 */
	private float findImprovementsMultipleDepot(Node[] giantTour, int[] depotMarkArr, int a, int b, int l) {
		if(a < b && b - a <= l + 1)
			return 0;

		int predA = a - 1;
		if(a - b == 1)
			predA = b;

		int markA = depotMarkArr[a];
		int markB = depotMarkArr[b - 1];
		
		float old = getDistance(giantTour[predA], giantTour[a], markA) + 
		getDistance(giantTour[a + l], giantTour[a + l + 1], markA) +
		getDistance(giantTour[b - 1], giantTour[b], markB);

		return old - 
		(getDistance(giantTour[predA], giantTour[a + l + 1], markA) +
				getDistance(giantTour[b - 1], giantTour[a], markB) +
				getDistance(giantTour[a + l], giantTour[b], markB));
	}
}
