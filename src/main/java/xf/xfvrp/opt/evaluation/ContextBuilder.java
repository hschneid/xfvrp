package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.XFVRPModel;

import java.util.Arrays;

public class ContextBuilder {

	public static Context build(XFVRPModel model) {
		Context context = new Context();
		
		context.setModel(model);

		// Variables
		context.setMaxGlobalNodeIdx(model.getMaxGlobalNodeIdx() + 1);

		// Amounts - Which amounts must be stored during checking - Each compartment and each load type - So same like vehicle capacities
		context.setAmountArr(new float[model.getVehicle().capacity.length]);

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
