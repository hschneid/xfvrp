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
 * Standard 2-opt search by inverting the giant tour
 * 
 * Implementation follows best ascent pattern by fully evaluating the n^2 neighborhood.
 * While improvements can be found, first all possible improving solutions are calculated. 
 * Then each of them are checked concerning all constraints.
 * 
 * @author hschneid
 */
public class XFVRP2OptIntra extends XFVRPOptImpBase {

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) {
		Node[] giantRoute = solution.getGiantRoute();
		
		if(model.getNbrOfDepots() > 1)
			throw new UnsupportedOperationException(this.getClass().getName()+" supports no multi depot");

		// Suche alle verbessernden L�sungen
		List<float[]> improvingStepList = search(giantRoute);

		// Sortier absteigend nach Potenzial
		sort(improvingStepList, 2);

		// Finde die erste valide verbessernde Lösung
		for (float[] val : improvingStepList) {
			int i = (int) val[0];
			int j = (int) val[1];

			swap(solution, i, j);

			Quality result = checkIt(solution);
			if(result != null && result.getFitness() < bestResult.getFitness())
				return result;
			
			swap(solution, i, j);
		}

		return null;
	}

	private List<float[]> search(Node[] giantTour) {
		int[] tourIdMarkArr = new int[giantTour.length];
		int id = 0;
		for (int i = 1; i < giantTour.length; i++) {
			if(giantTour[i].getSiteType() == SiteType.DEPOT)
				id++;
			tourIdMarkArr[i] = id;
		}
		
		List<float[]> improvingStepList = new ArrayList<>();
		for (int i = 1; i < giantTour.length - 1; i++) {
			if(giantTour[i].getSiteType() == SiteType.DEPOT)
				continue;

			for (int j = i + 1; j < giantTour.length - 1; j++) {
				if(tourIdMarkArr[i] != tourIdMarkArr[j])
					break;

				float val = 0;
				// Bestimme die L�nge von i-1 nach i
				val += getDistanceForOptimization(giantTour[i-1], giantTour[i]);
				// Bestimme die L�nge von j nach j+1
				val += getDistanceForOptimization(giantTour[j], giantTour[j+1]);
				// Bestimme die L�nge von i-1 nach j
				val -= getDistanceForOptimization(giantTour[i-1], giantTour[j]);
				// Bestimme die L�nge von i nach j+1
				val -= getDistanceForOptimization(giantTour[i], giantTour[j+1]);
				if(val > epsilon)
					improvingStepList.add(new float[]{i, j, val});
			}
		}
		return improvingStepList;
	}
}
