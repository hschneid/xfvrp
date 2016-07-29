package util.collection;

import java.util.List;

import xf.xfvrp.base.Node;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public class Route {
	private int routeStart;
	private int routeEnd;
	private List<Node> nodes;

	public Route(int routeStart, int routeEnd, List<Node> nodes) {
		super();
		this.routeStart = routeStart;
		this.routeEnd = routeEnd;
		this.nodes = nodes;
	}

	public int getRouteStart() {
		return routeStart;
	}

	public int getRouteEnd() {
		return routeEnd;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public void print() {
		System.out.println(nodes.toString());
	}
}
