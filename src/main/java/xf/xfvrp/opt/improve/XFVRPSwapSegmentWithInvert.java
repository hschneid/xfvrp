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
 * This neighborhood search produces improved solutions by
 * exchanging two segments of the giant tour. The size of the
 * segments must not be equal and has sizes between 2 and 5.
 * 
 * @author hschneid
 *
 */
public class XFVRPSwapSegmentWithInvert extends XFVRPOptImpBase {

	private static final int NO_INVERT = 0;
	private static final int A_INVERT = 1;
	private static final int B_INVERT = 2;
	private static final int BOTH_INVERT = 3;

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
		sort(improvingStepList, 5);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			int a = (int) val[0];
			int b = (int) val[1];
			int l = (int) val[2];
			int ll = (int) val[3];
			int i = (int) val[4];

			invert(solution, a, b, l, ll, i);
			exchange(solution, a, b, l, ll);

			Quality result = check(solution, loadingFootprint);
			if(result != null && result.getCost() < bestResult.getCost())
				return result;

			exchange(solution, a, b + ((ll + 1) - (l + 1)), ll, l);
			invert(solution, a, b, l, ll, i);
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
			for (int b = a + 2; b < giantTour.length - 3; b++) {
				for (int ll = 1; ll <= 4; ll++) {
					findImprovements(giantTour, a, b, 1, ll, improvingStepList);
					findImprovements(giantTour, a, b, 2, ll, improvingStepList);
					findImprovements(giantTour, a, b, 3, ll, improvingStepList);
					findImprovements(giantTour, a, b, 4, ll, improvingStepList);
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
		int[] tourIdMarkArr = new int[giantTour.length];
		int[] depotMarkArr = new int[giantTour.length];
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
			if(giantTour[a].getSiteType() == SiteType.DEPOT)
				continue;

			for (int b = a + 2; b < giantTour.length - 4; b++) {
				if(giantTour[b].getSiteType() == SiteType.DEPOT)
					continue;

				for (int ll = 1; ll <= 4; ll++) {
					if(tourIdMarkArr[b] != tourIdMarkArr[b+ll])
						continue;

					if(tourIdMarkArr[a] == tourIdMarkArr[a+1])
						findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 1, ll, improvingStepList);
					if(tourIdMarkArr[a] == tourIdMarkArr[a+2])
						findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 2, ll, improvingStepList);
					if(tourIdMarkArr[a] == tourIdMarkArr[a+3])
						findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 3, ll, improvingStepList);
					if(tourIdMarkArr[a] == tourIdMarkArr[a+4])
						findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 4, ll, improvingStepList);
				}
			}
		}
	}

	/**
	 * 
	 * @param giantRoute
	 * @param a Begin of first segment
	 * @param b Begin of second segment
	 * @param la Length of first segment
	 * @param lb Length of second segment
	 * @param impList
	 */
	private void findImprovements(Node[] giantTour, int a, int b, int la, int lb, List<float[]> impList) {
		int aa = a + la;
		if(aa >= b)
			return;

		int bb = b + lb;
		if(bb >= giantTour.length - 1)
			return;

		float old = 0;
		if((b - aa) == 1) {
			old = getDistanceForOptimization(giantTour[a - 1], giantTour[a]) + 
			getDistanceForOptimization(giantTour[aa], giantTour[b]) +
			getDistanceForOptimization(giantTour[bb], giantTour[bb + 1]);
		} else {
			old = getDistanceForOptimization(giantTour[a - 1], giantTour[a]) + 
			getDistanceForOptimization(giantTour[aa], giantTour[aa + 1]) +
			getDistanceForOptimization(giantTour[b - 1], giantTour[b]) +
			getDistanceForOptimization(giantTour[bb], giantTour[bb + 1]);
		}

		float val = 0;
		// NO INVERT
		{
			if((b - aa) == 1) {
				val = old - (getDistanceForOptimization(giantTour[a - 1], giantTour[b]) + 
						getDistanceForOptimization(giantTour[bb], giantTour[a]) +
						getDistanceForOptimization(giantTour[aa], giantTour[bb + 1]));
			} else {
				val = old - (getDistanceForOptimization(giantTour[a - 1], giantTour[b]) + 
						getDistanceForOptimization(giantTour[bb], giantTour[aa + 1]) +
						getDistanceForOptimization(giantTour[b - 1], giantTour[a]) +
						getDistanceForOptimization(giantTour[aa], giantTour[bb + 1]));
			}
			if(val > epsilon) impList.add(new float[]{a, b, la, lb, NO_INVERT, val});
		}
		// BOTH INVERT
		{
			if((b - aa) == 1) {
				val = old - (getDistanceForOptimization(giantTour[a - 1], giantTour[bb]) + 
						getDistanceForOptimization(giantTour[b], giantTour[aa]) +
						getDistanceForOptimization(giantTour[a], giantTour[bb + 1]));
			} else {
				val = old - (getDistanceForOptimization(giantTour[a - 1], giantTour[bb]) + 
						getDistanceForOptimization(giantTour[b], giantTour[aa + 1]) +
						getDistanceForOptimization(giantTour[b - 1], giantTour[aa]) +
						getDistanceForOptimization(giantTour[a], giantTour[bb + 1]));
			}
			if(val > epsilon) impList.add(new float[]{a, b, la, lb, BOTH_INVERT, val});
		}
		// A INVERT
		{
			if((b - aa) == 1) {
				val = old - (getDistanceForOptimization(giantTour[a - 1], giantTour[b]) + 
						getDistanceForOptimization(giantTour[bb], giantTour[aa]) +
						getDistanceForOptimization(giantTour[a], giantTour[bb + 1]));
			} else {
				val = old - (getDistanceForOptimization(giantTour[a - 1], giantTour[b]) + 
						getDistanceForOptimization(giantTour[bb], giantTour[aa + 1]) +
						getDistanceForOptimization(giantTour[b - 1], giantTour[aa]) +
						getDistanceForOptimization(giantTour[a], giantTour[bb + 1]));
			}
			if(val > epsilon) impList.add(new float[]{a, b, la, lb, A_INVERT, val});
		}
		// A INVERT
		{
			if((b - aa) == 1) {
				val = old - (getDistanceForOptimization(giantTour[a - 1], giantTour[bb]) + 
						getDistanceForOptimization(giantTour[b], giantTour[a]) +
						getDistanceForOptimization(giantTour[aa], giantTour[bb + 1]));
			} else {
				val = old - (getDistanceForOptimization(giantTour[a - 1], giantTour[bb]) + 
						getDistanceForOptimization(giantTour[b], giantTour[aa + 1]) +
						getDistanceForOptimization(giantTour[b - 1], giantTour[a]) +
						getDistanceForOptimization(giantTour[aa], giantTour[bb + 1]));
			}
			if(val > epsilon) impList.add(new float[]{a, b, la, lb, B_INVERT, val});
		}

	}

	/**
	 * 
	 * @param giantRoute
	 * @param a
	 * @param b
	 * @param la
	 * @param lb
	 * @param i
	 */
	private void invert(Solution solution, int a, int b, int la, int lb, int i) {
		switch (i) {
			case A_INVERT: {
				swap(solution, a, a + la);
				break;
			}
			case B_INVERT: {
				swap(solution, b, b + lb);
				break;
			}
			case BOTH_INVERT: {
				swap(solution, a, a + la);
				swap(solution, b, b + lb);
				break;
			}
			default:
				// NO_INVERT
				break;
		}
	}
	
	/**
	 * 
	 * @param giantRoute
	 * @param depotMarkArr
	 * @param a
	 * @param b
	 * @param la
	 * @param lb
	 * @param impList
	 */
	private void findImprovementsMultipleDepot(Node[] giantTour, int[] depotMarkArr, int a, int b, int la, int lb, List<float[]> impList) {
		int aa = a + la;
		if(aa >= b)
			return;

		int bb = b + lb;
		if(bb >= giantTour.length - 1)
			return;
		
		int markA = depotMarkArr[a];
		int markB = depotMarkArr[b - 1];

		float old = 0;
		if((b - aa) == 1) {
			old = getDistance(giantTour[a - 1], giantTour[a], markA) + 
			getDistance(giantTour[aa], giantTour[b], markA) +
			getDistance(giantTour[bb], giantTour[bb + 1], markB);
		} else {
			old = getDistance(giantTour[a - 1], giantTour[a], markA) + 
			getDistance(giantTour[aa], giantTour[aa + 1], markA) +
			getDistance(giantTour[b - 1], giantTour[b], markB) +
			getDistance(giantTour[bb], giantTour[bb + 1], markB);
		}

		float val = 0;
		// NO INVERT
		{
			if((b - aa) == 1) {
				val = old - (getDistance(giantTour[a - 1], giantTour[b], markA) + 
						getDistance(giantTour[bb], giantTour[a], markA) +
						getDistance(giantTour[aa], giantTour[bb + 1], markB));
			} else {
				val = old - (getDistance(giantTour[a - 1], giantTour[b], markA) + 
						getDistance(giantTour[bb], giantTour[aa + 1], markA) +
						getDistance(giantTour[b - 1], giantTour[a], markB) +
						getDistance(giantTour[aa], giantTour[bb + 1], markB));
			}
			if(val > epsilon) impList.add(new float[]{a, b, la, lb, NO_INVERT, val});
		}
		// BOTH INVERT
		{
			if((b - aa) == 1) {
				val = old - (getDistance(giantTour[a - 1], giantTour[bb], markA) + 
						getDistance(giantTour[b], giantTour[aa], markA) +
						getDistance(giantTour[a], giantTour[bb + 1], markB));
			} else {
				val = old - (getDistance(giantTour[a - 1], giantTour[bb], markA) + 
						getDistance(giantTour[b], giantTour[aa + 1], markA) +
						getDistance(giantTour[b - 1], giantTour[aa], markB) +
						getDistance(giantTour[a], giantTour[bb + 1], markB));
			}
			if(val > epsilon) impList.add(new float[]{a, b, la, lb, BOTH_INVERT, val});
		}
		// A INVERT
		{
			if((b - aa) == 1) {
				val = old - (getDistance(giantTour[a - 1], giantTour[b], markA) + 
						getDistance(giantTour[bb], giantTour[aa],markA) +
						getDistance(giantTour[a], giantTour[bb + 1], markB));
			} else {
				val = old - (getDistance(giantTour[a - 1], giantTour[b], markA) + 
						getDistance(giantTour[bb], giantTour[aa + 1], markA) +
						getDistance(giantTour[b - 1], giantTour[aa], markB) +
						getDistance(giantTour[a], giantTour[bb + 1], markB));
			}
			if(val > epsilon) impList.add(new float[]{a, b, la, lb, A_INVERT, val});
		}
		// B INVERT
		{
			if((b - aa) == 1) {
				val = old - (getDistance(giantTour[a - 1], giantTour[bb], markA) + 
						getDistance(giantTour[b], giantTour[a], markA) +
						getDistance(giantTour[aa], giantTour[bb + 1], markB));
			} else {
				val = old - (getDistance(giantTour[a - 1], giantTour[bb], markA) + 
						getDistance(giantTour[b], giantTour[aa + 1], markA) +
						getDistance(giantTour[b - 1], giantTour[a], markB) +
						getDistance(giantTour[aa], giantTour[bb + 1], markB));
			}
			if(val > epsilon) impList.add(new float[]{a, b, la, lb, B_INVERT, val});
		}

	}
}