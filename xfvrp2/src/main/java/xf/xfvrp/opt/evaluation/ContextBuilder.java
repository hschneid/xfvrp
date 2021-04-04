package xf.xfvrp.opt.evaluation;

import java.util.Arrays;

import xf.xfvrp.base.XFVRPModel;

public class ContextBuilder {

	public static Context build(XFVRPModel model) {
		Context context = new Context();
		
		context.setModel(model);

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

		return context;
	}

	
}
