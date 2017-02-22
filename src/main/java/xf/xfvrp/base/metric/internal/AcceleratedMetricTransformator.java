package xf.xfvrp.base.metric.internal;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;
import xf.xfvrp.base.metric.InternalMetric;
import xf.xfvrp.base.metric.Metric;

/** 
 * Copyright (c) 2012-present Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * This class performs the transformation of the user defined metric into a
 * internal metric, which is optimized for the use in optimization algorithms.
 * 
 * All nodes for the next optimization step and the associated vehicle data are
 * extracted into a rectangular matrix. The memory usage will be increased, but
 * the speed increases a lot.
 * 
 * @author hschneid
 *
 */
public class AcceleratedMetricTransformator {

	/**
	 * Performs the transformation of the user defined metric into a
	 * internal metric, which is optimized for the use in optimization algorithms.
	 * 
	 * @param metric The user defined metric
	 * @param nodeArr Current list of nodes
	 * @param veh Container object
	 * @return Internal metric for use in optimization procedures
	 */
	public static InternalMetric transform(Metric metric, Node[] nodeArr, Vehicle veh) {
		AcceleratedMetric acceleratedMetric = new AcceleratedMetric(nodeArr.length);
		for (int i = 0; i < nodeArr.length; i++)
			for (int j = 0; j < nodeArr.length; j++) {
				float[] v = metric.getDistanceAndTime(nodeArr[i], nodeArr[j], veh);

				if(v == null)
					throw new IllegalStateException("Missing distance information ("+nodeArr[i].getGeoId()+","+nodeArr[j].getGeoId()+")");

				acceleratedMetric.add(nodeArr[i].getIdx(), nodeArr[j].getIdx(), v[0], v[1]);
			}
		return acceleratedMetric;
	}
}
