package xf.xfvrp.opt.improve;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * No optimization
 * Input solution = Output solution
 *
 * Is used in testing
 *
 * @author hschneid
 *
 */
public class XFVRPNoOpt extends XFVRPOptBase {

	@Override
	public Solution execute(Solution solution) throws XFVRPException {
		return solution;
	}
}
