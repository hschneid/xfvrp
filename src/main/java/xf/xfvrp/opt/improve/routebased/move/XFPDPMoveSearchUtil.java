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
public class XFPDPMoveSearchUtil {

    private static final float EPSILON = 0.001f;

    /**
     * Searches all improving steps in search space for a PDP.
     */
    public static void search(Solution solution, Queue<float[]> improvingSteps) {
        Node[][] routes = solution.getRoutes();

        int nbrOfRoutes = routes.length;

        int[][] shipmentPositions = getShipmentPositions(routes, solution.getModel());

        for (int srcRtIdx = 0; srcRtIdx < nbrOfRoutes; srcRtIdx++) {
            Node[] srcRoute = routes[srcRtIdx];
            for (int dstRtIdx = 0; dstRtIdx < nbrOfRoutes; dstRtIdx++) {
                Node[] dstRoute = routes[dstRtIdx];
                for (int srcPos = 1; srcPos < routes[srcRtIdx].length - 1; srcPos++) {
                    // src node must not be a depot
                    if (routes[srcRtIdx][srcPos].getSiteType() != SiteType.CUSTOMER)
                        continue;

                    // Source node must be pickup
                    if(routes[srcRtIdx][srcPos].getDemand()[0] < 0)
                        continue;

                    // srcB is dependent delivery to pickup srcA
                    int srcDeliveryPos = shipmentPositions[routes[srcRtIdx][srcPos].getShipmentIdx()][1];

                    for (int dstPickupPos = 1; dstPickupPos < routes[dstRtIdx].length; dstPickupPos++) {
                        for (int dstDeliveryPos = dstPickupPos; dstDeliveryPos < routes[dstRtIdx].length; dstDeliveryPos++) {
                            // src and dst must be different positions
                            if (srcRtIdx == dstRtIdx && (srcPos == dstPickupPos || dstPickupPos - srcPos == 1)) {
                                continue;
                            }

                            // Destination pointer must not be at Source pointer
                            if(srcRoute == dstRoute &&
                                    dstPickupPos - srcPos != 0 && dstPickupPos - srcPos != 1 &&
                                    dstDeliveryPos - srcDeliveryPos != 0 && dstDeliveryPos - srcDeliveryPos != 1) {
                                continue;
                            }

                            search(solution, srcRoute, dstRoute, srcRtIdx, dstRtIdx, srcPos, srcDeliveryPos, dstPickupPos, dstDeliveryPos, improvingSteps);
                        }
                    }
                }
            }
        }
    }

