package xf.xfvrp.opt.improve.routebased;

import xf.xfvrp.opt.Solution;

/**
 * Copyright (c) 2012-2026 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * Shared overhang-route bookkeeping for all route-based neighborhood searches.
 * <p>
 * An "overhang route" is a route with more routes than allowed for its depot.
 * Moves to such a route are penalized, moves away from one are rewarded.
 * The first element of a step array is the improvement value, the second and
 * third elements are the source and destination route indices.
 **/
public class OverhangRouteCheck {

	/**
	 * In any case, if source route is overhang route
	 */
	public static boolean isSourceOverhangRoute(Solution solution, float[] newStep) {
		boolean[] isOverhang = solution.getOverhangRoutes();
		return isOverhang[(int) newStep[1]];
	}

	/**
	 * In any case, if destination route is overhang route
	 */
	public static boolean isDestinationOverhangRoute(Solution solution, float[] newStep) {
		boolean[] isOverhang = solution.getOverhangRoutes();
		return isOverhang[(int) newStep[2]];
	}

	/**
	 * If source route is overhang route, but destination is not, then it should get a bonus.
	 * <p>
	 * This counts for any move or swap.
	 */
	public static boolean isReduceOfOverhang(Solution solution, float[] newStep) {
		boolean[] isOverhang = solution.getOverhangRoutes();
		return isOverhang[(int) newStep[1]] && !isOverhang[(int) newStep[2]];
	}
}
