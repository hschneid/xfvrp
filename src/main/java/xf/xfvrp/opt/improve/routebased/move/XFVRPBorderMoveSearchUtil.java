package xf.xfvrp.opt.improve.routebased.move;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;

import java.util.Queue;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFVRPBorderMoveSearchUtil {

    private static final float EPSILON = 0.001f;

    /**
     * Searches all improving steps in search space for a VRP.
     */
    public static void search(Solution solution, Queue<float[]> improvingSteps, boolean isInvertationActive) {
        Node[][] routes = solution.getRoutes();

        int nbrOfRoutes = routes.length;
        for (int srcRtIdx = 0; srcRtIdx < nbrOfRoutes; srcRtIdx++) {
            Node[] srcRoute = routes[srcRtIdx];
            if(srcRoute.length == 0)
                continue;

            for (int dstRtIdx = 0; dstRtIdx < nbrOfRoutes; dstRtIdx++) {
                Node[] dstRoute = routes[dstRtIdx];
                if(dstRoute.length == 0)
                    continue;

                var sameRoute = srcRtIdx == dstRtIdx;

                for (int srcPos = 1; srcPos < routes[srcRtIdx].length - 1; srcPos++) {
                    if(srcRoute[srcPos].getSiteType() == SiteType.DEPOT)
                        continue;

                    // Move before SRC to start of DST
                    if(!sameRoute)
                        searchInRoutes(
                                solution,
                                srcRoute, dstRoute,
                                srcRtIdx, dstRtIdx,
                                1,
                                1, srcPos - 1,
                                improvingSteps, isInvertationActive);

                    // Move before SRC to end of DST
                    searchInRoutes(
                            solution,
                            srcRoute, dstRoute,
                            srcRtIdx, dstRtIdx,
                            1,
                            dstRoute.length - 1, srcPos - 1,
                            improvingSteps, isInvertationActive);

                    // Move after SRC to start of DST
                    searchInRoutes(
                            solution,
                            srcRoute, dstRoute,
                            srcRtIdx, dstRtIdx,
                            srcPos,
                            1, srcRoute.length - srcPos - 2,
                            improvingSteps, isInvertationActive);

                    // Move after SRC to end of DST
                    if(!sameRoute)
                        searchInRoutes(
                                solution,
                                srcRoute, dstRoute,
                                srcRtIdx, dstRtIdx,
                                srcPos,
                                dstRoute.length - 1, srcRoute.length - srcPos - 2,
                                improvingSteps, isInvertationActive);
                }
            }
        }
    }

    private static void searchInRoutes(Solution solution, Node[] srcRoute, Node[] dstRoute, int srcRtIdx, int dstRtIdx, int srcPos, int dstPos, int segmentLength, Queue<float[]> improvingSteps, boolean isInvertationActive) {
        // Prevent Non-sense move
        if(srcRtIdx == dstRtIdx && dstPos - (srcPos + segmentLength) == 1)
            return;

        if(srcRtIdx == dstRtIdx && srcPos == dstPos)
            return;

        // dstPos is directly before src
        if (srcRtIdx == dstRtIdx && srcPos - dstPos == 1) {
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
        addImprovingStep(solution, improvingSteps, val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.NO_INVERT, XFVRPMoveUtil.NO_OVERHANG);

        // with invert
        if (isInvertationActive && segmentLength > 0) {
            val =
                    old -
                            (model.getDistanceForOptimization(srcRoute[srcPos - 1], srcRoute[srcPos + segmentLength + 1]) +
                                    model.getDistanceForOptimization(dstRoute[dstPos - 1], srcRoute[srcPos + segmentLength]) +
                                    model.getDistanceForOptimization(srcRoute[srcPos], dstRoute[dstPos]));
            addImprovingStep(solution, improvingSteps, val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.INVERT, XFVRPMoveUtil.NO_OVERHANG);
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

        addImprovingStep(solution, improvingSteps, val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.NO_INVERT, XFVRPMoveUtil.NO_OVERHANG);

        // with invert
        if (isInvertationActive && segmentLength > 0) {
            val =
                    old -
                            (model.getDistanceForOptimization(route[dstPos - 1], route[srcPos + segmentLength]) +
                                    model.getDistanceForOptimization(route[srcPos], route[dstPos]) +
                                    model.getDistanceForOptimization(route[dstPos], route[srcPos + segmentLength + 1]));
            addImprovingStep(solution, improvingSteps, val, srcRtIdx, dstRtIdx, srcPos, dstPos, segmentLength, XFVRPMoveUtil.INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
    }

    private static void addImprovingStep(Solution solution, Queue<float[]> improvingSteps, float... newStep) {
        // Check for nbr of routes
        if (newStep[1] != newStep[2]) {
            // Is destination route an overhang route? --> Penalty (val gets negative)
            if (isDestinationOverhangRoute(solution, newStep) && !isSourceOverhangRoute(solution, newStep)) {
                newStep[0] = -1;
                newStep[7] = XFVRPMoveUtil.IS_OVERHANG;
            }
            // Is source an overhang and destination is not overhang --> Bonus
            else if (isReduceOfOverhang(solution, newStep)) {
                if (newStep[0] <= 0) {
                    newStep[0] = (EPSILON * 2) + (-newStep[0] / 10000f);
                    newStep[7] = XFVRPMoveUtil.IS_OVERHANG;
                }
            }
        }

        // Add only improving steps
        if (newStep[0] > EPSILON) {
            improvingSteps.add(newStep);
        }
    }

    /**
     * In any case, if source route is overhang route
     */
    private static boolean isSourceOverhangRoute(Solution solution, float[] newStep) {
        boolean[] isOverhang = solution.getOverhangRoutes();
        return isOverhang[(int) newStep[1]];
    }

    /**
     * In any case, if destination route is overhang route
     */
    private static boolean isDestinationOverhangRoute(Solution solution, float[] newStep) {
        boolean[] isOverhang = solution.getOverhangRoutes();
        return isOverhang[(int) newStep[2]];
    }

    /**
     * If source route is overhang route, but destination is not, then it should get a bonus.
     * <p>
     * This counts for any move.
     */
    private static boolean isReduceOfOverhang(Solution solution, float[] newStep) {
        boolean[] isOverhang = solution.getOverhangRoutes();
        return isOverhang[(int) newStep[1]] && !isOverhang[(int) newStep[2]];
    }
}
