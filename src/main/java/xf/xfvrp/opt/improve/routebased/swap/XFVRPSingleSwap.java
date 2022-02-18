package xf.xfvrp.opt.improve.routebased.swap;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.routebased.XFVRPOptImpBase;

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
 * exchanging two nodes. Size of NS is O(n²)
 *
 * @author hschneid
 *
 */
public class XFVRPSingleSwap extends XFVRPOptImpBase {

	private boolean isInvertationActive = false;
	private boolean isSegmentLengthEqual = false;
	private int maxSegmentLength = 1;

	@Override
	protected Queue<float[]> search(Solution solution) {
		Node[][] routes = solution.getRoutes();

		PriorityQueue<float[]> improvingSteps = new PriorityQueue<>(
				(o1, o2) -> Float.compare(o2[0], o1[0])
		);
		XFVRPSwapSearchUtil.search(model, routes, improvingSteps, maxSegmentLength, isSegmentLengthEqual, isInvertationActive);

		return improvingSteps;
	}

	@Override
	protected void change(Solution solution, float[] changeParameter) throws XFVRPException {
		XFVRPSwapUtil.change(solution, changeParameter);
	}

	@Override
	protected void reverseChange(Solution solution, float[] changeParameter) throws XFVRPException {
		XFVRPSwapUtil.reverseChange(solution, changeParameter);
	}

	public void setInvertationMode(boolean isInvertationActive) {
		this.isInvertationActive = isInvertationActive;
	}

	public void setEqualSegmentLength(boolean isSegmentLengthEqual) {
		this.isSegmentLengthEqual = isSegmentLengthEqual;
	}

}