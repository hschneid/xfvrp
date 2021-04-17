package xf.xfvrp.opt.improve.routebased.exchange;

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
public class XFVRPSegmentExchange {

	private static final int EXCHANGE = 0;
	private static final int MOVE_B_TO_A = 1;
	private static final int MOVE_A_TO_B = 2;

	private static final int NO_INVERT = 0;
	private static final int A_INVERT = 1;
	private static final int B_INVERT = 2;
	private static final int BOTH_INVERT = 3;

	private boolean isInvertationActive = true;
	private boolean isSegmentLengthEqual = false;

//	/*
//	 * (non-Javadoc)
//	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
//	 */
//	@Override
//	public Quality improve(final Solution solution, Quality bestResult) throws XFVRPException {
//		Node[] giantRoute = solution.getGiantRoute();
//
//		List<float[]> improvingStepList = search(giantRoute);
//
//		// Sortier absteigend nach Potenzial
//		sort(improvingStepList, 5);
//
//		// Finde die erste valide verbessernde L�sung
//		for (float[] val : improvingStepList) {
//			change(solution, val);
//
//			Quality result = checkIt(solution);
//			if(result != null && result.getCost() < bestResult.getCost())
//				return result;
//
//			reverseChange(solution, val);
//		}
//
//		return null;
//	}
//
//	/**
//	 * Searches all improving steps in search space for a VRP.
//	 */
//	private PriorityQueue<float[]> search(Node[][] routes) {
//		PriorityQueue<float[]> improvingStepList = new PriorityQueue<>(
//				(o1, o2) -> Float.compare(o2[6], o1[6])
//		);
//
//		int nbrOfRoutes = routes.length;
//		boolean[] changeTypes = new boolean[3];
//		for (int aRtIdx = 0; aRtIdx < nbrOfRoutes; aRtIdx++) {
//			Node[] aRoute = routes[aRtIdx];
//			for (int bRtIdx = 0; bRtIdx < nbrOfRoutes; bRtIdx++) {
//				Node[] bRoute = routes[bRtIdx];
//				for (int aPos = 1; aPos < routes[aRtIdx].length; aPos++) {
//					// SETUP
//					int bRouteLength = routes[bRtIdx].length;
//					int aMaxSegmentLength = MAX_SEGMENT_LENGTH;
//					int bMaxSegmentLength = MAX_SEGMENT_LENGTH;
//					changeTypes[EXCHANGE] = true;
//					changeTypes[MOVE_A_TO_B] = true;
//					changeTypes[MOVE_B_TO_A] = true;
//
//					// If A is a depot
//					if(routes[aRtIdx][aPos].getSiteType() == SiteType.DEPOT) {
//						changeTypes[EXCHANGE] = false;
//						changeTypes[MOVE_A_TO_B] = false;
//						bRouteLength = routes[bRtIdx].length - 1;
//						aMaxSegmentLength = 1;
//					}
//
//					for (int bPos = 1; bPos < bRouteLength; bPos++) {
//						// If B is a depot
//						if(routes[aRtIdx][aPos].getSiteType() == SiteType.DEPOT) {
//							changeTypes[EXCHANGE] = false;
//							changeTypes[MOVE_A_TO_B] = false;
//							bMaxSegmentLength = 1;
//						}
//
//						for (int aSegmentLength = 0; aSegmentLength < aMaxSegmentLength; aSegmentLength++) {
//							for (int bSegmentLength = 0; bSegmentLength < bMaxSegmentLength; bSegmentLength++) {
//								// Not overlapping --> change type not possible --> abort
//								// A and B must not overlap in general
//								// Segment must not contain depot
//								searchInRoutes();
//							}
//						}
//					}
//				}
//			}
//		}
//
//		int length = routes.length - 1;
//		for (int a = 1; a < length; a++) {
//			for (int b = a + 1; b < length; b++) {
//				for (int la = 0; la < 4; la++) {
//					if(a + la >= length)
//						continue;
//
//					for (int lb = 0; lb < 4; lb++) {
//						if(b + lb >= length)
//							continue;
//						if(isSegmentLengthEqual && la != lb)
//							continue;
//
//						findImprovements(giantRoute, depotMarkArr, a, b, la, lb, improvingStepList);
//					}
//				}
//			}
//		}
//
//		return improvingStepList;
//	}
//
//	private void searchInRoutes(
//			Node[] aRoute,
//			Node[] bRoute,
//			int aRtIdx,
//			int bRtIdx,
//			int aPos,
//			int bPos,
//			int aSegmentLength,
//			int bSegmentLength,
//			boolean[] changeTypes,
//			PriorityQueue<float[]> improvingStepList
//	) {
//		// Equal segment exchange
//	}
//
//	private void findImprovements(Node[] giantRoute, int[] depotMarkArr, int a, int b, int la, int lb, List<float[]> impList) {
//		int aa = a + la;
//		int bb = b + lb;
//
//		if (aa >= b)
//			return;
//
//		int markA = depotMarkArr[a];
//		int markB = depotMarkArr[b - 1];
//
//		float old;
//		if ((b - aa) == 1) {
//			old = getDistance(giantRoute[a - 1], giantRoute[a], markA) +
//					getDistance(giantRoute[aa], giantRoute[b], markA) +
//					getDistance(giantRoute[bb], giantRoute[bb + 1], markB);
//		} else {
//			old = getDistance(giantRoute[a - 1], giantRoute[a], markA) +
//					getDistance(giantRoute[aa], giantRoute[aa + 1], markA) +
//					getDistance(giantRoute[b - 1], giantRoute[b], markB) +
//					getDistance(giantRoute[bb], giantRoute[bb + 1], markB);
//		}
//
//		float val;
//		// NO INVERT
//		{
//			if ((b - aa) == 1) {
//				val = old - (getDistance(giantRoute[a - 1], giantRoute[b], markA) +
//						getDistance(giantRoute[bb], giantRoute[a], markA) +
//						getDistance(giantRoute[aa], giantRoute[bb + 1], markB));
//			} else {
//				val = old - (getDistance(giantRoute[a - 1], giantRoute[b], markA) +
//						getDistance(giantRoute[bb], giantRoute[aa + 1], markA) +
//						getDistance(giantRoute[b - 1], giantRoute[a], markB) +
//						getDistance(giantRoute[aa], giantRoute[bb + 1], markB));
//			}
//			if(val > epsilon)
//				impList.add(new float[]{a, b, la, lb, NO_INVERT, val});
//		}
//		// BOTH INVERT
//		if(isInvertationActive) {
//			if((b - aa) == 1) {
//				val = old - (getDistance(giantRoute[a - 1], giantRoute[bb], markA) +
//						getDistance(giantRoute[b], giantRoute[aa], markA) +
//						getDistance(giantRoute[a], giantRoute[bb + 1], markB));
//			} else {
//				val = old - (getDistance(giantRoute[a - 1], giantRoute[bb], markA) +
//						getDistance(giantRoute[b], giantRoute[aa + 1], markA) +
//						getDistance(giantRoute[b - 1], giantRoute[aa], markB) +
//						getDistance(giantRoute[a], giantRoute[bb + 1], markB));
//			}
//			if(val > epsilon)
//				impList.add(new float[]{a, b, la, lb, BOTH_INVERT, val});
//		}
//		// A INVERT
//		if(isInvertationActive) {
//			if((b - aa) == 1) {
//				val = old - (getDistance(giantRoute[a - 1], giantRoute[b], markA) +
//						getDistance(giantRoute[bb], giantRoute[aa],markA) +
//						getDistance(giantRoute[a], giantRoute[bb + 1], markB));
//			} else {
//				val = old - (getDistance(giantRoute[a - 1], giantRoute[b], markA) +
//						getDistance(giantRoute[bb], giantRoute[aa + 1], markA) +
//						getDistance(giantRoute[b - 1], giantRoute[aa], markB) +
//						getDistance(giantRoute[a], giantRoute[bb + 1], markB));
//			}
//			if(val > epsilon)
//				impList.add(new float[]{a, b, la, lb, A_INVERT, val});
//		}
//		// B INVERT
//		if(isInvertationActive) {
//			if((b - aa) == 1) {
//				val = old - (getDistance(giantRoute[a - 1], giantRoute[bb], markA) +
//						getDistance(giantRoute[b], giantRoute[a], markA) +
//						getDistance(giantRoute[aa], giantRoute[bb + 1], markB));
//			} else {
//				val = old - (getDistance(giantRoute[a - 1], giantRoute[bb], markA) +
//						getDistance(giantRoute[b], giantRoute[aa + 1], markA) +
//						getDistance(giantRoute[b - 1], giantRoute[a], markB) +
//						getDistance(giantRoute[aa], giantRoute[bb + 1], markB));
//			}
//			if(val > epsilon)
//				impList.add(new float[]{a, b, la, lb, B_INVERT, val});
//		}
//	}
//
//	private void change(Solution solution, float[] val) throws XFVRPException {
//		int a = (int) val[0];
//		int b = (int) val[1];
//		int l = (int) val[2];
//		int ll = (int) val[3];
//		int i = (int) val[4];
//
//		invert(solution, a, b, l, ll, i);
//		exchange(solution, a, b, l, ll);
//	}
//
//	private void reverseChange(Solution solution, float[] val) throws XFVRPException {
//		int a = (int) val[0];
//		int b = (int) val[1];
//		int l = (int) val[2];
//		int ll = (int) val[3];
//		int i = (int) val[4];
//
//		exchange(solution, a, b + ((ll + 1) - (l + 1)), ll, l);
//		invert(solution, a, b, l, ll, i);
//	}
//
//	private void invert(Solution solution, int a, int b, int la, int lb, int i) {
//		switch (i) {
//			case A_INVERT: {
//				swap(solution, a, a + la);
//				break;
//			}
//			case B_INVERT: {
//				swap(solution, b, b + lb);
//				break;
//			}
//			case BOTH_INVERT: {
//				swap(solution, a, a + la);
//				swap(solution, b, b + lb);
//				break;
//			}
//			default:
//				// NO_INVERT
//				break;
//		}
//	}
//
//	public void setInvertationMode(boolean isInvertationActive) {
//		this.isInvertationActive = isInvertationActive;
//	}
//
//	public void setEqualSegmentLength(boolean isSegmentLengthEqual) {
//		this.isSegmentLengthEqual = isSegmentLengthEqual;
//	}

}