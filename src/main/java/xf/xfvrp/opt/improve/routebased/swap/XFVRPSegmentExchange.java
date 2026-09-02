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
 * Copyright (c) 2012-2026 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * This neighborhood search produces improved solutions by
 * exchanging or moving two segments. The size of each segments may be
 * limited up to 3 nodes. The nodes of a segment can be inverted.
 * <p>
 * Size of NS is O(k * n²), where k is segment size and nbr of invert types
 *
 * @author hschneid
 */
public class XFVRPSegmentExchange extends XFVRPOptImpBase {

    private final boolean isInvertationActive = true;
    private final boolean isSegmentLengthEqual = false;
    private final int maxSegmentLength = 3;

    @Override
    protected void searchRoutePair(Solution solution, Queue<float[]> queue, int routeIdxA, int routeIdxB) {
        // Move search (both directions for inter-route)
        XFVRPMoveSearchUtil.searchDirectedRoutePair(solution, queue, routeIdxA, routeIdxB, maxSegmentLength, isInvertationActive);
        if (routeIdxA != routeIdxB) {
            XFVRPMoveSearchUtil.searchDirectedRoutePair(solution, queue, routeIdxB, routeIdxA, maxSegmentLength, isInvertationActive);
        }

        // Swap search
        XFVRPSwapSearchUtil.searchRoutePair(solution, queue, routeIdxA, routeIdxB, maxSegmentLength, isSegmentLengthEqual, isInvertationActive);
    }

    @Override
    protected Queue<float[]> search(Solution solution) {
        evaluateDirtyPairs(solution);
        PriorityQueue<float[]> queue = new PriorityQueue<>((o1, o2) -> Float.compare(o2[0], o1[0]));
        int nbrOfRoutes = solution.getRoutes().length;
        // Moves first: src 0..N, dst 0..N (move arrays have length 8)
        for (int src = 0; src < nbrOfRoutes; src++) {
            for (int dst = 0; dst < nbrOfRoutes; dst++) {
                collectDirectedMoves(src, dst, queue, 8);
            }
        }
        // Swaps second: a 0..N, b a..N (swap arrays have length 9)
        for (int a = 0; a < nbrOfRoutes; a++) {
            for (int b = a; b < nbrOfRoutes; b++) {
                collectPairMoves(a, b, queue, 9);
            }
        }
        return queue;
    }

    @Override
    protected Node[][] change(Solution solution, float[] changeParameter) throws XFVRPException {
        if (changeParameter.length == 9) {
            return XFVRPSwapUtil.change(solution, changeParameter);
        } else if (changeParameter.length == 8) {
            return XFVRPMoveUtil.change(solution, changeParameter);
        }

        return null;
    }
}