package xf.xfvrp.opt.improve.routebased.swap;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveUtil;

import java.util.Queue;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFVRPBorderSwapSearchUtil {

    private static final float EPSILON = 0.001f;

    /**
     * Searches all improving steps in search space for a VRP.
     */
    public static void search(Solution solution, Queue<float[]> improvingSteps, boolean isInvertActive) {
        Node[][] routes = solution.getRoutes();

        int nbrOfRoutes = routes.length;
        for (int aRtIdx = 0; aRtIdx < nbrOfRoutes; aRtIdx++) {
            Node[] aRoute = routes[aRtIdx];
            for (int bRtIdx = aRtIdx; bRtIdx < nbrOfRoutes; bRtIdx++) {
                Node[] bRoute = routes[bRtIdx];

                for (int aPos = 1; aPos < aRoute.length - 1; aPos++) {

                    for (int bPos = 1; bPos < bRoute.length - 1; bPos++) {
                        // aPos must not be identical to bPos
                        if (aRtIdx == bRtIdx && aPos >= bPos) {
                            continue;
                        }

                        if(aRtIdx != bRtIdx) {
                            // Both before the pointers
                            searchInRoutes(
                                    solution,
                                    aRoute, bRoute,
                                    aRtIdx, bRtIdx,
                                    1, aPos - 1,
                                    1, bPos - 1,
                                    improvingSteps,
                                    isInvertActive
                            );

                            // Both A after and B before the pointers
                            searchInRoutes(
                                    solution,
                                    aRoute, bRoute,
                                    aRtIdx, bRtIdx,
                                    aPos, aRoute.length - aPos - 2,
                                    1, bPos - 1,
                                    improvingSteps,
                                    isInvertActive
                            );

                            // Both after the pointers
                            searchInRoutes(
                                    solution,
                                    aRoute, bRoute,
                                    aRtIdx, bRtIdx,
                                    aPos, aRoute.length - aPos - 2,
                                    bPos, bRoute.length - bPos - 2,
                                    improvingSteps,
                                    isInvertActive
                            );

                        }

                        // Both A before and B after the pointers
                        searchInRoutes(
                                solution,
                                aRoute, bRoute,
                                aRtIdx, bRtIdx,
                                1, aPos - 1,
                                bPos, bRoute.length - bPos - 2,
                                improvingSteps,
                                isInvertActive
                        );

                    }
                }
            }
        }
    }

    private static void searchInRoutes(
            Solution solution,
            Node[] aRoute,
            Node[] bRoute,
            int aRtIdx,
            int bRtIdx,
            int aPos,
            int aSegmentLength,
            int bPos,
            int bSegmentLength,
            Queue<float[]> improvingSteps,
            boolean isInvertActive
    ) {
        // B-segment is directly before A-segment
        if (aRtIdx == bRtIdx && aPos - (bPos + bSegmentLength) == 1) {
            searchInRoutesBbeforeA(
                    solution,
                    aRoute,
                    aRtIdx,
                    aPos, aSegmentLength,
                    bPos, bSegmentLength,
                    improvingSteps,
                    isInvertActive
            );
        }
        // A-segment is directly before B-segment
        else if (aRtIdx == bRtIdx && bPos - (aPos + aSegmentLength) == 1) {
            searchInRoutesBbeforeA(
                    solution,
                    aRoute,
                    aRtIdx,
                    // Switched A and B to reuse same method
                    bPos, bSegmentLength,
                    aPos, aSegmentLength,
                    improvingSteps,
                    isInvertActive
            );
        } else {
            searchInRoutesNormal(
                    solution,
                    aRoute, bRoute,
                    aRtIdx, bRtIdx,
                    aPos, aSegmentLength,
                    bPos, bSegmentLength,
                    improvingSteps,
                    isInvertActive
            );
        }
    }

    private static void searchInRoutesNormal(Solution solution, Node[] aRoute, Node[] bRoute, int aRtIdx, int bRtIdx, int aPos, int aSegmentLength, int bPos, int bSegmentLength, Queue<float[]> improvingSteps, boolean isInvertActive) {
        XFVRPModel model = solution.getModel();

        int aa = aPos + aSegmentLength;
        int bb = bPos + bSegmentLength;

        float old = model.getDistance(aRoute[aPos - 1], aRoute[aPos]) +
                model.getDistance(aRoute[aa], aRoute[aa + 1]) +
                model.getDistance(bRoute[bPos - 1], bRoute[bPos]) +
                model.getDistance(bRoute[bb], bRoute[bb + 1]);

        float val;
        // NO INVERT
        val = old - (model.getDistance(aRoute[aPos - 1], bRoute[bPos]) +
                model.getDistance(bRoute[bb], aRoute[aa + 1]) +
                model.getDistance(bRoute[bPos - 1], aRoute[aPos]) +
                model.getDistance(aRoute[aa], bRoute[bb + 1]));
        addImprovingStep(solution, improvingSteps, val, aRtIdx, bRtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.NO_INVERT, XFVRPMoveUtil.NO_OVERHANG);

        // BOTH INVERT
        if (isInvertActive && aSegmentLength > 0 && bSegmentLength > 0) {
            val = old - (model.getDistance(aRoute[aPos - 1], bRoute[bb]) +
                    model.getDistance(bRoute[bPos], aRoute[aa + 1]) +
                    model.getDistance(bRoute[bPos - 1], aRoute[aa]) +
                    model.getDistance(aRoute[aPos], bRoute[bb + 1]));
            addImprovingStep(solution, improvingSteps, val, aRtIdx, bRtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.BOTH_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
        // A INVERT
        if (isInvertActive && aSegmentLength > 0) {
            val = old - (model.getDistance(aRoute[aPos - 1], bRoute[bPos]) +
                    model.getDistance(bRoute[bb], aRoute[aa + 1]) +
                    model.getDistance(bRoute[bPos - 1], aRoute[aa]) +
                    model.getDistance(aRoute[aPos], bRoute[bb + 1]));
            addImprovingStep(solution, improvingSteps, val, aRtIdx, bRtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.A_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
        // B INVERT
        if (isInvertActive && bSegmentLength > 0) {
            val = old - (model.getDistance(aRoute[aPos - 1], bRoute[bb]) +
                    model.getDistance(bRoute[bPos], aRoute[aa + 1]) +
                    model.getDistance(bRoute[bPos - 1], aRoute[aPos]) +
                    model.getDistance(aRoute[aa], bRoute[bb + 1]));
            addImprovingStep(solution, improvingSteps, val, aRtIdx, bRtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.B_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
    }

    private static void searchInRoutesBbeforeA(Solution solution, Node[] route, int rtIdx, int aPos, int aSegmentLength, int bPos, int bSegmentLength, Queue<float[]> improvingSteps, boolean isInvertActive) {
        XFVRPModel model = solution.getModel();

        int aa = aPos + aSegmentLength;
        int bb = bPos + bSegmentLength;

        float old =
                model.getDistance(route[bPos - 1], route[bPos]) +
                        model.getDistance(route[bb], route[aPos]) +
                        model.getDistance(route[aa], route[aa + 1]);

        float val;
        // NO INVERT
        val = old - (
                model.getDistance(route[bPos - 1], route[aPos]) +
                        model.getDistance(route[aa], route[bPos]) +
                        model.getDistance(route[bb], route[aa + 1])
        );
        addImprovingStep(solution, improvingSteps, val, rtIdx, rtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.NO_INVERT, XFVRPMoveUtil.NO_OVERHANG);

        // BOTH INVERT
        if (isInvertActive && aSegmentLength > 0 && bSegmentLength > 0) {
            val = old - (
                    model.getDistance(route[bPos - 1], route[aa]) +
                            model.getDistance(route[aPos], route[bb]) +
                            model.getDistance(route[bPos], route[aa + 1])
            );
            addImprovingStep(solution, improvingSteps, val, rtIdx, rtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.BOTH_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
        // A INVERT
        if (isInvertActive && aSegmentLength > 0) {
            val = old - (
                    model.getDistance(route[bPos - 1], route[aa]) +
                            model.getDistance(route[aPos], route[bPos]) +
                            model.getDistance(route[bb], route[aa + 1])
            );
            addImprovingStep(solution, improvingSteps, val, rtIdx, rtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.A_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
        // B INVERT
        if (isInvertActive && bSegmentLength > 0) {
            val = old - (
                    model.getDistance(route[bPos - 1], route[aPos]) +
                            model.getDistance(route[aa], route[bb]) +
                            model.getDistance(route[bPos], route[aa + 1])
            );
            addImprovingStep(solution, improvingSteps, val, rtIdx, rtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.B_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
    }

    /**
     * Swapping does not check for overhanging routes, because only customers are swapped, and this
     * is not changing the number of routes.
     */
    private static void addImprovingStep(Solution solution, Queue<float[]> improvingSteps, float... newStep) {
        // Prevent, that additional nodes are moved to overhanging routes
        if ((isDestinationOverhangRoute(solution, newStep) && newStep[5] > newStep[6]) ||
                (isSourceOverhangRoute(solution, newStep) && newStep[6] > newStep[5])) {
            newStep[0] = -1;
            newStep[8] = XFVRPMoveUtil.IS_OVERHANG;
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
}
