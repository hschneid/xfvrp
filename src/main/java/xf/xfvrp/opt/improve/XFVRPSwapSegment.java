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
 * exchanging two segments of the giant tour. The size of the
 * segments must not be equal and has sizes between 2 and 5.
 * 
 * @author hschneid
 *
 */
public class XFVRPSwapSegment extends XFVRPSwapSegmentWithInvert {

	public XFVRPSwapSegment() {
		super();
		setInvertationMode(false);
	}
}
