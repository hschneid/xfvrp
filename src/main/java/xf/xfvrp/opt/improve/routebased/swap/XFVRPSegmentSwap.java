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
    protected Queue<float[]> search(Solution solution) {
        PriorityQueue<float[]> improvingSteps = new PriorityQueue<>(
                (o1, o2) -> Float.compare(o2[0], o1[0])
        );
        XFVRPSwapSearchUtil.search(solution, improvingSteps, maxSegmentLength, isSegmentLengthEqual, isInvertationActive);

        return improvingSteps;
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