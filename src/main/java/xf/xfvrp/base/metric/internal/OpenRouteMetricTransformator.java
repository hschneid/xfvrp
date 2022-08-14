package xf.xfvrp.base.metric.internal;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPParameter;
import xf.xfvrp.base.metric.InternalMetric;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class OpenRouteMetricTransformator {

    /**
     * @param metric
     * @param nodeArr
     * @param parameter
     * @return
     */
    public static InternalMetric transform(InternalMetric metric, Node[] nodeArr, XFVRPParameter parameter) {
        AcceleratedMetric openMetric = new AcceleratedMetric(nodeArr.length);

        float[] zero = new float[]{0, 0};
        for (int i = 0; i < nodeArr.length; i++) {
            SiteType srcType = nodeArr[i].getSiteType();

            for (int j = 0; j < nodeArr.length; j++) {
                SiteType dstType = nodeArr[j].getSiteType();

                float[] v = metric.getDistanceAndTime(nodeArr[i], nodeArr[j]);
                if (parameter.isOpenRouteAtStart() && srcType == SiteType.DEPOT && dstType != SiteType.DEPOT)
                    v = zero;
                if (parameter.isOpenRouteAtEnd() && srcType != SiteType.DEPOT && dstType == SiteType.DEPOT)
                    v = zero;

                openMetric.add(nodeArr[i].getIdx(), nodeArr[j].getIdx(), v[0], v[1]);
            }
        }

        return openMetric;
    }
}
