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
 * @author hschneid
 *
 */
public class XFVRP3PointMove extends XFVRPOptImpBase {

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
		sort(improvingStepList, 2);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			int a = (int) val[0];
			int b = (int) val[1];

			swap3Point(giantTour, a, b);

			Quality result = check(giantTour, loadingFootprint);
			if(result != null && result.getFitness() < bestResult.getFitness())
				return result;

			if(a < b)
				swap3Point(giantTour, b - 1, a);
			else 
				swap3Point(giantTour, b, a + 1);
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
		for (int a = 1; a < giantTour.length - 2; a++) {
			for (int b = 1; b < giantTour.length - 1; b++) {
				if(a == b || a + 1 == b || a + 2 == b)
					continue;

				int predA = a - 1;
				if(a - b == 1)
					predA = b;

				float old = 
					getDistanceForOptimization(giantTour[predA], giantTour[a]) + 
					getDistanceForOptimization(giantTour[a + 1], giantTour[a + 2]) +
					getDistanceForOptimization(giantTour[b - 1], giantTour[b]) +
					((b == giantTour.length - 1 || a - b == 1) ? 0 : getDistanceForOptimization(giantTour[b], giantTour[b + 1]));


				float val = 0;
				if(a - b == 1) {
					val = old -  
					(getDistanceForOptimization(giantTour[a + 1], giantTour[b]) + 
							getDistanceForOptimization(giantTour[b], giantTour[a + 2]) +
							getDistanceForOptimization(giantTour[b - 1], giantTour[a]));
				} else
					val = old -  
					(getDistanceForOptimization(giantTour[predA], giantTour[b]) + 
							getDistanceForOptimization(giantTour[b], giantTour[a + 2]) +
							getDistanceForOptimization(giantTour[b - 1], giantTour[a]) + 
							((b == giantTour.length - 1) ? 0 : getDistanceForOptimization(giantTour[a + 1], giantTour[b + 1])));

				if(val > epsilon) improvingStepList.add(new float[]{a, b, val});
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
		for (int a = 1; a < giantTour.length - 2; a++) {
			int markA = depotMarkArr[a];
			for (int b = 1; b < giantTour.length - 1; b++) {
				if(a == b || a + 1 == b || a + 2 == b)
					continue;

				int predA = a - 1;
				if(a - b == 1)
					predA = b;

				// Alle Punkte m�ssen auf Auftr�gen liegen
				if(giantTour[a].getSiteType() == SiteType.DEPOT ||
						giantTour[a+1].getSiteType() == SiteType.DEPOT ||
						giantTour[b].getSiteType() == SiteType.DEPOT
				)
					continue;

				int markB = depotMarkArr[b - 1];

				float old = 
					getDistance(giantTour[predA], giantTour[a], markA) + 
					getDistance(giantTour[a + 1], giantTour[a + 2], markA) +
					getDistance(giantTour[b - 1], giantTour[b], markB) +
					((b == giantTour.length - 1 || a - b == 1) ? 0 : getDistance(giantTour[b], giantTour[b + 1], markB));


				float val = 0;
				if(a - b == 1) {
					val = old -  
					(getDistance(giantTour[a + 1], giantTour[b], markA) + 
							getDistance(giantTour[b], giantTour[a + 2], markB) +
							getDistance(giantTour[b - 1], giantTour[a], markB));
				} else
					val = old -  
					(getDistance(giantTour[predA], giantTour[b], markA) + 
							getDistance(giantTour[b], giantTour[a + 2], markA) +
							getDistance(giantTour[b - 1], giantTour[a], markB) + 
							((b == giantTour.length - 1) ? 0 : getDistance(giantTour[a + 1], giantTour[b + 1], markB)));

				if(val > epsilon) improvingStepList.add(new float[]{a, b, val});
			}
		}
	}

	/**
	 * 
	 * @param giantRoute
	 * @param a
	 * @param b
	 */
	private void swap3Point(Node[] giantTour, int a, int b) {
		Node[] arr = new Node[]{giantTour[a], giantTour[a + 1]};

		if(a < b) {
			giantTour[a] = giantTour[b];
			// Verschiebe den Block zwischen a + 1 und b
			System.arraycopy(giantTour, a + 2, giantTour, a + 1, b - (a + 1) - 1);
			System.arraycopy(arr, 0, giantTour, b - 1, 2);
		} else {
			giantTour[a + 1] = giantTour[b];
			System.arraycopy(giantTour, b + 1, giantTour, b + 2, a - b - 1);
			System.arraycopy(arr, 0, giantTour, b, 2);
		}
	}
}
