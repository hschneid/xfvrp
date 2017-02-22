package xf.xfvrp.base.preset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.fleximport.InternalCustomerData;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * 
 * @author hschneid
 *
 */
public class BlockNameConverter {

	public static final int UNDEF_BLOCK_IDX = -1;
	public static final int DEFAULT_BLOCK_IDX = 0;
	
	/**
	 * Converts the user block names into indexed numbers.
	 * 
	 * @param node Node without block indexes
	 * @param cust Contains the input data
	 * @param list
	 * @return Node with block indexes
	 */
	public static void convert(Node[] nodes, List<InternalCustomerData> list) {
		Map<String, Node> nodeMap = new HashMap<>();
		for (Node node : nodes)
			nodeMap.put(node.getExternID(), node);

		// Index block names
		Map<String, Integer> blockNameMap = new HashMap<>();
		int idx = DEFAULT_BLOCK_IDX + 1;
		for (InternalCustomerData c : list)
			if(c.getPresetBlockName().length() > 0 && !blockNameMap.containsKey(c.getPresetBlockName()))
				blockNameMap.put(c.getPresetBlockName(), idx++);
		
		// Insert block index into nodes
		list.forEach(cust -> {
			int blockIdx = DEFAULT_BLOCK_IDX;
			if(blockNameMap.containsKey(cust.getPresetBlockName()))
				blockIdx = blockNameMap.get(cust.getPresetBlockName());
			nodeMap.get(cust.getExternID()).setPresetBlockIdx(blockIdx);
		});
	}
}
