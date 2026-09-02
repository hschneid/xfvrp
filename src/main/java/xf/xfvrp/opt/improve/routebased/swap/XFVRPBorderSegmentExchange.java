package xf.xfvrp.opt.improve.routebased.swap;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.routebased.XFVRPOptImpBase;
import xf.xfvrp.opt.improve.routebased.move.XFVRPBorderMoveSearchUtil;
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
 * exchanging or moving two segments, where each segment contains a depot.
 * The nodes of a segment can be inverted.
 * <p>
 * Size of NS is O(k * n²).
 *
 * @author hschneid
 */
public class XFVRPBorderSegmentExchange extends XFVRPOptImpBase {

    private final boolean isInvertationActive = true;

    @Override
    protected void searchRoutePair(Solution solution, Queue<float[]> queue, int routeIdxA, int routeIdxB) {
        // Border move search (both directions for inter-route)
        XFVRPBorderMoveSearchUtil.searchDirectedRoutePair(solution, queue, routeIdxA, routeIdxB, isInvertationActive);
        if (routeIdxA != routeIdxB) {
            XFVRPBorderMoveSearchUtil.searchDirectedRoutePair(solution, queue, routeIdxB, routeIdxA, isInvertationActive);
        }

        // Border swap search
        XFVRPBorderSwapSearchUtil.searchRoutePair(solution, queue, routeIdxA, routeIdxB, isInvertationActive);
    }

    @Override
    protected Queue<float[]> search(Solution solution) {
        evaluateDirtyPairs(solution);
        PriorityQueue<float[]> queue = new PriorityQueue<>((o1, o2) -> Float.compare(o2[0], o1[0]));
        int nbrOfRoutes = solution.getRoutes().length;
        // Border moves first: src 0..N, dst 0..N (move arrays have length 8)
        for (int src = 0; src < nbrOfRoutes; src++) {
            for (int dst = 0; dst < nbrOfRoutes; dst++) {
                collectDirectedMoves(src, dst, queue, 8);
            }
        }
        // Border swaps second: a 0..N, b a..N (swap arrays have length 9)
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