package xf.xfvrp.base.metric;

import xf.xfvrp.base.Node;

/** 
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 *
 * A metric gives access to information about distances
 * and travel times between locations. This interface
 * specifies the necessary methods for all inherited metrics.
 * 
 * @author hschneid
 *
 */
public interface InternalMetric {

	/**
	 * Only distance
	 * 
	 * @param src Source node
	 * @param dst Destination node
	 * @return distance
	 */
	public float getDistance(Node src, Node dst);
	
	/**
	 * Only travel time
	 * 
	 * @param src Source node
	 * @param dst Destination node
	 * @return time
	 */
	public float getTime(Node src, Node dst);
	
	/**
	 * Returns the distance
	 * 
	 * @param src Source node
	 * @param dst Destination node
	 * @return record {distance, time}
	 */
	public float[] getDistanceAndTime(Node src, Node dst);
}
