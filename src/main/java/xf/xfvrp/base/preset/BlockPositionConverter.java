package xf.xfvrp.base.preset;

import java.util.Arrays;
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
public class BlockPositionConverter {

	public static final int UNDEF_BLOCK_IDX = -1;
	public static final int DEFAULT_BLOCK_IDX = 0;
	
	/**
	 * Converts the user block positions into an indexed numbers.
	 * 
	 * @param node Node without block indexes
	 * @param cust Contains the input data
	 * @param list
	 * @return Node with block indexes
	 */
	public static void convert(Node[] nodes, List<InternalCustomerData> list) {
		Map<String, Node> nodeMap = getMapping(nodes);

		normBlockPositions(list, nodeMap);
	}

	private static Map<String, Node> getMapping(Node[] nodes) {
		Map<String, Node> nodeMap = Arrays.stream(nodes).collect(Collectors.toMap(k -> k.getExternID(), v -> v, (v1, v2) -> v1));
		return nodeMap;
	}

	private static void normBlockPositions(List<InternalCustomerData> list, Map<String, Node> nodeMap) {
		list.stream()
		.filter(f -> f.getPresetBlockName() != null && f.getPresetBlockName().length() > 0)
		.filter(f -> f.getPresetBlockPosition() > 0)
		.collect(Collectors.groupingBy(k -> k.getPresetBlockName()))
		.values().stream()
		.map(m -> {
			m.sort((c1, c2) -> c1.getPresetBlockPosition() - c2.getPresetBlockPosition());
			return m;
		})
		.forEach(f -> {
			int posIdx = 1;
			for (InternalCustomerData cust : list) {
				Node node = nodeMap.get(cust.getExternID());
				node.setPresetBlockPos(posIdx++);
			}
		});
	}
}
