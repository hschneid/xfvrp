package xf.xfvrp.opt.init.precheck;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.opt.init.precheck.pdp.PDPPreCheckService;
import xf.xfvrp.opt.init.precheck.vrp.VRPPreCheckService;

/**
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class PreCheckService {

	public Node[] precheck(Node[] nodes, Vehicle vehicle, XFVRPParameter parameter) throws PreCheckException {
		if(parameter.isWithPDP())
			return new PDPPreCheckService().precheck(nodes, vehicle);

		return new VRPPreCheckService().precheck(nodes, vehicle);
	}
}
