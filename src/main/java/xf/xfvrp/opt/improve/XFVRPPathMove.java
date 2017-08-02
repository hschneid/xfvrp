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
 * The Path Move neighborhood operator removes from a solution
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
 * in the ordering of nodes (like 2-Opt). 
 * 
 * @author hschneid
 *
 */
public class XFVRPPathMove extends XFVRPOptImpBase {

	private static final int NO_INVERT = 0;
	private static final int INVERT = 1;

	private boolean isInvertationActive = true;

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) {
		final Set<String> loadingFootprint = getLoadingFootprint(solution);

		Node[] giantTour = solution.getGiantRoute();
		
		List<float[]> improvingStepList = search(giantTour);

		// Sortier absteigend nach Potenzial
		sort(improvingStepList, 4);

		// Find first valid improving change
		for (float[] val : improvingStepList) {
			// Variation
			change(solution, val);

			Quality result = check(solution, loadingFootprint);
			if(result != null && result.getCost() < bestResult.getCost())
				return result;

			// Reverse-Variation
			reverseChange(solution, val);
		}

		return null;
	}

	private void change(Solution solution, float[] val) {
		int a = (int) val[0];
		int b = (int) val[1];
		int l = (int) val[2];
		int i = (int) val[3];

		if(i == INVERT) swap(solution, a, a + l);
		pathMove(solution, a, a + l, b);
	}

	private void reverseChange(Solution solution, float[] val) {
		int a = (int) val[0];
		int b = (int) val[1];
		int l = (int) val[2];
		int i = (int) val[3];

		if(b > a)
			pathMove(solution, b - l - 1, b - 1, a);
		else
			pathMove(solution, b, b + l, a + l + 1);
		if(i == INVERT) swap(solution, a, a + l);
	}

	/**
	 * Searches all improving valid steps in search space for
	 * a VRP with multiple depots.
	 * 
	 * @param giantRoute
	 * @return improvingStepList
	 */
	private List<float[]> search(Node[] giantTour) {
		List<float[]> improvingStepList = new ArrayList<>();
		
		final int length = giantTour.length;
		int[] tourIdMarkArr = new int[length];
		int[] depotMarkArr = new int[length];
		int lastDepotIdx = 0;
		int id = 0;
		for (int i = 1; i < tourIdMarkArr.length; i++) {
			if(giantTour[i].getSiteType() == SiteType.DEPOT) {
				id++;
				lastDepotIdx = giantTour[i].getIdx();
			}
			tourIdMarkArr[i] = id;
			depotMarkArr[i] = lastDepotIdx;
		}

		// Suche alle verbessernden L�sungen
		for (int a = 1; a < length - 1; a++) {
			for (int b = 1; b < length; b++) {
				if(a == b)
					continue;

				// A darf kein Depot sein
				if(giantTour[a].getSiteType() == SiteType.DEPOT)
					continue;

				// Segmente m�ssen auf der selben Tour liegen
				for (int l = 0; l < 4; l++) {
					findImprovements(giantTour, depotMarkArr, tourIdMarkArr, a, b, l, improvingStepList);
				}
			}
		}

		return improvingStepList;
	}

	/**
	 * 
	 * @param giantRoute
	 * @param depotMarkArr
	 * @param tourIdMarker
	 * @param a
	 * @param b
	 * @param l
	 * @param impList
	 */
	private void findImprovements(Node[] giantRoute, int[] depotMarkArr, int[] tourIdMarker, int a, int b, int l, List<float[]> impList) {
		if(a + l + 1 >= giantRoute.length)
			return;

		// B must not lay in the segment
		if(b <= a + l && b >= a)
			return;

		// No useless moves
		if(a < b && b - (a + l) == 1)
			return;

		if(tourIdMarker[a] != tourIdMarker[a + l])
			return;

		int predA = (a - b == 1) ? b : a - 1;

		int markA = depotMarkArr[a];
		int markB = depotMarkArr[b - 1];

		float old = getDistance(giantRoute[predA], giantRoute[a], markA) + 
				getDistance(giantRoute[a + l], giantRoute[a + l + 1], markA) +
				getDistance(giantRoute[b - 1], giantRoute[b], markB);

		float val = 0;
		// No invert
		{
			val = 
					old - 
					(getDistance(giantRoute[predA], giantRoute[a + l + 1], markA) +
							getDistance(giantRoute[b - 1], giantRoute[a], markB) +
							getDistance(giantRoute[a + l], giantRoute[b], markB));
			if(val > epsilon) impList.add(new float[]{a, b, l, NO_INVERT, val});
		}
		// with invert
		if(isInvertationActive) {
			val = 
					old - 
					(getDistance(giantRoute[predA], giantRoute[a + l + 1], markA) +
							getDistance(giantRoute[b - 1], giantRoute[a + l], markB) +
							getDistance(giantRoute[a], giantRoute[b], markB));
			if(val > epsilon) impList.add(new float[]{a, b, l, INVERT, val});
		}
	}

	public void setInvertationMode(boolean isInvertationActive) {
		this.isInvertationActive = isInvertationActive;
	}
}
