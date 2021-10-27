package xf.xfvrp.opt;

import xf.xfvrp.base.XFVRPModel;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * The solution of the planning is stored in an
 * XFVRPSolution instance. The final giant tour
 * and the allocated model are stored.
 * 
 * @author hschneid
 *
 */
public class XFVRPSolution {

	protected Solution solution;
	protected XFVRPModel model;

	public XFVRPSolution(Solution solution) {
		this.solution = solution;
		this.model = solution.getModel();
	}

	public Solution getSolution() {
		return solution;
	}

	public XFVRPModel getModel() {
		return model;
	}
}
