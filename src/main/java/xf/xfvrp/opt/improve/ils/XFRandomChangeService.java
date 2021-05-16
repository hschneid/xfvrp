package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;

/**
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public interface XFRandomChangeService {

	Solution change(Solution solution, XFVRPModel model) throws XFVRPException;
	
}
