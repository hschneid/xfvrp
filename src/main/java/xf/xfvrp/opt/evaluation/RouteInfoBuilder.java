package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;

import java.util.*;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class RouteInfoBuilder {

	public static Map<Node, RouteInfo> build(Node[] route) throws XFVRPException {
		Map<Node, RouteInfo> routeInfos = new HashMap<>();
		RouteInfo[] routeInfoPerCompartment = new RouteInfo[route[0].getDemand().length];

		for (int idx = 0; idx < route.length; idx++) {
			Node node = route[idx];

			createRouteInfo(routeInfos, routeInfoPerCompartment, node);
		}

		return routeInfos;
	}

	private static void createRouteInfo(Map<Node, RouteInfo> routeInfos, RouteInfo[] routeInfoPerCompartment, Node node) throws XFVRPException {
		switch(node.getSiteType()) {
			case DEPOT :
			case REPLENISH :
				beginNewInfo(node, routeInfoPerCompartment, routeInfos);
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

	private static void beginNewInfo(Node node, RouteInfo[] routeInfoPerCompartment, Map<Node, RouteInfo> routeInfos) {
		Map<Node, RouteInfo> newRouteInfos = new HashMap<>();
		for (int compartmentIdx = node.getDemand().length - 1; compartmentIdx >= 0; compartmentIdx--) {
			// Compartments without replenishment
			if(node.getSiteType() == SiteType.REPLENISH && !node.isCompartmentReplenished()[compartmentIdx]) {
				continue;
			}

			// Finish old route info
			if(routeInfoPerCompartment[compartmentIdx] != null) {
				routeInfos.put(routeInfoPerCompartment[compartmentIdx].getDepot(), routeInfoPerCompartment[compartmentIdx]);
			}

			// Start new route info
			if(!newRouteInfos.containsKey(node)) {
				newRouteInfos.put(node, new RouteInfo(node));
			}
			routeInfoPerCompartment[compartmentIdx] = newRouteInfos.get(node);
		}
	}

	private static void updateInfo(Node node, RouteInfo[] routeInfoPerCompartment) throws XFVRPException {
		LoadType loadType = node.getLoadType();
		for (int compartmentIdx = node.getDemand().length - 1; compartmentIdx >= 0; compartmentIdx--) {
			if(loadType == LoadType.PICKUP) {
				routeInfoPerCompartment[compartmentIdx].addPickUpAmount(node.getDemand(), compartmentIdx);
			} else if(loadType == LoadType.DELIVERY) {
				routeInfoPerCompartment[compartmentIdx].addDeliveryAmount(node.getDemand(), compartmentIdx);
			} else
				throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE ,"Found unexpected load type ("+loadType.toString()+")");
		}

		// Service times only once per routeInfo-Node
		new HashSet<>(Arrays.asList(routeInfoPerCompartment))
				.forEach(rI -> {
					if(loadType == LoadType.PICKUP) {
						rI.addUnLoadingServiceTime(node.getServiceTime());
					} else if(loadType == LoadType.DELIVERY) {
						rI.addLoadingServiceTime(node.getServiceTime());
					}
				});
	}

}
