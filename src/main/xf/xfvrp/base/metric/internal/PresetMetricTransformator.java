package xf.xfvrp.base.metric.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import util.collection.ListMap;
import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.preset.BlockNameConverter;

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
public class PresetMetricTransformator {

	/**
	 * This method transforms an internal metric into an internal optimization metric, where distance
	 * informations are modified for the user given sequence position preset. This means:
	 * 
	 * if a node A must not be the predecessor of node B, than the distance between A->B
	 * is modified to unlimited.
	 * 
	 * @param metric The original internal metric
	 * @param nodeArr The nodes of current XFVRP model
	 * @return An internal optimization metric, where sequence position preset is considered.
	 */
	public static InternalMetric transform(InternalMetric metric, final Node[] nodeArr) {
		// Check if transformation is necessary. So, one sequence position preset is set.
		{
			boolean shallTransform = false;
			for (int i = 0; i < nodeArr.length; i++) {
				if(nodeArr[i].getPresetBlockPos() > BlockNameConverter.DEFAULT_BLOCK_IDX) {
					shallTransform = true;
					break;
				}
			}

			if(!shallTransform)
				return metric;
		}

		InternalOptMetric optMetric = new InternalOptMetric(nodeArr.length);
		int[] followerArr = new int[nodeArr.length];
		int[] ancestorArr = new int[nodeArr.length];
		Arrays.fill(followerArr, -1); Arrays.fill(ancestorArr, -1);

		// Allocate the nodes to their the preset block index
		ListMap<Integer, Integer> blockIdxMap = ListMap.create();
		for (int i = 0; i < nodeArr.length; i++) {
			if(nodeArr[i].getSiteType() == SiteType.DEPOT ||
					nodeArr[i].getSiteType() == SiteType.REPLENISH)
				continue;

			blockIdxMap.put(nodeArr[i].getPresetBlockIdx(), i);
		}

		// For each block
		for (int blockIdx : blockIdxMap.keySet()) {
			// For all nodes, which are allocated to this block
			List<Integer> nodeList = blockIdxMap.get(blockIdx);
			Collections.sort(nodeList, new Comparator<Integer>() {
				@Override
				public int compare(Integer o1, Integer o2) {
					return nodeArr[o1].getPresetBlockPos() - nodeArr[o2].getPresetBlockPos();
				}
			});

			// Build up a follower and ancestor array:
			// follower[i] = j means that node j is follower of i
			// same for ancestor
			for (int i = 0; i < nodeList.size(); i++) {
				final Node node = nodeArr[nodeList.get(i)];
				if(node.getPresetBlockPos() == -1)
					continue;

				if(i + 1 < nodeList.size()) {
					followerArr[node.getIdx()] = nodeArr[nodeList.get(i + 1)].getIdx();
					ancestorArr[nodeArr[nodeList.get(i + 1)].getIdx()] = node.getIdx();
				}
			}
		}

		// Fill new metric with
		// - the original metric data, if no follower or ancestor data are set
		// - OR an infinite number (=infinite distance).
		for (Node src : nodeArr) {
			for (Node dst : nodeArr) {
				int sIdx = src.getIdx();
				int dIdx = dst.getIdx();
				if((followerArr[sIdx] != -1 && followerArr[sIdx] != dIdx) || 
						(ancestorArr[dIdx] != -1 && ancestorArr[dIdx] != sIdx))
					optMetric.setDistance(src, dst, Float.MAX_VALUE);
				else
					optMetric.setDistance(src, dst, metric.getDistance(src, dst));
			}
		}
		return optMetric;
	}
}
