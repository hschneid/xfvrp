package xf.xfvrp.opt.improve;

import java.util.ArrayList;
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
 * This neighborhood search means the 3-opt-edge exchange
 * NS from literature. A solution is improved by
 * exchanging three edges with three new edges.
 * 
 * The process is comparable by double inverting like the 2-opt.
 * 
 * @author hschneid
 *
 */
public class XFVRP3Opt extends XFVRPOptImpBase {

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) {
		final Set<String> loadingFootprint = getLoadingFootprint(solution);
		
		if(model.getNbrOfDepots() > 1)
			throw new UnsupportedOperationException(this.getClass().getName()+" supports no multi depot");
		
		Node[] giantTour = solution.getGiantRoute();
		List<float[]> improvingStepList = new ArrayList<>();

		// Suche alle verbessernden L�sungen
		for (int a = 1; a < giantTour.length - 5; a++) {
			for (int b = a + 2; b < giantTour.length - 3; b++) {
				for (int c = b + 2; c < giantTour.length - 1; c++) {
					findImprovements(giantTour, a, b, c, improvingStepList);
				}
			}
		}
		
		// Sortier absteigend nach Potenzial
		sort(improvingStepList, 4);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			int a = (int) val[0];
			int b = (int) val[1];
			int c = (int) val[2];
			int m = (int) val[3];

			swap3Opt(solution, a, b, c, m);
			
			Quality result = check(solution, loadingFootprint);
			if(result != null && result.getFitness() < bestResult.getFitness()) {
				return result;
			}

			m = revertMethod(m);
			swap3Opt(solution, a, b, c, m);
		}

		return null;
	}

	/**
	 * 
	 * @param giantTour
	 * @param a
	 * @param b
	 * @param c
	 * @param impList
	 */
	public void findImprovements(Node[] giantTour, int a, int b, int c, List<float[]> impList) {
		final float old = getDistanceForOptimization(giantTour[a], giantTour[a+1]) + 
		getDistanceForOptimization(giantTour[b], giantTour[b+1]) + 
		getDistanceForOptimization(giantTour[c], giantTour[c+1]);

		float val = 0;
		// Invert (b + 1 - c)
		{
			val = getDistanceForOptimization(giantTour[a], giantTour[a + 1]) +
			getDistanceForOptimization(giantTour[b], giantTour[c]) +
			getDistanceForOptimization(giantTour[b + 1], giantTour[c + 1]) - old;
			if(val < epsilon) impList.add(new float[]{a, b, c, 0, -val});
		}
		// Invert (a + 1 - b)
		{
			val = getDistanceForOptimization(giantTour[a], giantTour[b]) +
			getDistanceForOptimization(giantTour[a + 1], giantTour[b + 1]) +
			getDistanceForOptimization(giantTour[c], giantTour[c + 1]) - old;
			if(val < epsilon) impList.add(new float[]{a, b, c, 1, -val});
		}
		// Invert (a + 1 - b UND b + 1 - c)
		{
			val = getDistanceForOptimization(giantTour[a], giantTour[b]) +
			getDistanceForOptimization(giantTour[a + 1], giantTour[c]) +
			getDistanceForOptimization(giantTour[b + 1], giantTour[c + 1]) - old;
			if(val < epsilon) impList.add(new float[]{a, b, c, 2, -val});
		}
		// Invert (a + 1 - c UND a + 1 - b UND b + 1 - c)
		{
			val = getDistanceForOptimization(giantTour[a], giantTour[b + 1]) +
			getDistanceForOptimization(giantTour[c], giantTour[a + 1]) +
			getDistanceForOptimization(giantTour[b], giantTour[c + 1]) - old;
			if(val < epsilon) impList.add(new float[]{a, b, c, 3, -val});
		}
		// Invert (a + 1 - c UND a + 1 - b)
		{
			val = getDistanceForOptimization(giantTour[a], giantTour[b + 1]) +
			getDistanceForOptimization(giantTour[c], giantTour[b]) +
			getDistanceForOptimization(giantTour[a + 1], giantTour[c + 1]) - old;
			if(val < epsilon) impList.add(new float[]{a, b, c, 4, -val});
		}
		// Invert (a + 1 - c UND b + 1 - c)
		{
			val = getDistanceForOptimization(giantTour[a], giantTour[c]) +
			getDistanceForOptimization(giantTour[b + 1], giantTour[a + 1]) +
			getDistanceForOptimization(giantTour[b], giantTour[c + 1]) - old;
			if(val < epsilon) impList.add(new float[]{a, b, c, 5, -val});
		}
		// Invert (a + 1 - c)
		{
			val = getDistanceForOptimization(giantTour[a], giantTour[c]) +
			getDistanceForOptimization(giantTour[b], giantTour[b + 1]) +
			getDistanceForOptimization(giantTour[a + 1], giantTour[c + 1]) - old;
			if(val < epsilon) impList.add(new float[]{a, b, c, 6, -val});
		}
	}

	/**
	 * 
	 * @param giantRoute
	 * @param a
	 * @param b
	 * @param c
	 * @param m
	 */
	private void swap3Opt(Solution solution, int a, int b, int c, int m) {
		
		
		switch(m) {
		case 0 : {
			// Invert (b + 1 - c)
			swap(solution, b + 1, c);
			break;
		}
		case 1 : {
			// Invert (a + 1 - b)
			swap(solution, a + 1, b);
			break;
		}
		case 2 : {
			// Invert (a + 1 - b UND b + 1 - c)
			swap(solution, a + 1, b);
			swap(solution, b + 1, c);
			break;
		}
		case 3 : {
			// Invert (a + 1 - c UND a + 1 - b UND b + 1 - c)
			swap(solution, a + 1, b);
			swap(solution, b + 1, c);
			swap(solution, a + 1, c);
			break;
		}
		case 4 : {
			// Invert (a + 1 - c UND a + 1 - b)
			// There for first exchange b+1 - c AND then whole range
			swap(solution, b + 1, c);
			swap(solution, a + 1, c);
			break;
		}
		case 5 : {
			// Invert (a + 1 - c UND b + 1 - c)
			swap(solution, a + 1, b);
			swap(solution, a + 1, c);
			break;
		}
		case 6 : {
			// Invert (a + 1 - c)
			swap(solution, a + 1, c);
			break;
		}
		case 7 : {
			// Revert move of case 3
			swap(solution, a + 1, c);
			swap(solution, a + 1, b);
			swap(solution, b + 1, c);
			break;
		}
		case 8 : {
			// Revert move of case 4
			swap(solution, a + 1, c);
			swap(solution, b + 1, c);
			break;
		}
		case 9 : {
			// Revert move of case 5
			swap(solution, a + 1, c);
			swap(solution, a + 1, b);
			break;
		}
		default:
			// Nothing to do
			break;
		}
	}

	/**
	 * 
	 * @param m
	 * @return
	 */
	private int revertMethod(int m) {
		switch(m) {
		case 3 : {return 7;}
		case 4 : {return 8;}
		case 5 : {return 9;}
		default : {return m;}
		}
	}

}
