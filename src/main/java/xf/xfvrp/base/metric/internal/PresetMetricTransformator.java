package xf.xfvrp.base.metric.internal;

import util.collection.ListMap;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.preset.BlockNameConverter;
import xf.xfvrp.base.preset.BlockPositionConverter;

import java.util.Arrays;
import java.util.List;

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
public class PresetMetricTransformator {

	/**
	 * This method transforms an internal metric into an internal optimization metric, where distance
	 * informations are modified for the user given sequence position preset. This means:
	 * 
	 * if a node A must not be the predecessor of node B, than the distance between A-B
	 * is modified to unlimited.
	 * 
	 * @param metric The original internal metric
	 * @param nodes The nodes of current XFVRP model
	 * @return An internal optimization metric, where sequence position preset is considered.
	 */
	public static InternalMetric transform(InternalMetric metric, final Node[] nodes) {
		boolean shallTransform = isNecessary(nodes);
		if(!shallTransform)
			return metric;

		InternalOptMetric optMetric = new InternalOptMetric(nodes.length);
		int[] followers = new int[nodes.length];
		int[] ancestors = new int[nodes.length];
		Arrays.fill(followers, -1); Arrays.fill(ancestors, -1);

		ListMap<Integer, Integer> blockIdxMap = allocateNodeToBlockIdx(nodes);

		fillFollowersAndAncestors(nodes, followers, ancestors, blockIdxMap);

		fillInternalMetric(metric, nodes, optMetric, followers, ancestors);
		
		return optMetric;
	}

	private static void fillInternalMetric(InternalMetric metric, final Node[] nodes, InternalOptMetric optMetric,
			int[] followers, int[] ancestors) {
		// Fill new metric with
		// - the original metric data, if no follower or ancestor data are set
		// - OR an infinite number (=infinite distance).

		for (Node src : nodes) {
			for (Node dst : nodes) {
				int sIdx = src.getIdx();
				int dIdx = dst.getIdx();
				if((followers[sIdx] != -1 && followers[sIdx] != dIdx) || 
						(ancestors[dIdx] != -1 && ancestors[dIdx] != sIdx))
					optMetric.setDistance(src, dst, Float.MAX_VALUE);
				else
					optMetric.setDistance(src, dst, metric.getDistance(src, dst));
			}
		}
	}

	private static void fillFollowersAndAncestors(final Node[] nodes, int[] followerArr, int[] ancestorArr,
			ListMap<Integer, Integer> blockIdxMap) {
		for (int blockIdx : blockIdxMap.keySet()) {
			// For all nodes, which are allocated to this block
			List<Integer> nodeList = blockIdxMap.get(blockIdx);
			nodeList.sort((o1, o2) -> nodes[o1].getPresetBlockPos() - nodes[o2].getPresetBlockPos());

			// Build up a follower and ancestor array:
			// follower[i] = j means that node j is follower of i
			// same for ancestor
			for (int i = 0; i < nodeList.size(); i++) {
				final Node node = nodes[nodeList.get(i)];
				if(node.getPresetBlockPos() == BlockPositionConverter.UNDEF_POSITION)
					continue;

				if(i + 1 < nodeList.size()) {
					followerArr[node.getIdx()] = nodes[nodeList.get(i + 1)].getIdx();
					ancestorArr[nodes[nodeList.get(i + 1)].getIdx()] = node.getIdx();
				}
			}
		}
	}

	private static ListMap<Integer, Integer> allocateNodeToBlockIdx(final Node[] nodes) {
		ListMap<Integer, Integer> blockIdxMap = ListMap.create();
		for (int i = 0; i < nodes.length; i++) {
			if(nodes[i].getSiteType() == SiteType.DEPOT ||
					nodes[i].getSiteType() == SiteType.REPLENISH)
				continue;

			blockIdxMap.put(nodes[i].getPresetBlockIdx(), i);
		}
		return blockIdxMap;
	}

	private static boolean isNecessary(final Node[] nodes) {
		return Arrays.stream(nodes).anyMatch(node -> node.getPresetBlockIdx() > BlockNameConverter.DEFAULT_BLOCK_IDX);
	}
}
