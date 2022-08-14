package xf.xfvrp.opt.improve.ils;

import xf.xfvrp.base.Node;
import xf.xfvrp.base.Quality;
import xf.xfvrp.base.SiteType;
import xf.xfvrp.base.exception.XFVRPException;
import xf.xfvrp.opt.Solution;
import xf.xfvrp.opt.XFVRPOptBase;
import xf.xfvrp.opt.improve.routebased.move.XFPDPMoveUtil;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Copyright (c) 2012-2022 Holger Schneider
 * All rights reserved.
 * <p>
 * This source code is licensed under the MIT License (MIT) found in the
 * LICENSE file in the root directory of this source tree.
 **/
public class XFPDPRandomChangeService extends XFVRPOptBase implements XFRandomChangeService {

    private static final int MAX_TRIES_CHOOSING = 100;
    private final int NBR_ACCEPTED_INVALIDS = 100;
    private final int NBR_OF_VARIATIONS = 5;

    /*
     * (non-Javadoc)
     * @see xf.xfvrp.opt.improve.ils.XFRandomChangeService#change(xf.xfvrp.opt.Solution, xf.xfvrp.base.XFVRPModel)
     */
    @Override
    public Solution change(Solution solution) {
        this.setModel(solution.getModel());

        return this.execute(solution);
    }

    /**
     * This perturb routine relocates single nodes iterativly. The nodes are
     * selected randomly.
     */
    @Override
    protected Solution execute(Solution solution) {
        Choice choice = new Choice();

        for (int i = 0; i < NBR_OF_VARIATIONS; i++) {
            try {
                // Search nodes for source shipment
                // Restriction: no depot
                chooseSrcPickup(choice, solution);
                chooseSrcDelivery(choice, solution);

                // Search destination
                // Restriction:
                //   Source is not destination
                //   Solution is not invalid
                int cnt = 0;
                while (cnt < NBR_ACCEPTED_INVALIDS) {
                    // Choose
                    chooseDstPickup(choice, solution);
                    chooseDstDelivery(choice, solution);

                    boolean isValid = checkMove(choice, solution);
                    if (isValid)
                        break;

                    cnt++;
                }
            } catch (NoSuchElementException | XFVRPException e) {
                e.printStackTrace();
                // Means, that one of the choose methods could not find a valid variation parameter.
            }
        }

        return solution;
    }

    private boolean checkMove(Choice choice, Solution solution) throws XFVRPException {
        Node[][] oldRoutes = XFPDPMoveUtil.change(solution, choice.toArray());

        Quality q = check(solution);
        if (q.getPenalty() == 0) {
            return true;
        }

        reverseChange(solution, choice.toArray(), oldRoutes);

        return false;
    }

    private void reverseChange(Solution solution, float[] val, Node[][] oldRoutes) {
        solution.setRoute((int) val[1], oldRoutes[0]);
        if (oldRoutes.length > 1)
            solution.setRoute((int) val[2], oldRoutes[1]);
        solution.resetQualities();
    }

    private void chooseSrcPickup(Choice choice, Solution solution) {
        var routes = solution.getRoutes();

        int srcRouteIdx;
        do {
            srcRouteIdx = rand.nextInt(routes.length);
        } while (routes[srcRouteIdx] == null || hasNoValidNodes(routes[srcRouteIdx]));

        // Choose a random source node (customer)
        int srcPickPos;
        do {
            // Max value (DEP, ..., X, >Y<, Z, DEP)
            srcPickPos = rand.nextInt(routes[srcRouteIdx].length - 2) + 1;
        } while (routes[srcRouteIdx][srcPickPos].getSiteType() != SiteType.CUSTOMER ||
                // Only pickups (demand > 0)
                routes[srcRouteIdx][srcPickPos].getDemand()[0] < 0);

        choice.srcRouteIdx = srcRouteIdx;
        choice.srcPickPos = srcPickPos;
    }

    private void chooseSrcDelivery(Choice choice, Solution solution) throws NoSuchElementException {
        Node[] srcRoute = solution.getRoutes()[choice.srcRouteIdx];
        int shipIdx = srcRoute[choice.srcPickPos].getShipmentIdx();

        for (int i = 0; i < srcRoute.length; i++) {
            // Only deliveries (demand < 0)
            if (srcRoute[i].getShipmentIdx() == shipIdx && srcRoute[i].getDemand()[0] < 0) {
                choice.srcDeliPos = i;
                return;
            }
        }

        throw new NoSuchElementException("Structural exception of solution, where a pickup node of a shipment has no delivery node.");
    }

    private void chooseDstPickup(Choice choice, Solution solution) {
        var routes = solution.getRoutes();

        int dstRouteIdx;
        do {
            dstRouteIdx = rand.nextInt(routes.length);
        } while (routes[dstRouteIdx] == null);

        int dstPickPos;
        if (routes[dstRouteIdx].length == 2) {
            dstPickPos = 1;
        } else {
            do {
                // Max value (DEP, ..., X, Y, >Z<, DEP)
                dstPickPos = rand.nextInt(routes[dstRouteIdx].length - 2) + 1;
            } while (
                // Destination must not be on source node
                    choice.srcRouteIdx == dstRouteIdx &&
                            (choice.srcPickPos == dstPickPos || choice.srcDeliPos == dstPickPos)
            );
        }

        choice.dstRouteIdx = dstRouteIdx;
        choice.dstPickPos = dstPickPos;
    }

    private void chooseDstDelivery(Choice choice, Solution solution) throws NoSuchElementException {
        Node[] dstRoute = solution.getRoutes()[choice.dstRouteIdx];

        int dstDeliPos;
        int counter = 0;
        do {
            // Max value (DEP, ..., X, Y, Z, >DEP<)
            dstDeliPos = rand.nextInt(dstRoute.length - 1) + 1;
        } while (isInvalidDstDeliveryPos(choice, dstDeliPos) && counter++ < MAX_TRIES_CHOOSING);

        if (counter == MAX_TRIES_CHOOSING)
            throw new NoSuchElementException("Could not find a possible choice for PDP random change: " + Arrays.toString(choice.toArray()));

        choice.dstDeliPos = dstDeliPos;
    }

    private boolean isInvalidDstDeliveryPos(Choice choice, int dstDeliveryIdx) {
        return (
                // pickup before delivery
                choice.dstPickPos > dstDeliveryIdx ||
                        // Prevent no-op change
                        (choice.srcRouteIdx == choice.dstRouteIdx && choice.srcPickPos + 2 == choice.dstPickPos && choice.srcDeliPos + 1 == dstDeliveryIdx) ||
                        // Not on src nodes
                        (choice.srcRouteIdx == choice.dstRouteIdx && dstDeliveryIdx == choice.srcPickPos || dstDeliveryIdx == choice.srcDeliPos)
        );
    }

    private boolean hasNoValidNodes(Node[] route) {
        for (int i = route.length - 1; i >= 0; i--) {
            if (route[i].getSiteType() == SiteType.CUSTOMER)
                return false;
        }

        return true;
    }

    private class Choice {
        int srcRouteIdx;
        int dstRouteIdx;
        int srcPickPos;
        int srcDeliPos;
        int dstPickPos;
        int dstDeliPos;

        public float[] toArray() {
            return new float[]{
                    -1,
                    srcRouteIdx,
                    dstRouteIdx,
                    srcPickPos,
                    srcDeliPos,
                    dstPickPos,
                    dstDeliPos
            };
        }
    }


}
