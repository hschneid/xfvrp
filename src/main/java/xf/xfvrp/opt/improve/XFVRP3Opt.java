package xf.xfvrp.opt.improve;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.opt.Solution;

import java.util.ArrayList;
import java.util.List;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This neighborhood search means the 3-opt-edge exchange
 * neighborhood search from literature. A solution is improved by
 * exchanging three edges with three new edges.
 * 
 * The process is comparable by inverting two segments (like double the 2-opt).
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
		if(model.getNbrOfDepots() > 1)
			throw new UnsupportedOperationException(this.getClass().getName()+" supports no multi depot");
		
		Node[] giantTour = solution.getGiantRoute();

		// Suche alle verbessernden L�sungen
		List<float[]> improvingStepList = search(giantTour);
		
		// Sortier absteigend nach Potenzial
		sort(improvingStepList, 4);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			change(solution, val);
			
			Quality result = checkIt(solution);
			if(result != null && result.getFitness() < bestResult.getFitness()) {
				return result;
			}

			reverseChange(solution, val);
		}

		return null;
	}
	
	private void change(Solution solution, float[] val) {
		int a = (int) val[0];
		int b = (int) val[1];
		int c = (int) val[2];
		int m = (int) val[3];

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
			// Invert (a + 1 - c UND b + 1 - c)
			// So, first exchange (b + 1 - c) AND then whole range
			swap(solution, b + 1, c);
			swap(solution, a + 1, c);
			break;
		}
		case 5 : {
			// Invert (a + 1 - c UND a + 1 - b)
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

	private void reverseChange(Solution solution, float[] val) {
		val[3] = modifyForReverseChange((int) val[3]);
		change(solution, val);
	}

	private List<float[]> search(Node[] giantRoute) {
		List<float[]> improvingStepList = new ArrayList<>();
		
		for (int a = 1; a < giantRoute.length - 5; a++) {
			for (int b = a; b < giantRoute.length - 3; b++) {
				for (int c = b; c < giantRoute.length - 1; c++) {
					findImprovements(giantRoute, a, b, c, improvingStepList);
				}
			}
		}
		
		return improvingStepList;
	}

	/**
	 * 
	 * @param giantTour
	 * @param a
	 * @param b
	 * @param c
	 * @param impList
	 */
	private void findImprovements(Node[] giantTour, int a, int b, int c, List<float[]> impList) {


		final float old = getDistanceForOptimization(giantTour[a], giantTour[a + 1]) +
				getDistanceForOptimization(giantTour[b], giantTour[b + 1]) +
				getDistanceForOptimization(giantTour[c], giantTour[c + 1]);

		float val;
		// Invert (b + 1 - c)
		if (c - b > 1) {
			val = old - (getDistanceForOptimization(giantTour[a], giantTour[a + 1]) +
					getDistanceForOptimization(giantTour[b], giantTour[c]) +
					getDistanceForOptimization(giantTour[b + 1], giantTour[c + 1]));
			if (val > epsilon) impList.add(new float[]{a, b, c, 0, val});
		}
		// Invert (a + 1 - b)
		if (b - a > 1) {
			val = old - (getDistanceForOptimization(giantTour[a], giantTour[b]) +
			getDistanceForOptimization(giantTour[a + 1], giantTour[b + 1]) +
			getDistanceForOptimization(giantTour[c], giantTour[c + 1]));
			if(val > epsilon) impList.add(new float[]{a, b, c, 1, val});
		}
		// Invert (a + 1 - b UND b + 1 - c)
		if(b - a > 1 && c - b > 1) {
			val = old - (getDistanceForOptimization(giantTour[a], giantTour[b]) +
			getDistanceForOptimization(giantTour[a + 1], giantTour[c]) +
			getDistanceForOptimization(giantTour[b + 1], giantTour[c + 1]));
			if(val > epsilon) impList.add(new float[]{a, b, c, 2, val});
		}
		// Invert (a + 1 - c UND a + 1 - b UND b + 1 - c)
		if(b - a > 1 && c - b > 1) {
			val = old - (getDistanceForOptimization(giantTour[a], giantTour[b + 1]) +
			getDistanceForOptimization(giantTour[c], giantTour[a + 1]) +
			getDistanceForOptimization(giantTour[b], giantTour[c + 1]));
			if(val > epsilon) impList.add(new float[]{a, b, c, 3, val});
		}
		// Invert (a + 1 - c UND a + 1 - b)
		if(b - a > 1) {
			val = old - (getDistanceForOptimization(giantTour[a], giantTour[b + 1]) +
			getDistanceForOptimization(giantTour[c], giantTour[b]) +
			getDistanceForOptimization(giantTour[a + 1], giantTour[c + 1]));
			if(val > epsilon) impList.add(new float[]{a, b, c, 4, val});
		}
		// Invert (a + 1 - c UND b + 1 - c)
		if(c - b > 1) {
			val = old - (getDistanceForOptimization(giantTour[a], giantTour[c]) +
			getDistanceForOptimization(giantTour[b + 1], giantTour[a + 1]) +
			getDistanceForOptimization(giantTour[b], giantTour[c + 1]));
			if(val > epsilon) impList.add(new float[]{a, b, c, 5, val});
		}
		// Invert (a + 1 - c)
		if(c - a > 1) {
			val = old - (getDistanceForOptimization(giantTour[a], giantTour[c]) +
			getDistanceForOptimization(giantTour[b + 1], giantTour[b]) +
			getDistanceForOptimization(giantTour[a + 1], giantTour[c + 1]));
			if(val > epsilon) impList.add(new float[]{a, b, c, 6, val});
		}
	}

	/**
	 * 
	 * @param m
	 * @return
	 */
	private int modifyForReverseChange(int m) {
		switch(m) {
		case 3 : {return 7;}
		case 4 : {return 8;}
		case 5 : {return 9;}
		default : {return m;}
		}
	}

}
