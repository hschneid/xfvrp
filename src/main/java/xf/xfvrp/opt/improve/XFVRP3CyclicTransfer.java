package xf.xfvrp.opt.improve;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.opt.Solution;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Experimental procedure for a cyclic transfer with 3 nodes
 * 
 * @author hschneid
 *
 */
public class XFVRP3CyclicTransfer extends XFVRPOptImpBase {

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	protected Quality improve(final Solution solution, Quality bestResult) {
		Node[] giantTour = solution.getGiantRoute();
		
		if(model.getNbrOfDepots() > 1)
			throw new UnsupportedOperationException(this.getClass().getName()+" supports no multi depot");
		
		List<float[]> improvingStepList = new ArrayList<>();

		// Suche alle verbessernden L�sungen
		for (int a = 1; a < giantTour.length - 1; a++) {
			for (int b = 1; b < giantTour.length - 1; b++) {
				if(a == b)
					continue;
				for (int c = 1; c < giantTour.length - 1; c++) {
					if(a == c || b == c)
						continue;

					float val = getDiff(giantTour, a, b, c);
					if(val > 0)
						improvingStepList.add(new float[]{a, b, c, val});
				}
			}
		}

		// Sortier absteigend nach Potenzial
		sort(improvingStepList, 3);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			int a = (int) val[0];
			int b = (int) val[1];
			int c = (int) val[2];

			exchange(solution, a,b);
			exchange(solution, a,c);

			Quality result = checkIt(solution);
			if(result != null && result.getFitness() < bestResult.getFitness())
				return result;

			exchange(solution, a, c);
			exchange(solution, a, b);
		}

		return null;
	}

	/**
	 * 
	 * @param giantRoute
	 * @param a
	 * @param b
	 * @param c
	 * @return
	 */
	private float getDiff(Node[] giantTour, int a, int b, int c) {
		int[] aa = new int[]{a-1, a, a+1, c};
		int[] bb = new int[]{b-1, b, b+1, a};
		int[] cc = new int[]{c-1, c, c+1, b};

		Set<Integer> set = new HashSet<>(3);
		set.add(a);
		set.add(b);
		set.add(c);

		int[] ff, ss, tt;
		if(a < b && a < c) {
			ff = aa;
			if(b < c) {
				ss = bb;
				tt = cc;
			} else {
				ss = cc;
				tt = bb;
			}
		} else if(b < a && b < c) {
			ff = bb;
			if(a < c) {
				ss = aa;
				tt = cc;
			} else {
				ss = cc;
				tt = aa;
			}
		} else {
			ff = cc;
			if(a < b) {
				ss = aa;
				tt = bb;
			} else {
				ss = bb;
				tt = aa;
			}
		}

		float val = 
			getDistanceForOptimization(giantTour[a], giantTour[a+1]) +
			getDistanceForOptimization(giantTour[b], giantTour[b+1]) +
			getDistanceForOptimization(giantTour[c], giantTour[c+1]) +
			(((a-1) != b && (a-1) != c) ? getDistanceForOptimization(giantTour[a-1], giantTour[a]) : 0) + 
			(((b-1) != a && (b-1) != c) ? getDistanceForOptimization(giantTour[b-1], giantTour[b]) : 0) + 
			(((c-1) != a && (c-1) != b) ? getDistanceForOptimization(giantTour[c-1], giantTour[c]) : 0);

		val -=  
			// START
			getDistanceForOptimization(giantTour[ff[0]], giantTour[ff[3]]) +
			// ZIEL
			getDistanceForOptimization(giantTour[tt[3]], giantTour[tt[2]]) +
			// First -> Next-First
			(					
					(!set.contains(ff[2])) ?
							getDistanceForOptimization(giantTour[ff[3]], giantTour[ff[2]]) :
								getDistanceForOptimization(giantTour[ff[3]], giantTour[ss[3]])
			) +
			// Pred-Second -> Second
			(
					(!set.contains(ss[0])) ?
							getDistanceForOptimization(giantTour[ss[0]], giantTour[ss[3]]) :
								0
			) +
			// Second -> Next-Second
			(
					(!set.contains(ss[2])) ?
							getDistanceForOptimization(giantTour[ss[3]], giantTour[ss[2]]) :
								getDistanceForOptimization(giantTour[ss[3]], giantTour[tt[3]])
			) +
			// Pred-Third -> Third
			(
					(!set.contains(tt[0])) ?
							getDistanceForOptimization(giantTour[tt[0]], giantTour[tt[3]]) :
								0
			);

		return val;
	}

}
