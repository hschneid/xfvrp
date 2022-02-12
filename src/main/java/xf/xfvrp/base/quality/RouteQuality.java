package xf.xfvrp.base.quality;

import xf.xfvrp.base.Quality;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Is the quality of a certain route
 *
 * @author hschneid
 *
 */
public class RouteQuality extends Quality {

	private final int routeIdx;

	public RouteQuality(int routeIdx, Quality q) {
		super(q);
		this.routeIdx = routeIdx;
	}

	public int getRouteIdx() {
		return routeIdx;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return routeIdx+" "+cost+" "+penalty;
	}

}
