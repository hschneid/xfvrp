package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.XFVRPModel;

import java.util.HashMap;
import java.util.Map;

public class RouteInfoBuilder {

	/**
	 * 
	 * @param route
	 * @param model 
	 * @return
	 */
	public static Map<Node, RouteInfo> build(Node[] route, XFVRPModel model) {
		Map<Node, RouteInfo> routeInfos = new HashMap<>();
		RouteInfo routeInfo = null;
		
		for (int idx = 0; idx < route.length; idx++) {
			Node node = route[idx];
			
			routeInfo = createRouteInfo(routeInfos, routeInfo, node);
		}
		
		return routeInfos;
	}

	private static RouteInfo createRouteInfo(Map<Node, RouteInfo> routeInfos, RouteInfo routeInfo, Node node) {
		switch(node.getSiteType()) {
			case DEPOT : {
				if(routeInfo != null) {
					routeInfos.put(routeInfo.getDepot(), routeInfo);
				}
				return new RouteInfo(node);
			}
			case REPLENISH : {
				if(routeInfo != null) {
					routeInfos.put(routeInfo.getDepot(), routeInfo);
				}
				return new RouteInfo(node);
			}
			case CUSTOMER : {
				changeRouteInfo(node, routeInfo);
				return routeInfo;
			}
			default : {
				throw new IllegalStateException("Found unexpected site type ("+node.getSiteType().toString()+")");
			}
		}			
	}

	private static void changeRouteInfo(Node node, RouteInfo routeInfo) {
		LoadType loadType = node.getLoadType();
		
		if(loadType == LoadType.PICKUP) {
			routeInfo.addUnLoadingServiceTime(node.getServiceTime());
			routeInfo.addPickUpAmount(node.getDemand());
		} else if(loadType == LoadType.DELIVERY) {
			routeInfo.addLoadingServiceTime(node.getServiceTime());
			routeInfo.addDeliveryAmount(node.getDemand());
		} else
			throw new IllegalStateException("Found unexpected load type ("+loadType.toString()+")");
	}
	
}
