package xf.xfvrp.base.preset;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.fleximport.CustomerData;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 
 * Copyright (c) 2012-2021 Holger Schneider
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

	public static final int UNDEF_POSITION = 0;
	
	/**
	 * Converts the user block positions into an indexed numbers.
	 */
	public static void convert(Node[] nodes, List<CustomerData> list) {
		Map<String, Node> nodeMap = getMapping(nodes);

		normBlockPositions(list, nodeMap);
	}

	private static Map<String, Node> getMapping(Node[] nodes) {
		return Arrays.stream(nodes).collect(Collectors.toMap(Node::getExternID, v -> v, (v1, v2) -> v1));
	}

	private static void normBlockPositions(List<CustomerData> list, Map<String, Node> nodeMap) {
		list.stream()
		.filter(f -> f.getPresetBlockName() != null && f.getPresetBlockName().length() > 0)
		.filter(f -> f.getPresetBlockPosition() >= 0)
		.collect(Collectors.groupingBy(k -> k.getPresetBlockName()))
		.values().stream()
		.filter(presets -> presets.size() > 1)
		.peek(m -> m.sort(Comparator.comparingInt(c -> c.getPresetBlockPosition())))
		.forEach(f -> {
			int posIdx = UNDEF_POSITION + 1;
			for (CustomerData cust : f) {
				Node node = nodeMap.get(cust.getExternID());
				node.setPresetBlockPos(posIdx++);
			}
		});
	}
}
