package xf.xfvrp.base.preset;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;
import xf.xfvrp.base.fleximport.CustomerData;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copyright (c) 2012-2022 Holger Schneider
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
		checkBlockPositions(list);
		normBlockPositions(list, nodeMap);
	}

	private static Map<String, Node> getMapping(Node[] nodes) {
		return Arrays.stream(nodes).collect(Collectors.toMap(Node::getExternID, v -> v, (v1, v2) -> v1));
	}

	private static void checkBlockPositions(List<CustomerData> customers) {
		List<String> duplicateBlockPositions = getCustomersWithPreset(customers)
				// Map block positions to List map
				.map(node -> node.getPresetBlockName() + "#" +node.getPresetBlockPosition())
				.collect(Collectors.groupingBy(key -> key))
				.entrySet()
				.stream()
				// Check for map entries with multiple positions
				.filter(e -> e.getValue().size() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());

		// Generate exception
		if(duplicateBlockPositions.size() > 0) {
			for (String blockPosition : duplicateBlockPositions) {
				throw new XFVRPException(
						XFVRPExceptionType.ILLEGAL_INPUT,
						String.format("The position %s in block %s is assigned multiple times. Please assign only once.", blockPosition, blockPosition)
				);
			}
		}
	}

	private static void normBlockPositions(List<CustomerData> list, Map<String, Node> nodeMap) {
		getCustomersWithPreset(list)
				.collect(Collectors.groupingBy(CustomerData::getPresetBlockName))
				.values().stream()
				.filter(presets -> presets.size() > 1)
				.peek(m -> m.sort(Comparator.comparingInt(CustomerData::getPresetBlockPosition)))
				.forEach(f -> {
					int posIdx = UNDEF_POSITION + 1;
					for (CustomerData cust : f) {
						Node node = nodeMap.get(cust.getExternID());
						node.setPresetBlockPos(posIdx++);
						// Rank of customers with position must be deactivated
						node.setPresetBlockRank(0);
					}
				});
	}


	private static Stream<CustomerData> getCustomersWithPreset(List<CustomerData> customers) {
		return customers.stream()
				.filter(f -> f.getPresetBlockName() != null && f.getPresetBlockName().length() > 0)
				.filter(f -> f.getPresetBlockPosition() >= UNDEF_POSITION);
	}

}
