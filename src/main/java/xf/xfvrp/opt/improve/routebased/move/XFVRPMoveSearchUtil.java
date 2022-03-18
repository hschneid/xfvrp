package xf.xfvrp.opt.improve.routebased.move;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;

import java.util.Queue;

/**
 * Copyright (c) 2012-2022 Holger Schneider
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
    public static void search(Solution solution, Queue<float[]> improvingSteps, int maxSegmentLength, boolean isInvertationActive) {
        Node[][] routes = solution.getRoutes();

        int nbrOfRoutes = routes.length;
        for (int srcRtIdx = 0; srcRtIdx < nbrOfRoutes; srcRtIdx++) {
            Node[] srcRoute = routes[srcRtIdx];
            for (int dstRtIdx = 0; dstRtIdx < nbrOfRoutes; dstRtIdx++) {
                Node[] dstRoute = routes[dstRtIdx];
                for (int srcPos = 1; srcPos < routes[srcRtIdx].length - 1; srcPos++) {
                    // src node must not be a depot
                    if(routes[srcRtIdx][srcPos].getSiteType() == SiteType.DEPOT)
                        continue;

                    for (int dstPos = 1; dstPos < routes[dstRtIdx].length; dstPos++) {
                        // src and dst must be different positions
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

                            searchInRoutes(solution, srcRoute, dstRoute, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, improvingSteps, isInvertationActive);
                        }
                    }
                }
            }
        }
    }

    private static void searchInRoutes(Solution solution, Node[] srcRoute, Node[] dstRoute, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, Queue<float[]> improvingSteps, boolean isInvertationActive) {
        // dstPos is directly before src
        if(srcRtIdx == dstRtIdx && srcPos - dstPos == 1) {
            searchWithDstBefore(solution, srcRoute, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, improvingSteps, isInvertationActive);
        } else {
            searchNormal(solution, srcRoute, dstRoute, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, improvingSteps, isInvertationActive);
        }
    }

    private static void searchNormal(Solution solution, Node[] srcRoute, Node[] dstRoute, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, Queue<float[]> improvingSteps, boolean isInvertationActive) {
        XFVRPModel model = solution.getModel();
        float old = model.getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos]) +
                model.getDistanceForOptimization(srcRoute[srcPos + segmentLength], srcRoute[srcPos + segmentLength + 1]) +
                model.getDistanceForOptimization(dstRoute[dstPos - 1], dstRoute[dstPos]);

        // No invert
        float val =
                old -
                        (model.getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos + segmentLength + 1]) +
                                model.getDistanceForOptimization(dstRoute[dstPos - 1], srcRoute[srcPos]) +
                                model.getDistanceForOptimization(srcRoute[srcPos + segmentLength], dstRoute[dstPos]));
        addImprovingStep(solution, improvingSteps, val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.NO_INVERT, XFVRPMoveUtil.NO_OVERGANG);

        // with invert
        if (isInvertationActive && segmentLength > 0) {
            val =
                    old -
                            (model.getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos + segmentLength + 1]) +
                                    model.getDistanceForOptimization(dstRoute[dstPos - 1], srcRoute[srcPos + segmentLength]) +
                                    model.getDistanceForOptimization(srcRoute[srcPos], dstRoute[dstPos]));
            addImprovingStep(solution, improvingSteps, val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.INVERT, XFVRPMoveUtil.NO_OVERGANG);
        }
    }

    private static void searchWithDstBefore(Solution solution, Node[] route, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, Queue<float[]> improvingSteps, boolean isInvertationActive) {
        XFVRPModel model = solution.getModel();
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

        addImprovingStep(solution, improvingSteps, val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.NO_INVERT, XFVRPMoveUtil.NO_OVERGANG);

        // with invert
        if (isInvertationActive && segmentLength > 0) {
            val =
                    old -
                            (model.getDistanceForOptimization(route[dstPos - 1], route[srcPos + segmentLength]) +
                                    model.getDistanceForOptimization(route[srcPos], route[dstPos]) +
                                    model.getDistanceForOptimization(route[dstPos], route[srcPos + segmentLength + 1]));
            addImprovingStep(solution, improvingSteps, val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.INVERT, XFVRPMoveUtil.NO_OVERGANG);
        }
    }

    private static void addImprovingStep(Solution solution, Queue<float[]> improvingSteps, float... newStep) {
        // Check for nbr of routes
        if(newStep[1] != newStep[2]) {
            // Is destination route an overhang route? --> Penalty (val gets negative)
            if(isDestinationOverhangRoute(solution, newStep)) {
                newStep[0] = -1;
                newStep[7] = XFVRPMoveUtil.IS_OVERGANG;
            }
            // Is source an overhang and destination is not overhang --> Bonus
            else if(isReduceOfOverhang(solution, newStep)) {
                newStep[0] = EPSILON * 2;
                newStep[7] = XFVRPMoveUtil.IS_OVERGANG;
            }
        }

        // Add only improving steps
        if (newStep[0] > EPSILON) {
            improvingSteps.add(newStep);
        }
    }

    /**
     * In any case, if destination is overhang route, then prevent a move to this route.
     */
    private static boolean isDestinationOverhangRoute(Solution solution, float[] newStep) {
        boolean[] isOverhang = solution.getOverhangRoutes();
        return isOverhang[(int)newStep[2]];
    }

    /**
     * If source route is overhang route, but destination is not, then it should get a bonus.
     *
     * This counts for any move.
     */
    private static boolean isReduceOfOverhang(Solution solution, float[] newStep) {
        boolean[] isOverhang = solution.getOverhangRoutes();
        return isOverhang[(int)newStep[1]] && !isOverhang[(int)newStep[2]];
    }


}