    private static void search(
            Solution solution,
            Node[] srcRoute,
            Node[] dstRoute,
            int srcRtIdx,
            int dstRtIdx,
            int srcPickupPos,
            int srcDeliveryPos,
            int dstPickupPos,
            int dstDeliveryPos,
            Queue<float[]> improvingSteps
    ) {
        XFVRPModel model = solution.getModel();
        float val = 0;

        // Einfache Opteration:
        // Schiebe eine Sendung (P & D) nach rechts. Es wird immer nach einem
        // Zielpunkt eingef�gt. Daher soll srcA ungleich (dstA & dstA - 1) sein UND srcB ungleich (dstB & dstB - 1).
        // Da B immer nach A sein muss, muss dstB gr��er dstA sein.
        // Verschiedene Sachen noch ungel�st:
        //  - Verschieben nach links
        //  - srcA und srcB werden mit dstA und dstB ineinander gewurschtelt
        //  - A und B liegen jetzt direkt nebeneinander

        val -= model.getDistanceForOptimization(srcRoute[srcPickupPos - 1], srcRoute[srcPickupPos]);
        val -= model.getDistanceForOptimization(srcRoute[srcPickupPos], srcRoute[srcPickupPos + 1]);
        val -= model.getDistanceForOptimization(srcRoute[srcDeliveryPos], srcRoute[srcDeliveryPos + 1]);
        // Wenn A und B nicht direkt nebeneinander liegen.
        if(srcDeliveryPos - srcPickupPos != 1)
            val -= model.getDistanceForOptimization(srcRoute[srcDeliveryPos - 1], srcRoute[srcDeliveryPos]);
        // Wenn A und B nicht direkt vor dstA weggeschoben werden
        if(srcRtIdx != dstRtIdx || (dstPickupPos - srcPickupPos != 1 && dstPickupPos - srcDeliveryPos != 1))
            val -= model.getDistanceForOptimization(dstRoute[dstPickupPos - 1], dstRoute[dstPickupPos]);
        // Wenn A und B nicht direkt vor dstB weggeschoben werden UND dstA und dstB nicht gleich sind
        if(srcRtIdx != dstRtIdx || (dstDeliveryPos - srcPickupPos != 1 && dstDeliveryPos - srcDeliveryPos != 1 && dstPickupPos != dstDeliveryPos))
            val -= model.getDistanceForOptimization(dstRoute[dstDeliveryPos - 1], dstRoute[dstDeliveryPos]);

        int offset = (srcRtIdx == dstRtIdx && (dstPickupPos - srcDeliveryPos == 1 || dstPickupPos - srcPickupPos == 1)) ?
                (srcDeliveryPos - srcPickupPos == 1) ? 3 : 2
                : 1;
        val += model.getDistanceForOptimization(dstRoute[dstPickupPos - offset], srcRoute[srcPickupPos]);
        if(dstPickupPos != dstDeliveryPos) {
            val += model.getDistanceForOptimization(srcRoute[srcPickupPos], dstRoute[dstPickupPos]);
            val += model.getDistanceForOptimization(dstRoute[dstDeliveryPos - 1], srcRoute[srcDeliveryPos]);
        } else
            val += model.getDistanceForOptimization(srcRoute[srcPickupPos], srcRoute[srcDeliveryPos]);

        val += model.getDistanceForOptimization(srcRoute[srcDeliveryPos], dstRoute[dstDeliveryPos]);

        // Wenn vor dstB NICHT irgend etwas weggeschoben wird
        offset = (srcDeliveryPos - srcPickupPos == 1) ? 2 : 1;
        if(srcRtIdx == dstRtIdx && dstDeliveryPos - srcPickupPos != offset && dstPickupPos - srcPickupPos != offset) {
            // Wenn srcA und srcB direkt nebeneinander liegen, dann �berspringe srcB
            offset = (srcDeliveryPos - srcPickupPos == 1) ? 2 : 1;
            val += model.getDistanceForOptimization(srcRoute[srcPickupPos - 1], srcRoute[srcPickupPos + offset]);
        }
        // Wenn nicht gilt, dass srcB vor dstA eingefügt wird oder
        // srcA vor dstA eingefügt wird und srcA und srcB hintereinander liegen
        if(srcDeliveryPos - srcPickupPos != 1 && ! (dstPickupPos - srcDeliveryPos == 1 || (dstPickupPos - srcPickupPos == 1 && srcDeliveryPos - srcPickupPos == 1)))
            val += model.getDistanceForOptimization(srcRoute[srcDeliveryPos - 1], srcRoute[srcDeliveryPos + 1]);

        addImprovingStep(solution, improvingSteps, -val, srcRtIdx, dstRtIdx, srcPickupPos, srcDeliveryPos, dstPickupPos, dstDeliveryPos, XFVRPMoveUtil.NO_OVERHANG);
    }

    private static void addImprovingStep(Solution solution, Queue<float[]> improvingSteps, float... newStep) {
        // Check for nbr of routes
        if(newStep[1] != newStep[2]) {
            // Is destination route an overhang route? --> Penalty (val gets negative)
            if(isDestinationOverhangRoute(solution, newStep) && !isSourceOverhangRoute(solution, newStep)) {
                newStep[0] = -1;
                newStep[7] = XFVRPMoveUtil.IS_OVERHANG;
            }
            // Is source an overhang and destination is not overhang --> Bonus
            else if(isReduceOfOverhang(solution, newStep)) {
                if(newStep[0] <= 0) {
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

    private static int[][] getShipmentPositions(Node[][] routes, XFVRPModel model) {
        int[][] shipmentPairIdxArr = new int[model.getNbrOfShipments()][2];

        for (int rt = routes.length - 1; rt >= 0; rt--) {
            for (int pos = routes[rt].length - 2; pos >= 1; pos--) {
                Node node = routes[rt][pos];
                if (node.getSiteType() == SiteType.CUSTOMER) {
                    int loadTypeIdx = (node.getDemand()[0] > 0) ? 0 : 1;

                    shipmentPairIdxArr[node.getShipmentIdx()][loadTypeIdx] = pos;
                }
            }
        }

        return shipmentPairIdxArr;
    }

    /**
     * In any case, if source route is overhang route
     */
    private static boolean isSourceOverhangRoute(Solution solution, float[] newStep) {
        boolean[] isOverhang = solution.getOverhangRoutes();
        return isOverhang[(int)newStep[1]];
    }

    /**
     * In any case, if destination route is overhang route
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
