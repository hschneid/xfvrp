package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class RouteInfoBuilder {

	public static Map<Node, RouteInfo> build(Node[] route) throws XFVRPException {
		Map<Node, RouteInfo> routeInfos = new HashMap<>();
		RouteInfo routeInfo = null;
		
		for (int idx = 0; idx < route.length; idx++) {
			Node node = route[idx];
			
			routeInfo = createRouteInfo(routeInfos, routeInfo, node);
		}
		
		return routeInfos;
	}

	private static RouteInfo createRouteInfo(Map<Node, RouteInfo> routeInfos, RouteInfo routeInfo, Node node) throws XFVRPException {
		switch(node.getSiteType()) {
			case DEPOT :
			case REPLENISH :
				if(routeInfo != null) {
					routeInfos.put(routeInfo.getDepot(), routeInfo);
				}
				return new RouteInfo(node);
			case CUSTOMER : {
				changeRouteInfo(node, routeInfo);
				return routeInfo;
			}
			default : {
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT, "Found unexpected site type ("+node.getSiteType().toString()+")");
			}
		}			
	}

	private static void changeRouteInfo(Node node, RouteInfo routeInfo) throws XFVRPException {
		LoadType loadType = node.getLoadType();
		
		if(loadType == LoadType.PICKUP) {
			routeInfo.addUnLoadingServiceTime(node.getServiceTime());
			routeInfo.addPickUpAmount(node.getDemand());
		} else if(loadType == LoadType.DELIVERY) {
			routeInfo.addLoadingServiceTime(node.getServiceTime());
			routeInfo.addDeliveryAmount(node.getDemand());
		} else
			throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE ,"Found unexpected load type ("+loadType.toString()+")");
	}
	
}
