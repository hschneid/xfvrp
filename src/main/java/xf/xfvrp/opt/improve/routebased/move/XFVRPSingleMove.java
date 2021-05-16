package xf.xfvrp.opt.improve.routebased.move;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.routebased.XFVRPOptImpBase;

import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Copyright (c) 2012-2021 Holger Schneider
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
 * @author hschneid
 *
 */
public class XFVRPSingleMove extends XFVRPOptImpBase {

	private static final int MAX_SEGMENT_LENGTH = 1;
	private static final boolean IS_INVERT_ACTIVE = false;

	@Override
	protected Queue<float[]> search(Node[][] routes) {
		PriorityQueue<float[]> improvingSteps = new PriorityQueue<>(
				(o1, o2) -> Float.compare(o2[0], o1[0])
		);
		XFVRPMoveSearchUtil.search(model, routes, improvingSteps, MAX_SEGMENT_LENGTH, IS_INVERT_ACTIVE);

		return improvingSteps;
	}

	@Override
	protected void change(Solution solution, float[] changeParameter) throws XFVRPException {
		XFVRPMoveUtil.change(solution, changeParameter);
	}

	@Override
	protected void reverseChange(Solution solution, float[] changeParameter) throws XFVRPException {
		XFVRPMoveUtil.reverseChange(solution, changeParameter);
	}
}
