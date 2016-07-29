package xf.xfvrp.opt.improve;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * The OrOpt (with invert) neighborhood removes from a solution
 * 3 edges, but the 3 inserted edges considers a
 * specific logic (other than 3-opt) so that a certain
 * segment of the route plan (a path) is relocated to
 * any position in the route plan.
 * 
 * To improve the performance of this huge neighborhood search
 * the length of the paths is restricted up to 4. Longer paths
 * than 4 are not relocated.
 * 
 * As expansion of standard OrOpt a chosen path can be additionally inverted
 * in the ordering of nodes. 
 * 
 * @author hschneid
 *
 */
public class XFVRPOrOptWithInvert extends XFVRPOptImpBase {

	private static final int NO_INVERT = 0;
	private static final int INVERT = 1;

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Node[] giantTour, Quality bestResult) {
		final Set<String> loadingFootprint = getLoadingFootprint(giantTour);

		List<float[]> improvingStepList = new ArrayList<>();

		if(model.getNbrOfDepots() == 1)
			searchSingleDepot(giantTour, improvingStepList);
		else
			searchMultiDepot(giantTour, improvingStepList);

		// Sortier absteigend nach Potenzial
		sort(improvingStepList, 4);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			int a = (int) val[0];
			int b = (int) val[1];
			int l = (int) val[2];
			int i = (int) val[3];

			// Variation
			if(i == INVERT) swap(giantTour, a, a + l);
			pathMove(giantTour, a, a + l, b);

			Quality result = check(giantTour, loadingFootprint);
			if(result != null && result.getCost() < bestResult.getCost())
				return result;

			// Reverse-Variation
			if(b > a)
				pathMove(giantTour, b - l - 1, b - 1, a);
			else
				pathMove(giantTour, b, b + l, a + l + 1);
			if(i == INVERT) swap(giantTour, a, a + l);
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

				findImprovementsSingleDepot(giantTour, a, b, 1, improvingStepList);
				findImprovementsSingleDepot(giantTour, a, b, 2, improvingStepList);
				findImprovementsSingleDepot(giantTour, a, b, 3, improvingStepList);
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
		final int length = giantTour.length;
		int[] tourIdMarkArr = new int[length];
		int[] depotMarkArr = new int[length];
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
		for (int a = 0; a < length; a++) {
			for (int b = 1; b < length - 1; b++) {
				if(a == b)
					continue;

				// Darf kein Depot sein
				if(giantTour[a].getSiteType() == SiteType.DEPOT)
					continue;

				// Segmente m�ssen auf der selben Tour liegen
				if(a + 1 < length && tourIdMarkArr[a] == tourIdMarkArr[a + 1])
					findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 1, improvingStepList);
				if(a + 2 < length && tourIdMarkArr[a] == tourIdMarkArr[a + 2])
					findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 2, improvingStepList);
				if(a + 3 < length && tourIdMarkArr[a] == tourIdMarkArr[a + 3])
					findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 3, improvingStepList);
			}
		}

	}

	/**
	 * 
	 * @param giantRoute
	 * @param a
	 * @param b
	 * @param l
	 * @param impList
	 */
	private void findImprovementsSingleDepot(Node[] giantTour, int a, int b, int l, List<float[]> impList) {
		if(a < b && b - a <= l + 1)
			return;

		int predA = a - 1;
		if(a - b == 1)
			predA = b;

		float old = getDistanceForOptimization(giantTour[predA], giantTour[a]) + 
				getDistanceForOptimization(giantTour[a + l], giantTour[a + l + 1]) +
				getDistanceForOptimization(giantTour[b - 1], giantTour[b]);

		float val = 0;
		// No invert
		{
			val = 
					old - 
					(getDistanceForOptimization(giantTour[predA], giantTour[a + l + 1]) +
							getDistanceForOptimization(giantTour[b - 1], giantTour[a]) +
							getDistanceForOptimization(giantTour[a + l], giantTour[b]));
			if(val > epsilon) impList.add(new float[]{a, b, l, NO_INVERT, val});
		}
		// with invert
		{
			val = 
					old - 
					(getDistanceForOptimization(giantTour[predA], giantTour[a + l + 1]) +
							getDistanceForOptimization(giantTour[b - 1], giantTour[a + l]) +
							getDistanceForOptimization(giantTour[a], giantTour[b]));
			if(val > epsilon) impList.add(new float[]{a, b, l, INVERT, val});
		}
	}

	/**
	 * 
	 * @param giantRoute
	 * @param depotMarkArr
	 * @param a
	 * @param b
	 * @param l
	 * @param impList
	 */
	private void findImprovementsMultipleDepot(Node[] giantTour, int[] depotMarkArr, int a, int b, int l, List<float[]> impList) {
		if(a < b && b - a <= l + 1)
			return;

		int predA = a - 1;
		if(a - b == 1)
			predA = b;

		int markA = depotMarkArr[a];
		int markB = depotMarkArr[b - 1];

		float old = getDistance(giantTour[predA], giantTour[a], markA) + 
				getDistance(giantTour[a + l], giantTour[a + l + 1], markA) +
				getDistance(giantTour[b - 1], giantTour[b], markB);

		float val = 0;
		// No invert
		{
			val = 
					old - 
					(getDistance(giantTour[predA], giantTour[a + l + 1], markA) +
							getDistance(giantTour[b - 1], giantTour[a], markB) +
							getDistance(giantTour[a + l], giantTour[b], markB));
			if(val > epsilon) impList.add(new float[]{a, b, l, NO_INVERT, val});
		}
		// with invert
		{
			val = 
					old - 
					(getDistance(giantTour[predA], giantTour[a + l + 1], markA) +
							getDistance(giantTour[b - 1], giantTour[a + l], markB) +
							getDistance(giantTour[a], giantTour[b], markB));
			if(val > epsilon) impList.add(new float[]{a, b, l, INVERT, val});
		}
	}
}
