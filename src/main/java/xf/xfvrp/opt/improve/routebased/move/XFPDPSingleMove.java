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
 * <p>
 * Contains the optimization algorithms
 * for 2-opt
 *
 * @author hschneid
 */
public class XFPDPSingleMove extends XFVRPOptImpBase {

    @Override
    protected void searchRoutePair(Solution solution, Queue<float[]> queue, int routeIdxA, int routeIdxB) {
        XFPDPMoveSearchUtil.searchDirectedRoutePair(solution, queue, routeIdxA, routeIdxB);
        if (routeIdxA != routeIdxB) {
            XFPDPMoveSearchUtil.searchDirectedRoutePair(solution, queue, routeIdxB, routeIdxA);
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
        return XFPDPMoveUtil.change(solution, changeParameter);
    }
}
