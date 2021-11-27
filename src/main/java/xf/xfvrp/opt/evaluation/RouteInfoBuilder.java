package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class RouteInfoBuilder {

	public static Map<Node, RouteInfo[]> build(Node[] route, Context context) throws XFVRPException {
		Map<Node, RouteInfo[]> routeInfos = new HashMap<>();
		RouteInfo[] routeInfoPerCompartment = new RouteInfo[context.getModel().getCompartments().length];

		// Go over all nodes in giant route (or single route)
		for (int idx = 0; idx < route.length; idx++) {
			createRouteInfo(route[idx], routeInfoPerCompartment, routeInfos, context);
		}

		return routeInfos;
	}

	private static void createRouteInfo(Node node, RouteInfo[] routeInfoPerCompartment, Map<Node, RouteInfo[]> routeInfos, Context context) throws XFVRPException {
		switch(node.getSiteType()) {
			case DEPOT :
			case REPLENISH :
				beginNewInfo(node, routeInfoPerCompartment, routeInfos, context);
				break;
			case CUSTOMER : {
				updateInfo(node, routeInfoPerCompartment);
				break;
			}
			default : {
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_ARGUMENT, "Found unexpected site type ("+node.getSiteType().toString()+")");
			}
		}
	}

	private static void beginNewInfo(Node node, RouteInfo[] routeInfoPerCompartment, Map<Node, RouteInfo[]> routeInfos, Context context) {
		for (int compartmentIdx = node.getDemand().length - 1; compartmentIdx >= 0; compartmentIdx--) {
			// Compartments without replenishment
			if(node.getSiteType() == SiteType.REPLENISH && !context.getModel().getCompartments()[compartmentIdx].isReplenished()) {
				continue;
			}

			// Finish old route info
			if(routeInfoPerCompartment[compartmentIdx] != null) {
				routeInfos.get(routeInfoPerCompartment[compartmentIdx].getDepot())[compartmentIdx] = routeInfoPerCompartment[compartmentIdx];
			}

			// Start new route info
			if(!routeInfos.containsKey(node)) {
				routeInfos.put(node, new RouteInfo[context.getModel().getCompartments().length]);
			}
			routeInfoPerCompartment[compartmentIdx] = new RouteInfo(node, compartmentIdx);
		}
	}

	private static void updateInfo(Node node, RouteInfo[] routeInfoPerCompartment) throws XFVRPException {
		LoadType loadType = node.getLoadType();
		for (int compartmentIdx = node.getDemand().length - 1; compartmentIdx >= 0; compartmentIdx--) {
			if(loadType == LoadType.PICKUP) {
				routeInfoPerCompartment[compartmentIdx].addPickUpAmount(node.getDemand()[compartmentIdx]);
				routeInfoPerCompartment[compartmentIdx].addUnLoadingServiceTime(node.getServiceTime());
			} else if(loadType == LoadType.DELIVERY) {
				routeInfoPerCompartment[compartmentIdx].addDeliveryAmount(node.getDemand()[compartmentIdx]);
				routeInfoPerCompartment[compartmentIdx].addLoadingServiceTime(node.getServiceTime());
			} else
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE ,"Found unexpected load type ("+loadType.toString()+")");
		}
	}

}
