package xf.xfvrp.opt.improve.routebased.swap;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.routebased.XFVRPOptImpBase;
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveSearchUtil;
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveUtil;

import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This neighborhood search produces improved solutions by
 * exchanging or moving two segments. The size of each segments may be
 * limited up to 3 nodes. The nodes of a segment can be inverted.
 *
 * Size of NS is O(k * n²), where k is segment size and nbr of invert types
 *
 * @author hschneid
 *
 */
public class XFVRPSegmentExchange extends XFVRPOptImpBase {

	private boolean isInvertationActive = true;
	private boolean isSegmentLengthEqual = false;
	private int maxSegmentLength = 3;

	@Override
	protected Queue<float[]> search(Solution solution) {
		PriorityQueue<float[]> improvingSteps = new PriorityQueue<>(
				(o1, o2) -> Float.compare(o2[0], o1[0])
		);
		XFVRPMoveSearchUtil.search(solution, improvingSteps, maxSegmentLength, isInvertationActive);
		XFVRPSwapSearchUtil.search(solution, improvingSteps, maxSegmentLength, isSegmentLengthEqual, isInvertationActive);

		return improvingSteps;
	}

	@Override
	protected Node[][] change(Solution solution, float[] changeParameter) throws XFVRPException {
		if(changeParameter.length == 9) {
			return XFVRPSwapUtil.change(solution, changeParameter);
		} else if(changeParameter.length == 8) {
			return XFVRPMoveUtil.change(solution, changeParameter);
		}

		return null;
	}
}