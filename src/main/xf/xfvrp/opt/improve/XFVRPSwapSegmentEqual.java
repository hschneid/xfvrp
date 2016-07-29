package xf.xfvrp.opt.improve;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;


/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This neighborhood search produces improved solutions by
 * exchanging two segments of the giant tour. Both segments 
 * have the same size, which ranges between 2 and 5.
 * 
 * @author hschneid
 *
 */
public class XFVRPSwapSegmentEqual extends XFVRPOptImpBase {

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Node[] giantTour, Quality bestResult) {
		final Set<String> loadingFootprint = getLoadingFootprint(giantTour);

		List<float[]> improvingStepList = new ArrayList<>();

		if(model.getNbrOfDepots() > 1)
			throw new UnsupportedOperationException(this.getClass().getName()+" supports no multi depot");

		// Suche alle verbessernden L�sungen
		for (int a = 1; a < giantTour.length - 4; a++) {
			for (int b = a + 2; b < giantTour.length - 3; b++) {
				float val = findImprovements(giantTour, a, b, 1);
				if(val > epsilon) improvingStepList.add(new float[]{a, b, 1, val});
				val = findImprovements(giantTour, a, b, 2);
				if(val > epsilon) improvingStepList.add(new float[]{a, b, 2, val});
				val = findImprovements(giantTour, a, b, 3);
				if(val > epsilon) improvingStepList.add(new float[]{a, b, 3, val});
				val = findImprovements(giantTour, a, b, 4);
				if(val > epsilon) improvingStepList.add(new float[]{a, b, 4, val});
			}
		}
		
		// Sortier absteigend nach Potenzial
		sort(improvingStepList, 3);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			int a = (int) val[0];
			int b = (int) val[1];
			int l = (int) val[2];

			for (int i = 0; i <= l; i++)
				exchange(giantTour, a+i, b+i);

			Quality result = check(giantTour, loadingFootprint);
			if(result != null && result.getFitness() < bestResult.getFitness())
				return result;

			for (int i = 0; i <= l; i++)
				exchange(giantTour, a+i, b+i);
		}

		return null;
	}

	/**
	 * Calculates the improving potential of the starting point of first segment,
	 * the starting point of second segment and the length of both segments.
	 * 
	 * If first segment overlaps with second segment or second segment reaches end 
	 * of gianttour, the parameter are rejected by returning 0.   
	 * 
	 * @param giantRoute
	 * @param a
	 * @param b
	 * @param l
	 * @return
	 */
	private float findImprovements(Node[] giantTour, int a, int b, int l) {
		if(a + l >= b)
			return 0;

		if(b + l >= giantTour.length - 1)
			return 0;

		float val = 0;
		if((b - (a + l)) == 1) {
			val = getDistanceForOptimization(giantTour[a - 1], giantTour[a]) + 
			getDistanceForOptimization(giantTour[a + l], giantTour[b]) +
			getDistanceForOptimization(giantTour[b + l], giantTour[b + l + 1]);

			val -= getDistanceForOptimization(giantTour[a - 1], giantTour[b]) + 
			getDistanceForOptimization(giantTour[b + l], giantTour[a]) +
			getDistanceForOptimization(giantTour[a + l], giantTour[b + l + 1]);
		} else {
			val = getDistanceForOptimization(giantTour[a - 1], giantTour[a]) + 
			getDistanceForOptimization(giantTour[a + l], giantTour[a + l + 1]) +
			getDistanceForOptimization(giantTour[b - 1], giantTour[b]) +
			getDistanceForOptimization(giantTour[b + l], giantTour[b + l + 1]);

			val -= getDistanceForOptimization(giantTour[a - 1], giantTour[b]) + 
			getDistanceForOptimization(giantTour[b + l], giantTour[a + l + 1]) +
			getDistanceForOptimization(giantTour[b - 1], giantTour[a]) +
			getDistanceForOptimization(giantTour[a + l], giantTour[b + l + 1]);
		}

		return val;
	}
}
