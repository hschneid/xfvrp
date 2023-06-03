package xf.xfvrp.base.metric;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public enum Metrics {

    EUCLEDIAN(EucledianMetric.class);

    private final Class clazz;

    Metrics(Class clazz) {
        this.clazz = clazz;
    }

    public Metric get() {
        try {
            return (Metric) Class.forName(clazz.getName()).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "no copy of metric is possible", e);
        }
    }
}
