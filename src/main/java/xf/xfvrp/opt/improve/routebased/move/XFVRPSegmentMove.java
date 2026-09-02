package xf.xfvrp.opt.improve.routebased.move;

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
 * The Segment Move neighborhood operator removes from a route
 * a segment and inserts it at a specific position in same or other route.
 * <p>
 * To improve the performance of this huge neighborhood search O(n^3),
 * the length of the segments is restricted up to 4. Longer segments
 * than 4 are not relocated.
 * <p>
 * As expansion of standard segment relocation a chosen segment can be
 * additionally inverted in the ordering of nodes.
 *
 * @author hschneid
 */
public class XFVRPSegmentMove extends XFVRPOptImpBase {

    private static final int MAX_SEGMENT_LENGTH = 3;
    private final boolean IS_INVERT_ACTIVE;

    public XFVRPSegmentMove() {
        IS_INVERT_ACTIVE = true;
    }

    public XFVRPSegmentMove(boolean isInvertActive) {
        IS_INVERT_ACTIVE = isInvertActive;
    }

    @Override
    protected void searchRoutePair(Solution solution, Queue<float[]> queue, int routeIdxA, int routeIdxB) {
        XFVRPMoveSearchUtil.searchDirectedRoutePair(solution, queue, routeIdxA, routeIdxB, MAX_SEGMENT_LENGTH, IS_INVERT_ACTIVE);
        if (routeIdxA != routeIdxB) {
            XFVRPMoveSearchUtil.searchDirectedRoutePair(solution, queue, routeIdxB, routeIdxA, MAX_SEGMENT_LENGTH, IS_INVERT_ACTIVE);
        }
    }

    @Override
    protected Queue<float[]> search(Solution solution) {
        evaluateDirtyPairs(solution);
        PriorityQueue<float[]> queue = new PriorityQueue<>((o1, o2) -> Float.compare(o2[0], o1[0]));
        int nbrOfRoutes = solution.getRoutes().length;
        for (int src = 0; src < nbrOfRoutes; src++) {
            for (int dst = 0; dst < nbrOfRoutes; dst++) {
                collectDirectedMoves(src, dst, queue);
            }
        }
        return queue;
    }

    @Override
    protected Node[][] change(Solution solution, float[] changeParameter) throws XFVRPException {
        return XFVRPMoveUtil.change(solution, changeParameter);
    }

}
