package xf.xfvrp.base.preset;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
	 */
	public static void convert(Node[] nodes, List<InternalCustomerData> list) {
		Map<String, Node> nodeMap = allocateNodesByExternID(nodes);

		Map<String, Integer> blockNameMap = allocateIndexByExternID(list);
		
		setBlockIndexes(list, nodeMap, blockNameMap);
	}

	private static Map<String, Node> allocateNodesByExternID(Node[] nodes) {
		return Arrays.stream(nodes).collect(Collectors.toMap(k -> k.getExternID(), v -> v, (v1, v2) -> v1));
	}

	private static void setBlockIndexes(List<InternalCustomerData> list, Map<String, Node> nodeMap,
			Map<String, Integer> blockNameMap) {
		for (InternalCustomerData cust : list) {
			int blockIdx = (blockNameMap.containsKey(cust.getPresetBlockName())) ? blockNameMap.get(cust.getPresetBlockName()) : DEFAULT_BLOCK_IDX;
			nodeMap.get(cust.getExternID()).setPresetBlockIdx(blockIdx);
		}
	}

	private static Map<String, Integer> allocateIndexByExternID(List<InternalCustomerData> list) {
		Map<String, Integer> blockNameMap = new HashMap<>();
		int idx = DEFAULT_BLOCK_IDX + 1;
		for (InternalCustomerData c : list)
			if(!isBlockNameUndefined(c.getPresetBlockName()) && !blockNameMap.containsKey(c.getPresetBlockName()))
				blockNameMap.put(c.getPresetBlockName(), idx++);
		return blockNameMap;
	}
	
	private static boolean isBlockNameUndefined(String blockName) {
		return (blockName == null || blockName.trim().length() == 0);
	}
}
