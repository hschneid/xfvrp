package xf.xfvrp.opt.improve;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
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
 * This neighborhood search produces improved solutions by
 * exchanging two segments of the giant tour. The size of each
 * segments may be between 1 and 4. All nodes of a segment must 
 * be placed on one route.
 * 
 * @author hschneid
 *
 */
public class XFVRPSwapSegmentWithInvert extends XFVRPOptImpBase {

	private static final int NO_INVERT = 0;
	private static final int A_INVERT = 1;
	private static final int B_INVERT = 2;
	private static final int BOTH_INVERT = 3;

	private boolean isInvertationActive = true;
	private boolean isSegmentLengthEqual = false;

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) {
		Node[] giantRoute = solution.getGiantRoute();

		List<float[]> improvingStepList = search(giantRoute);

		// Sortier absteigend nach Potenzial
		sort(improvingStepList, 5);

		// Finde die erste valide verbessernde L�sung
		for (float[] val : improvingStepList) {
			change(solution, val);

			Quality result = checkIt(solution);
			if(result != null && result.getCost() < bestResult.getCost())
				return result;

			reverseChange(solution, val);
		}

		return null;
	}

	/**
	 * Searches all improving valid steps in search space for
	 * a VRP with multiple depots.
	 * 
	 * @param giantRoute
	 * @return improvingStepList
	 */
	private List<float[]> search(Node[] giantRoute) {
		List<float[]> improvingStepList = new ArrayList<>();

		// Preparation
		int[] tourIdMarkArr = new int[giantRoute.length];
		int[] depotMarkArr = new int[giantRoute.length];
		int lastDepotIdx = 0;
		int id = 0;
		for (int i = 1; i < tourIdMarkArr.length; i++) {
			if(giantRoute[i].getSiteType() == SiteType.DEPOT) {
				id++;
				lastDepotIdx = giantRoute[i].getIdx();
			}
			tourIdMarkArr[i] = id;
			depotMarkArr[i] = lastDepotIdx;
		}

		// Search improving steps
		int length = giantRoute.length - 1;
		for (int a = 1; a < length; a++) {
			for (int b = a + 1; b < length; b++) {
				for (int la = 0; la < 4; la++) {
					if(a + la >= length)
						continue;
					if(tourIdMarkArr[a] != tourIdMarkArr[a+la])
						continue;

					for (int lb = 0; lb < 4; lb++) {
						if(b + lb >= length)
							continue;
						if(tourIdMarkArr[b] != tourIdMarkArr[b+lb])
							continue;
						if(isSegmentLengthEqual && la != lb)
							continue;

						findImprovements(giantRoute, depotMarkArr, a, b, la, lb, improvingStepList);
					}
				}
			}
		}

		return improvingStepList;
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
	private void findImprovements(Node[] giantRoute, int[] depotMarkArr, int a, int b, int la, int lb, List<float[]> impList) {
		int aa = a + la;
		int bb = b + lb;

		if (aa >= b)
			return;

		int markA = depotMarkArr[a];
		int markB = depotMarkArr[b - 1];

		float old;
		if ((b - aa) == 1) {
			old = getDistance(giantRoute[a - 1], giantRoute[a], markA) +
					getDistance(giantRoute[aa], giantRoute[b], markA) +
					getDistance(giantRoute[bb], giantRoute[bb + 1], markB);
		} else {
			old = getDistance(giantRoute[a - 1], giantRoute[a], markA) +
					getDistance(giantRoute[aa], giantRoute[aa + 1], markA) +
					getDistance(giantRoute[b - 1], giantRoute[b], markB) +
					getDistance(giantRoute[bb], giantRoute[bb + 1], markB);
		}

		float val;
		// NO INVERT
		{
			if ((b - aa) == 1) {
				val = old - (getDistance(giantRoute[a - 1], giantRoute[b], markA) +
						getDistance(giantRoute[bb], giantRoute[a], markA) +
						getDistance(giantRoute[aa], giantRoute[bb + 1], markB));
			} else {
				val = old - (getDistance(giantRoute[a - 1], giantRoute[b], markA) +
						getDistance(giantRoute[bb], giantRoute[aa + 1], markA) +
						getDistance(giantRoute[b - 1], giantRoute[a], markB) +
						getDistance(giantRoute[aa], giantRoute[bb + 1], markB));
			}
			if(val > epsilon) 
				impList.add(new float[]{a, b, la, lb, NO_INVERT, val});
		}
		// BOTH INVERT
		if(isInvertationActive) {	
			if((b - aa) == 1) {
				val = old - (getDistance(giantRoute[a - 1], giantRoute[bb], markA) + 
						getDistance(giantRoute[b], giantRoute[aa], markA) +
						getDistance(giantRoute[a], giantRoute[bb + 1], markB));
			} else {
				val = old - (getDistance(giantRoute[a - 1], giantRoute[bb], markA) + 
						getDistance(giantRoute[b], giantRoute[aa + 1], markA) +
						getDistance(giantRoute[b - 1], giantRoute[aa], markB) +
						getDistance(giantRoute[a], giantRoute[bb + 1], markB));
			}
			if(val > epsilon) 
				impList.add(new float[]{a, b, la, lb, BOTH_INVERT, val});
		}
		// A INVERT
		if(isInvertationActive) {
			if((b - aa) == 1) {
				val = old - (getDistance(giantRoute[a - 1], giantRoute[b], markA) + 
						getDistance(giantRoute[bb], giantRoute[aa],markA) +
						getDistance(giantRoute[a], giantRoute[bb + 1], markB));
			} else {
				val = old - (getDistance(giantRoute[a - 1], giantRoute[b], markA) + 
						getDistance(giantRoute[bb], giantRoute[aa + 1], markA) +
						getDistance(giantRoute[b - 1], giantRoute[aa], markB) +
						getDistance(giantRoute[a], giantRoute[bb + 1], markB));
			}
			if(val > epsilon) 
				impList.add(new float[]{a, b, la, lb, A_INVERT, val});
		}
		// B INVERT
		if(isInvertationActive) {
			if((b - aa) == 1) {
				val = old - (getDistance(giantRoute[a - 1], giantRoute[bb], markA) + 
						getDistance(giantRoute[b], giantRoute[a], markA) +
						getDistance(giantRoute[aa], giantRoute[bb + 1], markB));
			} else {
				val = old - (getDistance(giantRoute[a - 1], giantRoute[bb], markA) + 
						getDistance(giantRoute[b], giantRoute[aa + 1], markA) +
						getDistance(giantRoute[b - 1], giantRoute[a], markB) +
						getDistance(giantRoute[aa], giantRoute[bb + 1], markB));
			}
			if(val > epsilon) 
				impList.add(new float[]{a, b, la, lb, B_INVERT, val});
		}
	}

	private void change(Solution solution, float[] val) {
		int a = (int) val[0];
		int b = (int) val[1];
		int l = (int) val[2];
		int ll = (int) val[3];
		int i = (int) val[4];

		invert(solution, a, b, l, ll, i);
		exchange(solution, a, b, l, ll);
	}

	private void reverseChange(Solution solution, float[] val) {
		int a = (int) val[0];
		int b = (int) val[1];
		int l = (int) val[2];
		int ll = (int) val[3];
		int i = (int) val[4];

		exchange(solution, a, b + ((ll + 1) - (l + 1)), ll, l);
		invert(solution, a, b, l, ll, i);
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

	public void setInvertationMode(boolean isInvertationActive) {
		this.isInvertationActive = isInvertationActive;
	}
	
	public void setEqualSegmentLength(boolean isSegmentLengthEqual) {
		this.isSegmentLengthEqual = isSegmentLengthEqual;
	}

}