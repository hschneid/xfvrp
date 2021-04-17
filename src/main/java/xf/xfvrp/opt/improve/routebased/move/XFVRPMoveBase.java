package xf.xfvrp.opt.improve.routebased.move;

import xf.xfvrp.base.Quality;
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

	private final int maxSegmentLength;
	private final boolean isInvertationActive;

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

		PriorityQueue<float[]> improvingSteps = new PriorityQueue<>(
                (o1, o2) -> Float.compare(o2[6], o1[6])
        );
		XFVRPMoveSearchUtil.search(model, solution.getRoutes(), improvingSteps, maxSegmentLength, isInvertationActive);

		// Find first valid improving change
		while(!improvingSteps.isEmpty()) {
			float[] val = improvingSteps.remove();

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
}
