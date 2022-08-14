package xf.xfvrp.opt.evaluation;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.base.exception.XFVRPExceptionType;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class FeasibilityAnalzer {

    public static void checkFeasibility(Node[] route) throws XFVRPException {
        if (route == null)
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "Empty route is not allowed to report");
        if (route[0].getSiteType() != SiteType.DEPOT)
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "First node in route is not a depot.");
        if (route[route.length - 1].getSiteType() != SiteType.DEPOT)
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "Last node in route is not a depot.");
        if (hasNullObjects(route))
            throw new XFVRPException(XFVRPExceptionType.ILLEGAL_STATE, "Route contains null objects!");
    }

    private static boolean hasNullObjects(Object[] objects) {
        for (int i = objects.length - 1; i >= 0; i--) {
            if (objects[i] == null) return true;
        }
        return false;
    }
}
