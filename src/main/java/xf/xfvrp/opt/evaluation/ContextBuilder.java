package xf.xfvrp.opt.evaluation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import xf.xfvrp.base.LoadType;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;

public class ContextBuilder {

	public static Context build(Solution solution, XFVRPModel model) {
		Context context = new Context();

		Node[] giantRoute = solution.getGiantRoute();

		// Active nodes for evalution are true, duplicates or empty routes are false
		context.setActiveNodes(getActiveNodes(giantRoute));

		// Variables
		context.setMaxGlobalNodeIdx(model.getMaxGlobalNodeIdx() + 1);
		context.setAmountArr(new float[model.getVehicle().capacity.length * 2]);

		context.setBlockPresetArr(new int[model.getNbrOfBlocks()]);
		Arrays.fill(context.getBlockPresetArr(), -1);
		context.setAvailablePresetCountArr(model.getBlockPresetCountList());
		context.setFoundPresetCountArr(new int[model.getNbrOfBlocks()]);
		context.setLastPresetSequenceRankArr(new int[model.getNbrOfBlocks()]);
		context.setPresetRoutingBlackList(new boolean[context.getMaxGlobalNodeIdx()]);
		context.setPresetRoutingNodeList(new boolean[context.getMaxGlobalNodeIdx()]);

		// Service times at the depot for amount on the route
		context.setRouteInfos(buildRouteInfos(giantRoute, model));

		return context;
	}

	/**
	 * Searches in the giant route for nodes which can be ignored during
	 * evalution. This can be the case for empty routes or unnecessary
	 * replenishments.
	 * 
	 * @param giantRoute
	 * @return list of active (true) or disabled (false) nodes in giant route
	 */
	private static boolean[] getActiveNodes(Node[] giantRoute) {
		boolean[] activeNodes = new boolean[giantRoute.length];

		int lastNodeIdx = 0;
		Node lastNode = giantRoute[lastNodeIdx];
		for (int i = 1; i < activeNodes.length; i++) {
			activeNodes[i] = true;

			Node currNode = giantRoute[i];

			if(currNode.getSiteType() == SiteType.DEPOT && 
					lastNode.getSiteType() == SiteType.DEPOT)
				activeNodes[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.REPLENISH &&
					lastNode.getSiteType() == SiteType.REPLENISH)
				activeNodes[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.DEPOT &&
					lastNode.getSiteType() == SiteType.REPLENISH)
				activeNodes[lastNodeIdx] = false;
			else if(currNode.getSiteType() == SiteType.REPLENISH &&
					lastNode.getSiteType() == SiteType.DEPOT)
				activeNodes[i] = false;

			if(activeNodes[i]) {
				lastNode = currNode;
				lastNodeIdx = i;
			}
		}

		return activeNodes;
	}

	/**
	 * 
	 * @param giantRoute
	 * @param model 
	 * @return
	 */
	private static Map<Node, RouteInfo> buildRouteInfos(Node[] giantRoute, XFVRPModel model) {
		Map<Node, RouteInfo> routeInfos = new HashMap<>();
		
		RouteInfo routeInfo = new RouteInfo(giantRoute[0]);
		for (int i = 1; i < giantRoute.length; i++) {
			if(giantRoute[i].getSiteType() == SiteType.DEPOT || 
					giantRoute[i].getSiteType() == SiteType.REPLENISH) {
				routeInfos.put(routeInfo.getDepot(), routeInfo);
				routeInfo = new RouteInfo(giantRoute[i]);
			} else if(giantRoute[i].getSiteType() == SiteType.CUSTOMER) {
				changeRouteInfo(giantRoute[i], routeInfo);
			} else
				throw new IllegalStateException("Found unexpected site type ("+giantRoute[i].getSiteType().toString()+")");
		}
		return routeInfos;
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
