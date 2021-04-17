package xf.xfvrp.opt.improve.routebased.move;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.XFVRPOptImpBase;

import java.util.PriorityQueue;

/**
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * The Segment Move neighborhood operator removes from a route
 * a segment and inserts it at a specific position in same or other route.
 *
 * To improve the performance of this huge neighborhood search O(n^3),
 * the length of the segments is restricted up to 4. Longer segments
 * than 4 are not relocated.
 *
 * As expansion of standard segment relocation a chosen segment can be
 * additionally inverted in the ordering of nodes.
 *
 * @author hschneid
 *
 */
abstract class XFVRPMoveBase extends XFVRPOptImpBase {

	public static final int NO_INVERT = 0;
	public static final int INVERT = 1;

	private int maxSegmentLength = 3;
	private boolean isInvertationActive = true;

	public XFVRPMoveBase(int maxSegmentLength, boolean isInvertationActive) {
		this.maxSegmentLength = maxSegmentLength;
		this.isInvertationActive = isInvertationActive;
	}

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) throws XFVRPException {
		checkIt(solution);

		PriorityQueue<float[]> improvingStepList = search(solution.getRoutes());

		// Find first valid improving change
		while(!improvingStepList.isEmpty()) {
			float[] val = improvingStepList.remove();

			// Variation
			XFVRPMoveUtil.change(solution, val);

			Quality result = checkIt(solution, (int)val[0], (int)val[1]);
			if(result != null && result.getFitness() < bestResult.getFitness()) {
				solution.fixateQualities();
				return result;
			}

			// Reverse-Variation
			XFVRPMoveUtil.reverseChange(solution, val);
			solution.resetQualities();
		}

		return null;
	}

	/**
	 * Searches all improving steps in search space for a VRP.
	 */
	protected PriorityQueue<float[]> search(Node[][] routes) {
		PriorityQueue<float[]> improvingStepList = new PriorityQueue<>(
				(o1, o2) -> Float.compare(o2[6], o1[6])
		);

		int nbrOfRoutes = routes.length;
		for (int srcRtIdx = 0; srcRtIdx < nbrOfRoutes; srcRtIdx++) {
			Node[] srcRoute = routes[srcRtIdx];
			for (int dstRtIdx = 0; dstRtIdx < nbrOfRoutes; dstRtIdx++) {
				Node[] dstRoute = routes[dstRtIdx];
				for (int srcPos = 1; srcPos < routes[srcRtIdx].length - 1; srcPos++) {
					for (int dstPos = 1; dstPos < routes[dstRtIdx].length; dstPos++) {
						// src node must not be a depot
						if(routes[srcRtIdx][srcPos].getSiteType() == SiteType.DEPOT)
							continue;
						if(srcRtIdx == dstRtIdx && (srcPos == dstPos || dstPos - srcPos == 1)) {
							continue;
						}

						for (int segmentLength = 0; segmentLength < maxSegmentLength; segmentLength++) {
							// src segment must not too big for src route
							if((srcPos + segmentLength) > srcRoute.length - 2) {
								break;
							}
							// Dst must not lay in the segment or directly behind it (no-move)
							if(srcRoute == dstRoute && dstPos <= srcPos + segmentLength + 1 && dstPos >= srcPos) {
								break;
							}

							searchInRoutes(srcRoute, dstRoute, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, improvingStepList);
						}
					}
				}
			}
		}

		return improvingStepList;
	}

	private void searchInRoutes(Node[] srcRoute, Node[] dstRoute, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, PriorityQueue<float[]> improvingStepList) {
		// dstPos is directly before src
		if(srcRtIdx == dstRtIdx && srcPos - dstPos == 1) {
			searchWithDstBefore(srcRoute, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, improvingStepList);
		} else {
			searchNormal(srcRoute, dstRoute, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, improvingStepList);
		}
	}

	private void searchNormal(Node[] srcRoute, Node[] dstRoute, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, PriorityQueue<float[]> improvingStepList) {
		float old = getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos]) +
				getDistanceForOptimization(srcRoute[srcPos + segmentLength], srcRoute[srcPos + segmentLength + 1]) +
				getDistanceForOptimization(dstRoute[dstPos - 1], dstRoute[dstPos]);

		// No invert
		float val =
				old -
						(getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos + segmentLength + 1]) +
								getDistanceForOptimization(dstRoute[dstPos - 1], srcRoute[srcPos]) +
								getDistanceForOptimization(srcRoute[srcPos + segmentLength], dstRoute[dstPos]));
		if (val > epsilon) improvingStepList.add(new float[]{srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, NO_INVERT, val});

		// with invert
		if (isInvertationActive && segmentLength > 0) {
			val =
					old -
							(getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos + segmentLength + 1]) +
									getDistanceForOptimization(dstRoute[dstPos - 1], srcRoute[srcPos + segmentLength]) +
									getDistanceForOptimization(srcRoute[srcPos], dstRoute[dstPos]));
			if (val > epsilon) improvingStepList.add(new float[]{srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, INVERT, val});
		}
	}

	private void searchWithDstBefore(Node[] route, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, PriorityQueue<float[]> improvingStepList) {
		float old =
				getDistanceForOptimization(route[dstPos - 1], route[dstPos]) +
						getDistanceForOptimization(route[dstPos], route[srcPos]) +
						getDistanceForOptimization(route[srcPos + segmentLength], route[srcPos + segmentLength + 1]);

		// No invert
		float val =
				old -
						(getDistanceForOptimization(route[dstPos - 1], route[srcPos]) +
								getDistanceForOptimization(route[srcPos + segmentLength], route[dstPos]) +
								getDistanceForOptimization(route[dstPos], route[srcPos + segmentLength + 1]));
		if (val > epsilon) improvingStepList.add(new float[]{srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, NO_INVERT, val});

		// with invert
		if (isInvertationActive && segmentLength > 0) {
			val =
					old -
							(getDistanceForOptimization(route[dstPos - 1], route[srcPos + segmentLength]) +
									getDistanceForOptimization(route[srcPos], route[dstPos]) +
									getDistanceForOptimization(route[dstPos], route[srcPos + segmentLength + 1]));
			if (val > epsilon) improvingStepList.add(new float[]{srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, INVERT, val});
		}
	}
}
