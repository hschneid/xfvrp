package xf.xfvrp.opt.evaluation;

import java.util.Arrays;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;

public class ContextBuilder {

	public static Context build(Solution solution, XFVRPModel model) {
		Context context = new Context();

		Node[] giantRoute = solution.getGiantRoute();

		// Active nodes for evalution are true, duplicates or empty routes are false
		context.setActiveNodes(ActiveNodeAnalyzer.getActiveNodes(giantRoute));

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
		context.setRouteInfos(RouteInfoBuilder.build(giantRoute, context.getActiveNodes(), model));

		return context;
	}

	
}
