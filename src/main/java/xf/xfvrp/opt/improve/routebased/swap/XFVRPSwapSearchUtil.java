package xf.xfvrp.opt.improve.routebased.swap;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.XFVRPModel;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.improve.routebased.move.XFVRPMoveUtil;

import java.util.Queue;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 *
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFVRPSwapSearchUtil {

    private static final float EPSILON = 0.001f;

    /**
     * Searches all improving steps in search space for a VRP.
     */
    public static void search(Solution solution, Queue<float[]> improvingSteps, int maxSegmentLength, boolean isSegmentLengthEqual, boolean isInvertActive) {
        Node[][] routes = solution.getRoutes();

        int nbrOfRoutes = routes.length;
        for (int aRtIdx = 0; aRtIdx < nbrOfRoutes; aRtIdx++) {
            Node[] aRoute = routes[aRtIdx];
            for (int bRtIdx = aRtIdx; bRtIdx < nbrOfRoutes; bRtIdx++) {
                Node[] bRoute = routes[bRtIdx];
                for (int aPos = 1; aPos < aRoute.length - 1; aPos++) {

                    int aMaxSegmentLength = Math.min(maxSegmentLength, aRoute.length - aPos - 1);
                    for (int aSegmentLength = 0; aSegmentLength < aMaxSegmentLength; aSegmentLength++) {

                        for (int bPos = 1; bPos < bRoute.length - 1; bPos++) {
                            // aPos must not be identical to bPos
                            if(aRtIdx == bRtIdx && bPos == aPos) {
                                continue;
                            }

                            // Both segments must not overlap - Is bPos in A-segment
                            if(aRtIdx == bRtIdx && bPos >= aPos && bPos <= aPos + aSegmentLength) {
                                continue;
                            }

                            int bMaxSegmentLength = Math.min(maxSegmentLength, bRoute.length - bPos - 1);
                            for (int bSegmentLength = 0; bSegmentLength < bMaxSegmentLength; bSegmentLength++) {
                                // Both segments must not overlap - Is B-segment in A-segment
                                if(aRtIdx == bRtIdx && bPos < aPos && bPos + bSegmentLength >= aPos) {
                                    continue;
                                }

                                // If segment length should be equal
                                if(isSegmentLengthEqual && aSegmentLength != bSegmentLength) {
                                    continue;
                                }

                                searchInRoutes(
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
        if(aRtIdx == bRtIdx && aPos - (bPos + bSegmentLength) == 1) {
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
        else if(aRtIdx == bRtIdx && bPos - (aPos + aSegmentLength) == 1) {
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
        if(isInvertActive && aSegmentLength > 0 && bSegmentLength > 0) {
            val = old - (model.getDistance(aRoute[aPos - 1], bRoute[bb]) +
                    model.getDistance(bRoute[bPos], aRoute[aa + 1]) +
                    model.getDistance(bRoute[bPos - 1], aRoute[aa]) +
                    model.getDistance(aRoute[aPos], bRoute[bb + 1]));
            addImprovingStep(solution, improvingSteps, val, aRtIdx, bRtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.BOTH_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
        // A INVERT
        if(isInvertActive && aSegmentLength > 0) {
            val = old - (model.getDistance(aRoute[aPos - 1], bRoute[bPos]) +
                    model.getDistance(bRoute[bb], aRoute[aa + 1]) +
                    model.getDistance(bRoute[bPos - 1], aRoute[aa]) +
                    model.getDistance(aRoute[aPos], bRoute[bb + 1]));
            addImprovingStep(solution, improvingSteps, val, aRtIdx, bRtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.A_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
        // B INVERT
        if(isInvertActive && bSegmentLength > 0) {
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
        if(isInvertActive && aSegmentLength > 0 && bSegmentLength > 0) {
            val = old - (
                    model.getDistance(route[bPos - 1], route[aa]) +
                            model.getDistance(route[aPos], route[bb]) +
                            model.getDistance(route[bPos], route[aa + 1])
            );
            addImprovingStep(solution, improvingSteps, val, rtIdx, rtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.BOTH_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
        // A INVERT
        if(isInvertActive && aSegmentLength > 0) {
            val = old - (
                    model.getDistance(route[bPos - 1], route[aa]) +
                            model.getDistance(route[aPos], route[bPos]) +
                            model.getDistance(route[bb], route[aa + 1])
            );
            addImprovingStep(solution, improvingSteps, val, rtIdx, rtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.A_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
        // B INVERT
        if(isInvertActive && bSegmentLength > 0) {
            val = old - (
                    model.getDistance(route[bPos - 1], route[aPos]) +
                            model.getDistance(route[aa], route[bb]) +
                            model.getDistance(route[bPos], route[aa + 1])
            );
            addImprovingStep(solution, improvingSteps, val, rtIdx, rtIdx, aPos, bPos, aSegmentLength, bSegmentLength, XFVRPSwapUtil.B_INVERT, XFVRPMoveUtil.NO_OVERHANG);
        }
    }

    private static void addImprovingStep(Solution solution, Queue<float[]> improvingSteps, float... newStep) {
        // Check for nbr of routes
        /*if(newStep[1] != newStep[2]) {
            // Is destination route an overhang route? --> Penalty (val gets negative)
            if(isDestinationOverhangRoute(solution, newStep)) {
                newStep[0] = -1;
                newStep[7] = XFVRPMoveUtil.IS_OVERHANG;
            }
            // Is source an overhang and destination is not overhang --> Bonus
            else if(isReduceOfOverhang(solution, newStep)) {
                newStep[0] = EPSILON * 2;
                newStep[7] = XFVRPMoveUtil.IS_OVERHANG;
            }
        }*/

        // Add only improving steps
        if (newStep[0] > EPSILON) {
            improvingSteps.add(newStep);
        }
    }
}
