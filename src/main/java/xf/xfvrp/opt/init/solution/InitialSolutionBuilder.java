package xf.xfvrp.opt.init.solution;

import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.monitor.StatusManager;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.init.solution.pdp.PDPInitialSolutionBuilder;
import xf.xfvrp.opt.init.solution.vrp.VRPInitialSolutionBuilder;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * Creates a trivial solution out of the model.
 * <p>
 * The solution must be feasible/valid, but no optimization is
 * applied.
 *
 * @author hschneid
 */
public class InitialSolutionBuilder {

    public Solution build(XFVRPModel model, XFVRPParameter parameter, StatusManager statusManager) throws XFVRPException {
        if (parameter.isWithPDP())
            return new PDPInitialSolutionBuilder().build(model);

        return new VRPInitialSolutionBuilder().build(model.getCustomerNodes(), model, statusManager);
    }
}
