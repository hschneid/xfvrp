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
 * This neighborhood search produces improved solutions by
 * exchanging two segments of the giant tour. The size of the
 * segments must not be equal and has sizes between 2 and 5.
 * 
 * @author hschneid
 *
 */
public class XFVRPSwapSegment extends XFVRPOptImpBase {

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

		// Sortiere absteigend nach Potenzial
		sort(improvingStepList, 4);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			int a = (int) val[0];
			int b = (int) val[1];
			int l = (int) val[2];
			int ll = (int) val[3];

			exchange(giantTour, a, b, l, ll);

			Quality result = check(giantTour, loadingFootprint);
			if(result != null && result.getFitness() < bestResult.getFitness())
				return result;

			exchange(giantTour, a, b + ((ll+1) - (l+1)), ll, l);

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
				float val = 0;
				for (int ll = 1; ll <= 4; ll++) {
					val = findImprovementsSingleDepot(giantTour, a, b, 1, ll);
					if(val > epsilon) improvingStepList.add(new float[]{a, b, 1, ll, val});
					val = findImprovementsSingleDepot(giantTour, a, b, 2, ll);
					if(val > epsilon) improvingStepList.add(new float[]{a, b, 2, ll, val});
					val = findImprovementsSingleDepot(giantTour, a, b, 3, ll);
					if(val > epsilon) improvingStepList.add(new float[]{a, b, 3, ll, val});
					val = findImprovementsSingleDepot(giantTour, a, b, 4, ll);
					if(val > epsilon) improvingStepList.add(new float[]{a, b, 4, ll, val});
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
			for (int b = a + 2; b < giantTour.length - 3; b++) {
				if(giantTour[b].getSiteType() == SiteType.DEPOT)
					continue;

				float val = 0;
				for (int ll = 1; ll <= 4; ll++) {
					if(tourIdMarkArr[b] != tourIdMarkArr[b+ll])
						continue;

					if(tourIdMarkArr[a] == tourIdMarkArr[a+1]) {
						val = findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 1, ll);
						if(val > epsilon) improvingStepList.add(new float[]{a, b, 1, ll, val});
					}
					if(tourIdMarkArr[a] == tourIdMarkArr[a+2]) {
						val = findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 2, ll);
						if(val > epsilon) improvingStepList.add(new float[]{a, b, 2, ll, val});
					}
					if(tourIdMarkArr[a] == tourIdMarkArr[a+3]) {
						val = findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 3, ll);
						if(val > epsilon) improvingStepList.add(new float[]{a, b, 3, ll, val});
					}
					if(tourIdMarkArr[a] == tourIdMarkArr[a+4]) {
						val = findImprovementsMultipleDepot(giantTour, depotMarkArr, a, b, 4, ll);
						if(val > epsilon) improvingStepList.add(new float[]{a, b, 4, ll, val});
					}
				}
			}
		}
	}

//	/**
//	 * 
//	 * 
//	 * @param giantRoute
//	 * @param a
//	 * @param b
//	 * @param la
//	 * @param lb
//	 */
//	private void exchange(XFNode[] giantRoute, int a, int b, int la, int lb) {
//		if(la == lb) {
//			for (int i = 0; i <= la; i++)
//				exchange(giantRoute, a+i, b+i);
//		} else {
//			XFNode[] aArr = new XFNode[la + 1];
//			System.arraycopy(giantRoute, a , aArr, 0, aArr.length);
//			XFNode[] bArr = new XFNode[lb + 1];
//			System.arraycopy(giantRoute, b , bArr, 0, bArr.length);
//			XFNode[] iArr = new XFNode[b - (a + la + 1)];
//			System.arraycopy(giantRoute, a + la + 1 , iArr, 0, b - (a + la + 1));
//
//			System.arraycopy(bArr, 0 , giantRoute, a, bArr.length);
//			System.arraycopy(iArr, 0 , giantRoute, a + bArr.length, iArr.length);
//			System.arraycopy(aArr, 0 , giantRoute, a + bArr.length + iArr.length, aArr.length);
//		}
//	}

	/**
	 * 
	 * @param giantRoute
	 * @param a
	 * @param b
	 * @param la
	 * @return
	 */
	private float findImprovementsSingleDepot(Node[] giantTour, int a, int b, int la, int lb) {
		int aa = a + la;
		if(aa >= b)
			return 0;

		int bb = b + lb;
		if(bb >= giantTour.length - 1)
			return 0;

		float val = 0;
		if((b - aa) == 1) {
			val = getDistanceForOptimization(giantTour[a - 1], giantTour[a]) + 
			getDistanceForOptimization(giantTour[aa], giantTour[b]) +
			getDistanceForOptimization(giantTour[bb], giantTour[bb + 1]);

			val -= getDistanceForOptimization(giantTour[a - 1], giantTour[b]) + 
			getDistanceForOptimization(giantTour[bb], giantTour[a]) +
			getDistanceForOptimization(giantTour[aa], giantTour[bb + 1]);
		} else {
			val = getDistanceForOptimization(giantTour[a - 1], giantTour[a]) + 
			getDistanceForOptimization(giantTour[aa], giantTour[aa + 1]) +
			getDistanceForOptimization(giantTour[b - 1], giantTour[b]) +
			getDistanceForOptimization(giantTour[bb], giantTour[bb + 1]);

			val -= getDistanceForOptimization(giantTour[a - 1], giantTour[b]) + 
			getDistanceForOptimization(giantTour[bb], giantTour[aa + 1]) +
			getDistanceForOptimization(giantTour[b - 1], giantTour[a]) +
			getDistanceForOptimization(giantTour[aa], giantTour[bb + 1]);
		}

		return val;
	}
	
	/**
	 * 
	 * @param giantRoute
	 * @param depotMarkArr 
	 * @param a
	 * @param b
	 * @param la
	 * @return
	 */
	private float findImprovementsMultipleDepot(Node[] giantTour, int[] depotMarkArr, int a, int b, int la, int lb) {
		int aa = a + la;
		if(aa >= b)
			return 0;

		int bb = b + lb;
		if(bb >= giantTour.length - 1)
			return 0;

		int markA = depotMarkArr[a];
		int markB = depotMarkArr[b - 1];
		
		float val = 0;
		if((b - aa) == 1) {
			val = getDistance(giantTour[a - 1], giantTour[a], markA) + 
			getDistance(giantTour[aa], giantTour[b], markA) +
			getDistance(giantTour[bb], giantTour[bb + 1], markB);

			val -= getDistance(giantTour[a - 1], giantTour[b], markA) + 
			getDistance(giantTour[bb], giantTour[a], markA) +
			getDistance(giantTour[aa], giantTour[bb + 1], markB);
		} else {
			val = getDistance(giantTour[a - 1], giantTour[a], markA) + 
			getDistance(giantTour[aa], giantTour[aa + 1], markA) +
			getDistance(giantTour[b - 1], giantTour[b], markB) +
			getDistance(giantTour[bb], giantTour[bb + 1], markB);

			val -= getDistance(giantTour[a - 1], giantTour[b], markA) + 
			getDistance(giantTour[bb], giantTour[aa + 1], markA) +
			getDistance(giantTour[b - 1], giantTour[a], markB) +
			getDistance(giantTour[aa], giantTour[bb + 1], markB);
		}

		return val;
	}
}
