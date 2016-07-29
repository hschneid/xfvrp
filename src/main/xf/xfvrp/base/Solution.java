package xf.xfvrp.base;

import xf.xfvrp.report.Report;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * All solutions have to implement this interface with methods for
 * accessing the report (result) or the used model with input data.
 * 
 * @author hschneid
 *
 */
public interface Solution {

	/**
	 * The report is a textual representation
	 * of the last planned solution. A Report instance
	 * is hierarchical structured. 
	 * 
	 * @return report of last solution
	 */
	public Report getReport();
	
	/**
	 * Each solution is dependent of the underlying model,
	 * which can be reached by this method.
	 * 
	 * @return model of last solution
	 */
	public XFVRPModel getModel();
}
