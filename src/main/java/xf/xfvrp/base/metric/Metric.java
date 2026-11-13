package xf.xfvrp.base.metric;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Vehicle;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 * <p>
 * <p>
 * A metric gives access to information about distances
 * and travel times between locations. This interface
 * specifies the necessary methods for all inherited metrics.
 *
 * @author hschneid
 */
public interface Metric {

    /**
     * Only distance
     *
     * @param src Source node
     * @param dst Destination node
     * @param veh Container
     * @return distance
     */
    float getDistance(Node src, Node dst, Vehicle veh);

    /**
     * Only travel time
     *
     * @param src Source node
     * @param dst Destination node
     * @param veh Container
     * @return time
     */
    float getTime(Node src, Node dst, Vehicle veh);

    /**
     * Returns the distance
     *
     * @param src Source node
     * @param dst Destination node
     * @param veh Container
     * @return record {0 = distance, 1 = time}
     */
    float[] getDistanceAndTime(Node src, Node dst, Vehicle veh);
}
