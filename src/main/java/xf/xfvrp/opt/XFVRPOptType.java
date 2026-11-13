package xf.xfvrp.opt;

import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;

/**
 * Copyright (c) 2012-2023 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @author hschneid
 */
public class XFVRPOptType {

    private final Class<? extends XFVRPOptBase> clazz;

    XFVRPOptType(Class<? extends XFVRPOptBase> clazz) {
        this.clazz = clazz;
    }

    public XFVRPOptBase create() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "no copy of optimization procedure possible", e);
        }
    }
}
