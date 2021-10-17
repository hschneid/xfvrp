package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.base.compartment.CompartmentLoadBuilder;

import java.util.Arrays;

/**
 * Copyright (c) 2012-2021 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class ContextBuilder {

	public static Context build(XFVRPModel model) {
		Context context = new Context();
		
		context.setModel(model);

		// Variables
		context.setMaxGlobalNodeIdx(model.getMaxGlobalNodeIdx() + 1);

		// Amounts - Which amounts must be stored during checking - Each compartment and each load type - So same like vehicle capacities
		context.setAmountArr(CompartmentLoadBuilder.createCompartmentLoads(model.getCompartments()));

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
