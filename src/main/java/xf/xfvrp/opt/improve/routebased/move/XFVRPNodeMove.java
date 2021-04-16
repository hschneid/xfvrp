package xf.xfvrp.opt.improve.routebased.move;

/**
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Contains the optimization algorithms
 * for relocate neighborhood. One customer node can
 * be moved to any other position in the 
 * route plan.
 *
 * @author hschneid
 *
 */
public class XFVRPNodeMove extends XFVRPMoveBase {

	private static final int MAX_SEGMENT_LENGTH = 1;
	private static final boolean IS_INVERT_ACTIVE = false;

	public XFVRPNodeMove() {
		super(MAX_SEGMENT_LENGTH, IS_INVERT_ACTIVE);
	}
}
