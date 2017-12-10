package xf.xfvrp.opt.improve;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * The OrOpt neighborhood removes from a solution
 * 3 edges, but the 3 inserted edges considers a
 * specific logic (other than 3-opt) so that a certain
 * segment of the route plan is moved to
 * any position in the route plan.
 * 
 * To improve the performance of this huge neighborhood search
 * the length of the segments is restricted up to 4. Longer paths
 * than 3 are not moved.
 * 
 * @author hschneid
 *
 */
public class XFVRPOrOpt extends XFVRPPathMove {
	
	public XFVRPOrOpt() {
		super();
		setInvertationMode(false);
	}
}
