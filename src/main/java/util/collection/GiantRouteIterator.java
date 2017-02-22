package util.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 **/
public class GiantRouteIterator implements Iterator<Route> {

	private List<Node> giantRoute;
	int pointer = 0;
	private Route singleRoute;
	
	public void init(List<Node> giantRoute){
		this.pointer = 0;
		this.giantRoute = giantRoute;
		this.singleRoute = null;
	}
	
	@Override
	public boolean hasNext() {
		int routeStart = pointer;
		List<Node> r = new ArrayList<>();
		
		while(pointer < giantRoute.size()) {
			r.add(giantRoute.get(pointer));
			
			if (pointer > routeStart && giantRoute.get(pointer).getSiteType() == SiteType.DEPOT)
				break;
			
			pointer++;			
		}
		
		int routeEnd = pointer;
		
		if (r.size() > 1) {
			singleRoute = new Route(routeStart, routeEnd, r);
			return true;
		}
		singleRoute = null;
		return false;
	}

	@Override
	public Route next() {
		return singleRoute;
	}
}
