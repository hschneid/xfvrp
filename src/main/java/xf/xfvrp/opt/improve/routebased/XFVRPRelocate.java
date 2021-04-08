package xf.xfvrp.opt.improve.routebased;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.XFVRPOptImpBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Contains the optimization algorithms
 * for relocate neighborhood. One customer node can
 * be moved to any other position in the 
 * route plan.
 *
 *
 * @author hschneid
 *
 */
public class XFVRPRelocate extends XFVRPOptImpBase {

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) throws XFVRPException {
		List<float[]> improvingStepList = new ArrayList<>();

		search(solution.getRoutes(), improvingStepList);

		// Sort the potentials descending
		sort(improvingStepList, 4);

		// Find first valid improvement
		for (float[] val : improvingStepList) {
			changeSolution(solution, val);

			Quality result = checkIt(solution, (int)val[0], (int)val[1]);
			if(result != null && result.getFitness() < bestResult.getFitness()) {
				solution.fixateQualities();
				return result;
			}

			resetSolution(solution, val);
			solution.resetQualities();
		}

		return null;
	}

	private void changeSolution(Solution solution, float[] parameter) throws XFVRPException {
		int srcRouteIdx = (int) parameter[0];
		int dstRouteIdx = (int) parameter[1];
		int srcPos = (int) parameter[2];
		int dstPos = (int) parameter[3];

		move2(solution, srcRouteIdx, dstRouteIdx, srcPos, dstPos);
	}

	private void resetSolution(Solution solution, float[] parameter) throws XFVRPException {
		int srcRouteIdx = (int) parameter[0];
		int dstRouteIdx = (int) parameter[1];
		int srcPos = (int) parameter[2];
		int dstPos = (int) parameter[3];

		if(srcRouteIdx != dstRouteIdx) {
			move2(solution, dstRouteIdx, srcRouteIdx, dstPos, srcPos);
		} else {
			if (srcPos > dstPos) {
				move2(solution, dstRouteIdx, srcRouteIdx, dstPos, srcPos + 1);
			} else {
				move2(solution, dstRouteIdx, srcRouteIdx, dstPos - 1, srcPos);
			}
		}
	}

	/**
	 * Searches all improving valid steps in search space
	 */
	private void search(Node[][] routes, List<float[]> improvingStepList) {
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

						float val = 0;
						// If dst is directly before src
						if(srcRtIdx == dstRtIdx && srcPos - dstPos == 1) {
							// Calculate distance for removed edges
							val += getDistanceForOptimization(srcRoute[srcPos]    , srcRoute[srcPos+1]);
							val += getDistanceForOptimization(srcRoute[dstPos]    , srcRoute[srcPos]);
							val += getDistanceForOptimization(srcRoute[dstPos - 1], srcRoute[dstPos]);
							// Calculate distance for added edges
							val -= getDistanceForOptimization(srcRoute[dstPos - 1], srcRoute[srcPos]);
							val -= getDistanceForOptimization(srcRoute[srcPos]    , srcRoute[dstPos]);
							val -= getDistanceForOptimization(srcRoute[dstPos]    , srcRoute[srcPos+1]);
						} else {
							// Calculate distance for removed edges
							val += getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos]);
							val += getDistanceForOptimization(srcRoute[srcPos]    , srcRoute[srcPos + 1]);
							val += getDistanceForOptimization(dstRoute[dstPos - 1], dstRoute[dstPos]);
							// Calculate distance for added edges
							val -= getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos + 1]);
							val -= getDistanceForOptimization(dstRoute[dstPos - 1], srcRoute[srcPos]);
							val -= getDistanceForOptimization(srcRoute[srcPos]    , dstRoute[dstPos]);
						}

						if(val > epsilon)
							improvingStepList.add(new float[]{srcRtIdx, dstRtIdx, srcPos, dstPos, val});
					}
				}
			}
		}
	}
}
