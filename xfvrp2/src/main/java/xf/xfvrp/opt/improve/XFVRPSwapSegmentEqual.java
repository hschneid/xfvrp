package xf.xfvrp.opt.improve;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This neighborhood search produces improved solutions by
 * exchanging two segments of the giant tour. Both segments 
 * have the same size, which ranges between 2 and 5.
 * 
 * @author hschneid
 *
 */
public class XFVRPSwapSegmentEqual extends XFVRPSwapSegmentWithInvert {

	public XFVRPSwapSegmentEqual() {
		super();
		setInvertationMode(false);
		setEqualSegmentLength(true);
	}
}
