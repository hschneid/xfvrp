package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.XFVRPPathMove;
import xf.xfvrp.opt.improve.XFVRPRelocate;
import xf.xfvrp.opt.improve.XFVRPSwap;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * Optimization procedure for iterative local search
 * 
 * Three local search procedures with adaptive randomized variable neighborhood selection.
 * 
 * @author hschneid
 *
 */
public class XFVRPILS extends XFILS {

	/*
	 * (non-Javadoc)
	 * @see xf.xfvrp.opt.improve.ils.XFILS#execute(xf.xfvrp.opt.Solution)
	 */
	@Override
	public Solution execute(Solution solution) {
		optArr = new XFVRPOptBase[]{
				new XFVRPRelocate(),
				new XFVRPSwap(),
				new XFVRPPathMove()
		};
		
		optPropArr = new double[] {
				0.4, 0.4, 0.2
		};
		
		randomChangeService = new XFVRPRandomChangeService();

		return super.execute(solution);
	}
	
}
