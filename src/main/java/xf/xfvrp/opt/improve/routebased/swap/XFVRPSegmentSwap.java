package xf.xfvrp.opt.improve.routebased.swap;

import xf.xfvrp.base.Quality;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.routebased.XFVRPOptImpBase;

import java.util.PriorityQueue;

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
public class XFVRPSegmentSwap extends XFVRPOptImpBase {

	private boolean isInvertationActive = true;
	private boolean isSegmentLengthEqual = false;
	private int maxSegmentLength = 3;

	/*
	 * (non-Javadoc)
	 * @see de.fhg.iml.vlog.xftour.xfvrp.opt.improve.XFVRPOptImpBase#improve(de.fhg.iml.vlog.xftour.model.XFNode[], de.fhg.iml.vlog.xftour.model.Quality)
	 */
	@Override
	public Quality improve(final Solution solution, Quality bestResult) throws XFVRPException {
		checkIt(solution);

		PriorityQueue<float[]> improvingSteps = new PriorityQueue<>(
				(o1, o2) -> Float.compare(o2[7], o1[7])
		);
		XFVRPSwapSearchUtil.search(model, solution.getRoutes(), improvingSteps, maxSegmentLength, isSegmentLengthEqual, isInvertationActive);

		// Find first valid improving change
		while(!improvingSteps.isEmpty()) {
			float[] val = improvingSteps.remove();

			// Variation
			XFVRPSwapUtil.change(solution, val);

			Quality result = checkIt(solution, (int)val[0], (int)val[1]);
			if(result != null && result.getFitness() < bestResult.getFitness()) {
				solution.fixateQualities();
				return result;
			}

			// Reverse-Variation
			XFVRPSwapUtil.reverseChange(solution, val);
			solution.resetQualities();
		}

		return null;
	}

	public void setInvertationMode(boolean isInvertationActive) {
		this.isInvertationActive = isInvertationActive;
	}

	public void setEqualSegmentLength(boolean isSegmentLengthEqual) {
		this.isSegmentLengthEqual = isSegmentLengthEqual;
	}

}