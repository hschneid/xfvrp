package xf.xfvrp.opt.evaluation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;

public class RouteInfoBuilder {

	/**
	 * 
	 * @param nodes
	 * @param model 
	 * @return
	 */
	public static Map<Node, RouteInfo> build(List<Node> nodes, XFVRPModel model) {
		Map<Node, RouteInfo> routeInfos = new HashMap<>();
		RouteInfo routeInfo = new RouteInfo(null);
		
		for (int idx = 0; idx < nodes.size(); idx++) {
			Node node = nodes.get(idx);
			
			routeInfo = createRouteInfo(routeInfos, routeInfo, node);
		}
		
		if(!routeInfos.containsKey(routeInfo.getDepot()))
			routeInfos.put(routeInfo.getDepot(), routeInfo);
		
		return routeInfos;
	}

	private static RouteInfo createRouteInfo(Map<Node, RouteInfo> routeInfos, RouteInfo routeInfo, Node node) {
		if(node.getSiteType() == SiteType.DEPOT || node.getSiteType() == SiteType.REPLENISH) {
			routeInfos.put(routeInfo.getDepot(), routeInfo);
			return new RouteInfo(node);
		} else if(node.getSiteType() == SiteType.CUSTOMER) {
			changeRouteInfo(node, routeInfo);
			return routeInfo;
		} else
			throw new IllegalStateException("Found unexpected site type ("+node.getSiteType().toString()+")");
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
