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
 * Copyright (c) 2012-2023 Holger Schneider
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
    protected Queue<float[]> search(Solution solution) {
        var improvingSteps = new PriorityQueue<float[]>(
                (o1, o2) -> Float.compare(o2[0], o1[0])
        );
        XFVRPBorderMoveSearchUtil.search(solution, improvingSteps, isInvertationActive);
        XFVRPBorderSwapSearchUtil.search(solution, improvingSteps, isInvertationActive);

        return improvingSteps;
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