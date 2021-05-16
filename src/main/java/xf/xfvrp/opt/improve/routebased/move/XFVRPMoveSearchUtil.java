package xf.xfvrp.opt.improve.routebased.move;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;

import java.util.Queue;

/**
 * Copyright (c) 2012-2020 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFVRPMoveSearchUtil {

    private static final float EPSILON = 0.001f;

    /**
     * Searches all improving steps in search space for a VRP.
     */
    public static void search(XFVRPModel model, Node[][] routes, Queue<float[]> improvingSteps, int maxSegmentLength, boolean isInvertationActive) {
        int nbrOfRoutes = routes.length;
        for (int srcRtIdx = 0; srcRtIdx < nbrOfRoutes; srcRtIdx++) {
            Node[] srcRoute = routes[srcRtIdx];
            for (int dstRtIdx = 0; dstRtIdx < nbrOfRoutes; dstRtIdx++) {
                Node[] dstRoute = routes[dstRtIdx];
                for (int srcPos = 1; srcPos < routes[srcRtIdx].length - 1; srcPos++) {
                    for (int dstPos = 1; dstPos < routes[dstRtIdx].length; dstPos++) {
                        // src node must not be a depot
                        if(routes[srcRtIdx][srcPos].getSiteType() == SiteType.DEPOT)
                            continue;
                        if(srcRtIdx == dstRtIdx && (srcPos == dstPos || dstPos - srcPos == 1)) {
                            continue;
                        }

                        for (int segmentLength = 0; segmentLength < maxSegmentLength; segmentLength++) {
                            // src segment must not too big for src route
                            if((srcPos + segmentLength) > srcRoute.length - 2) {
                                break;
                            }
                            // Dst must not lay in the segment or directly behind it (no-move)
                            if(srcRoute == dstRoute && dstPos <= srcPos + segmentLength + 1 && dstPos >= srcPos) {
                                break;
                            }

                            searchInRoutes(model, srcRoute, dstRoute, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, improvingSteps, isInvertationActive);
                        }
                    }
                }
            }
        }
    }

    private static void searchInRoutes(XFVRPModel model, Node[] srcRoute, Node[] dstRoute, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, Queue<float[]> improvingSteps, boolean isInvertationActive) {
        // dstPos is directly before src
        if(srcRtIdx == dstRtIdx && srcPos - dstPos == 1) {
            searchWithDstBefore(model, srcRoute, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, improvingSteps, isInvertationActive);
        } else {
            searchNormal(model, srcRoute, dstRoute, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, improvingSteps, isInvertationActive);
        }
    }

    private static void searchNormal(XFVRPModel model, Node[] srcRoute, Node[] dstRoute, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, Queue<float[]> improvingSteps, boolean isInvertationActive) {
        float old = model.getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos]) +
                model.getDistanceForOptimization(srcRoute[srcPos + segmentLength], srcRoute[srcPos + segmentLength + 1]) +
                model.getDistanceForOptimization(dstRoute[dstPos - 1], dstRoute[dstPos]);

        // No invert
        float val =
                old -
                        (model.getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos + segmentLength + 1]) +
                                model.getDistanceForOptimization(dstRoute[dstPos - 1], srcRoute[srcPos]) +
                                model.getDistanceForOptimization(srcRoute[srcPos + segmentLength], dstRoute[dstPos]));
        if (val > EPSILON) improvingSteps.add(new float[]
                {val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.NO_INVERT}
                );

        // with invert
        if (isInvertationActive && segmentLength > 0) {
            val =
                    old -
                            (model.getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos + segmentLength + 1]) +
                                    model.getDistanceForOptimization(dstRoute[dstPos - 1], srcRoute[srcPos + segmentLength]) +
                                    model.getDistanceForOptimization(srcRoute[srcPos], dstRoute[dstPos]));
            if (val > EPSILON) improvingSteps.add(new float[]
                    {val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.INVERT}
                    );
        }
    }

    private static void searchWithDstBefore(XFVRPModel model, Node[] route, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, Queue<float[]> improvingSteps, boolean isInvertationActive) {
        float old =
                model.getDistanceForOptimization(route[dstPos - 1], route[dstPos]) +
                        model.getDistanceForOptimization(route[dstPos], route[srcPos]) +
                        model.getDistanceForOptimization(route[srcPos + segmentLength], route[srcPos + segmentLength + 1]);

        // No invert
        float val =
                old -
                        (model.getDistanceForOptimization(route[dstPos - 1], route[srcPos]) +
                                model.getDistanceForOptimization(route[srcPos + segmentLength], route[dstPos]) +
                                model.getDistanceForOptimization(route[dstPos], route[srcPos + segmentLength + 1]));
        if (val > EPSILON) improvingSteps.add(new float[]
                {val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.NO_INVERT}
                );

        // with invert
        if (isInvertationActive && segmentLength > 0) {
            val =
                    old -
                            (model.getDistanceForOptimization(route[dstPos - 1], route[srcPos + segmentLength]) +
                                    model.getDistanceForOptimization(route[srcPos], route[dstPos]) +
                                    model.getDistanceForOptimization(route[dstPos], route[srcPos + segmentLength + 1]));
            if (val > EPSILON) improvingSteps.add(new float[]
                    {val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.INVERT}
                    );
        }
    }
}
