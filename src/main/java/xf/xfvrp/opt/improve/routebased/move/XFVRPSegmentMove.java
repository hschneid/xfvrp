package xf.xfvrp.opt.improve.routebased.move;

/**
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * The Segment Move neighborhood operator removes from a route
 * a segment and inserts it at a specific position in same or other route.
 *
 * To improve the performance of this huge neighborhood search O(n^3),
 * the length of the segments is restricted up to 4. Longer segments
 * than 4 are not relocated.
 *
 * As expansion of standard segment relocation a chosen segment can be
 * additionally inverted in the ordering of nodes.
 *
 * @author hschneid
 *
 */
public class XFVRPSegmentMove extends XFVRPMoveBase {

	private static final int MAX_SEGMENT_LENGTH = 3;
	private static final boolean IS_INVERT_ACTIVE = true;

	public XFVRPSegmentMove() {
		super(MAX_SEGMENT_LENGTH, IS_INVERT_ACTIVE);
	}

	public XFVRPSegmentMove(boolean isInvertActive) {
		super(MAX_SEGMENT_LENGTH, isInvertActive);
	}
}
