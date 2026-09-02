package xf.xfvrp.opt.improve.routebased.swap;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.routebased.XFVRPOptImpBase;

import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Copyright (c) 2012-2026 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * This neighborhood search produces improved solutions by
 * exchanging two segments. The size of each segments may be
 * limited upto 3 nodes. The nodes of a segment can be inverted.
 * <p>
 * Size of NS is O(k * n²), where k is segment size and nbr of invert types
 *
 * @author hschneid
 */
public class XFVRPSegmentSwap extends XFVRPOptImpBase {

    private boolean isInvertationActive = true;
    private boolean isSegmentLengthEqual = false;
    private final int maxSegmentLength = 3;

    @Override
    protected void searchRoutePair(Solution solution, Queue<float[]> queue, int routeIdxA, int routeIdxB) {
        XFVRPSwapSearchUtil.searchRoutePair(solution, queue, routeIdxA, routeIdxB, maxSegmentLength, isSegmentLengthEqual, isInvertationActive);
    }

    @Override
    protected Queue<float[]> search(Solution solution) {
        evaluateDirtyPairs(solution);
        PriorityQueue<float[]> queue = new PriorityQueue<>((o1, o2) -> Float.compare(o2[0], o1[0]));
        int nbrOfRoutes = solution.getRoutes().length;
        for (int a = 0; a < nbrOfRoutes; a++) {
            for (int b = a; b < nbrOfRoutes; b++) {
                collectPairMoves(a, b, queue);
            }
        }
        return queue;
    }

    @Override
    protected Node[][] change(Solution solution, float[] changeParameter) throws XFVRPException {
        return XFVRPSwapUtil.change(solution, changeParameter);
    }

    public void setInvertationMode(boolean isInvertationActive) {
        this.isInvertationActive = isInvertationActive;
    }

    public void setEqualSegmentLength(boolean isSegmentLengthEqual) {
        this.isSegmentLengthEqual = isSegmentLengthEqual;
    }

}