package xf.xfvrp.base.metric.internal;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.metric.InternalMetric;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This class transforms an internal metric object by adding fix costs at
 * all edges from a depot to a customer. This way vehicle dependent fix costs
 * are realized.
 * 
 * An internal metric can be achived by an AcceleratedMetricTransformator.
 * 
 * @author hschneid
 *
 */
public class FixCostMetricTransformator {

	/**
	 * Adds vehicle dependent fix costs at an internal metric.
	 * 
	 * @param metric Internal metric without fix costs
	 * @param nodeArr List of nodes
	 * @param vehicle Container with the fix cost per each vehicle
	 * @return Internal metric with fix costs
	 */
	public static InternalMetric transform(InternalMetric metric, Node[] nodeArr, Vehicle vehicle) {
		InternalOptMetric optMetric = new InternalOptMetric(nodeArr.length);
		
		for (Node src : nodeArr) {
			SiteType srcType = src.getSiteType();
			
			for (Node dst : nodeArr) {
				SiteType dstType = dst.getSiteType();
				
				float dist = metric.getDistance(src, dst);
				if(
						(srcType == SiteType.DEPOT && dstType != SiteType.DEPOT) ||
						(srcType != SiteType.DEPOT && dstType == SiteType.DEPOT)
						) {
					dist += vehicle.fixCost;
				}
				
				optMetric.setDistance(src, dst, dist);
			}
		}
		
		return optMetric;
	}
}
