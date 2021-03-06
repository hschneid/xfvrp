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

	public static final int UNDEF_POSITION = 0;
	
	/**
	 * Converts the user block positions into an indexed numbers.
	 */
	public static void convert(Node[] nodes, List<InternalCustomerData> list) {
		Map<String, Node> nodeMap = getMapping(nodes);

		normBlockPositions(list, nodeMap);
	}

	private static Map<String, Node> getMapping(Node[] nodes) {
		return Arrays.stream(nodes).collect(Collectors.toMap(k -> k.getExternID(), v -> v, (v1, v2) -> v1));
	}

	private static void normBlockPositions(List<InternalCustomerData> list, Map<String, Node> nodeMap) {
		list.stream()
		.filter(f -> f.getPresetBlockName() != null && f.getPresetBlockName().length() > 0)
		.filter(f -> f.getPresetBlockPosition() >= 0)
		.collect(Collectors.groupingBy(k -> k.getPresetBlockName()))
		.values().stream()
		.filter(presets -> presets.size() > 1)
		.map(m -> {
			m.sort((c1, c2) -> c1.getPresetBlockPosition() - c2.getPresetBlockPosition());
			return m;
		})
		.forEach(f -> {
			int posIdx = UNDEF_POSITION + 1;
			for (InternalCustomerData cust : f) {
				Node node = nodeMap.get(cust.getExternID());
				node.setPresetBlockPos(posIdx++);
			}
		});
	}
}
