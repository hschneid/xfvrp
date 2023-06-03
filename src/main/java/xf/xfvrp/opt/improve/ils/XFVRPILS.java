package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.routebased.move.XFVRPSegmentMove;
import xf.xfvrp.opt.improve.routebased.move.XFVRPSingleMove;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPBorderSegmentExchange;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSegmentExchange;
import xf.xfvrp.opt.improve.routebased.swap.XFVRPSingleSwap;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Optimization procedure for iterative local search
 * <p>
 * Four local search procedures with adaptive randomized variable neighborhood selection.
 *
 * @author hschneid
 */
public class XFVRPILS extends XFILS {

    /*
     * (non-Javadoc)
     * @see xf.xfvrp.opt.improve.ils.XFILS#execute(xf.xfvrp.opt.Solution)
     */
    @Override
    public Solution execute(Solution solution) throws XFVRPException {
        optArr = new XFVRPOptBase[]{
                new XFVRPSingleMove(),
                new XFVRPSingleSwap(),
                new XFVRPSegmentMove(),
                new XFVRPBorderSegmentExchange(),
                new XFVRPSegmentExchange()
        };

        optPropArr = new double[]{
                // 0.9, 0.1
                0.3, 0.3, 0.15, 0.15, 0.1
        };

        randomChangeService = new XFVRPRandomChangeService();

        return super.execute(solution);
    }

}
